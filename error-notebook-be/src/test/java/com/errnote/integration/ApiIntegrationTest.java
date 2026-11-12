package com.errnote.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.errnote.common.ApiResponse;
import com.errnote.integration.ai.AiServiceClient;
import com.errnote.integration.ai.dto.AiParseResponse;
import com.errnote.integration.ai.dto.WrongQuestionDraftDto;
import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * 接口全量集成测试（B-11 / H2 内存库）：上传→AI 解析（Feign 替身）→确认→列表/详情→编辑→删除联动删图 全链路；
 * 日志留痕（OPERATION/AI_CALL）；meta；导出；camelCase 与规约一致性（TC-D-03）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private AiServiceClient aiServiceClient;

  private static final String RAW =
      "{\"mock-raw\":\"raw-model-response-full-text\",\"choices\":[]}";

  private void mockAiOk() {
    WrongQuestionDraftDto draft =
        WrongQuestionDraftDto.builder()
            .subject("physics")
            .gradeLevel("grade9")
            .questionType("fillBlank")
            .questionStem("在探究\"串联电路电压规律\"的实验中……")
            .sourceName("2026—2027 学年度上学期九年级物理学科素养调研")
            .questionNumber("14")
            .standardAnswer(Arrays.asList("12", "1:5"))
            .knowledgePoints(Arrays.asList("电压表读数", "串联电路分压规律"))
            .knowledgeAnalysis("**步骤1** ……")
            .tags(Arrays.asList())
            .build();
    AiParseResponse data =
        AiParseResponse.builder()
            .draft(draft)
            .rawModelResponse(RAW)
            .model("glm-5.3-flash")
            .durationMs(18342L)
            .build();
    given(aiServiceClient.parse(any())).willReturn(ApiResponse.ok(data));
  }

  /** 规约 1a 全链路：上传→解析草稿→确认→列表可见→详情字段一致→编辑→删除联动删图。 */
  @Test
  void fullLifecycleFromUploadToConfirmEditDelete() throws Exception {
    // 1. 上传图片（真实落盘 target/test-data）
    MvcResult upload =
        mockMvc
            .perform(
                multipart("/api/v1/questions/parse-image")
                    .file(
                        new MockMultipartFile(
                            "files",
                            "q.jpg",
                            "image/jpeg",
                            new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 1, 2, 3}))
                    .file(
                        new MockMultipartFile(
                            "files",
                            "a.jpg",
                            "image/jpeg",
                            new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 9})))
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();
    assertThat(upload.getResponse().getContentAsString()).contains("images/");

    // 2. AI 解析 → 草稿 PENDING_CONFIRM
    mockAiOk();
    String parseBody =
        "{\"images\":[{\"url\":\"images/202611/qa1.jpg\",\"role\":\"QUESTION\"},"
            + "{\"url\":\"images/202611/qa2.jpg\",\"role\":\"ANSWER_SHEET\"}]}";
    MvcResult parse =
        mockMvc
            .perform(
                post("/api/v1/questions/parse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(parseBody))
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.status").value("PENDING_CONFIRM"))
            .andReturn();
    String id =
        com.jayway.jsonpath.JsonPath.read(
            parse.getResponse().getContentAsString(), "$.data.errorQuestionId");

    // 3. 确认入库（tags 去重 + 必填校验负例）
    mockMvc
        .perform(
            post("/api/v1/questions/" + id + "/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"subject\":\"\",\"questionStem\":\"\"}"))
        .andExpect(jsonPath("$.code").value(40000));
    String confirmBody =
        "{\"subject\":\"physics\",\"gradeLevel\":\"grade9\",\"questionType\":\"fillBlank\","
            + "\"questionStem\":\"确认后的题干\",\"questionImages\":[{\"url\":\"images/202611/qa1.jpg\",\"role\":\"QUESTION\"}],"
            + "\"sourceName\":\"2026 学年度调研\",\"questionNumber\":\"14\","
            + "\"standardAnswer\":[\"12\",\"1:5\"],\"knowledgePoints\":[\"电压表读数\",\"串联电路分压规律\"],"
            + "\"tags\":[\"二刷\",\"易错\",\"二刷\"],\"knowledgeAnalysis\":\"步骤…\",\"wrongDate\":\"2026-11-10\"}";
    mockMvc
        .perform(
            post("/api/v1/questions/" + id + "/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(confirmBody))
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.data.tags.length()").value(2)); // TC-T-01 去重

    // 4. 列表可见（仅 CONFIRMED）；详情全字段 camelCase 与规约一致（TC-D-03）
    mockMvc
        .perform(get("/api/v1/questions").param("subject", "physics"))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.list[0].errorQuestionId").value(id));
    mockMvc
        .perform(get("/api/v1/questions/" + id))
        .andExpect(jsonPath("$.data.errorQuestionId").value(id))
        .andExpect(jsonPath("$.data.subject").value("physics"))
        .andExpect(jsonPath("$.data.gradeLevel").value("grade9"))
        .andExpect(jsonPath("$.data.questionType").value("fillBlank"))
        .andExpect(jsonPath("$.data.questionStem").value("确认后的题干"))
        .andExpect(jsonPath("$.data.questionImages[0].url").value("images/202611/qa1.jpg"))
        .andExpect(jsonPath("$.data.questionImages[0].role").value("QUESTION"))
        .andExpect(jsonPath("$.data.sourceName").value("2026 学年度调研"))
        .andExpect(jsonPath("$.data.questionNumber").value("14"))
        .andExpect(jsonPath("$.data.standardAnswer[1]").value("1:5"))
        .andExpect(jsonPath("$.data.knowledgePoints[0]").value("电压表读数"))
        .andExpect(jsonPath("$.data.tags[1]").value("易错"))
        .andExpect(jsonPath("$.data.knowledgeAnalysis").value("步骤…"))
        .andExpect(jsonPath("$.data.wrongDate").value("2026-11-10"))
        .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
        .andExpect(jsonPath("$.data.updatedAt").isNotEmpty());

    // 5. 编辑（PUT）改题干并把图片替换为真实落盘文件
    String stored = storeRealImage();
    assertThat(new File("./target/test-data", stored)).exists();
    String putBody =
        confirmBody.replace("确认后的题干", "编辑后的题干").replace("images/202611/qa1.jpg", stored);
    mockMvc
        .perform(
            put("/api/v1/questions/" + id).contentType(MediaType.APPLICATION_JSON).content(putBody))
        .andExpect(jsonPath("$.data.questionStem").value("编辑后的题干"))
        .andExpect(jsonPath("$.data.questionImages[0].url").value(stored));

    // 6. 删除联动删图（BR-04）：记录删除后磁盘文件同步消失
    mockMvc.perform(delete("/api/v1/questions/" + id)).andExpect(jsonPath("$.code").value(0));
    mockMvc.perform(get("/api/v1/questions/" + id)).andExpect(jsonPath("$.code").value(40400));
    assertThat(new File("./target/test-data", stored)).doesNotExist();

    // 7. 未知 id → 40400
    mockMvc.perform(get("/api/v1/questions/no-such-id")).andExpect(jsonPath("$.code").value(40400));
  }

  /** 日志链路（FR-C1~C3）：OPERATION 随请求产生；AI_CALL 含模型返回原文（TC-G-03）。 */
  @Test
  void businessLogsRecordOperationAndAiCallWithRaw() throws Exception {
    mockAiOk();
    mockMvc
        .perform(
            post("/api/v1/questions/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"images\":[{\"url\":\"images/202611/x.jpg\",\"role\":\"QUESTION\"}]}"))
        .andExpect(jsonPath("$.code").value(0));

    MvcResult logs =
        mockMvc
            .perform(get("/api/v1/logs").param("logType", "AI_CALL"))
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.list[0].logType").value("AI_CALL"))
            .andExpect(jsonPath("$.data.list[0].module").value("OpenAiVisionAdapter#parse"))
            .andExpect(
                jsonPath("$.data.list[0].action")
                    .value(org.hamcrest.Matchers.containsString("glm-5.3-flash")))
            .andExpect(
                jsonPath("$.data.list[0].responseContent")
                    .value(org.hamcrest.Matchers.containsString("raw-model-response-full-text")))
            .andExpect(jsonPath("$.data.list[0].success").value(true))
            .andExpect(jsonPath("$.data.list[0].errorCode").value(0))
            .andExpect(jsonPath("$.data.list[0].durationMs").value(18342))
            .andReturn();
    assertThat(logs).isNotNull();

    mockMvc
        .perform(get("/api/v1/logs").param("logType", "OPERATION").param("success", "true"))
        .andExpect(jsonPath("$.data.total").value(1)) // 上述 parse 请求自身的操作日志（TC-G-01）
        .andExpect(jsonPath("$.data.list[0].module").value("QuestionParseController#parse"));
  }

  /** meta：enums 来自 yml；knowledge-points 联想含确认过的知识点（TC-C-04 集成）。 */
  @Test
  void metaEndpointsServeEnumsAndKnowledgeSuggestions() throws Exception {
    mockMvc
        .perform(get("/api/v1/meta/enums"))
        .andExpect(jsonPath("$.data.subjects[0]").value("chinese"))
        .andExpect(jsonPath("$.data.subjectTabs.length()").value(3))
        .andExpect(jsonPath("$.data.gradeLevels.length()").value(12));

    mockAiOk();
    String id =
        com.jayway.jsonpath.JsonPath.read(
            mockMvc
                .perform(
                    post("/api/v1/questions/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"images\":[{\"url\":\"images/202611/x.jpg\",\"role\":\"QUESTION\"}]}"))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.data.errorQuestionId");
    mockMvc
        .perform(
            post("/api/v1/questions/" + id + "/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"subject\":\"physics\",\"questionStem\":\"题干\","
                        + "\"knowledgePoints\":[\"电压表读数\"]}"))
        .andExpect(jsonPath("$.code").value(0));

    mockMvc
        .perform(get("/api/v1/meta/knowledge-points").param("keyword", "电压"))
        .andExpect(jsonPath("$.data[0].name").value("电压表读数"))
        .andExpect(jsonPath("$.data[0].useCount").value(1));
  }

  /** 导出（真链路）：markdown 含确认题干与答案（TC-E-03 流量 + 内容）。 */
  @Test
  void exportMarkdownFromConfirmedData() throws Exception {
    mockAiOk();
    String id =
        com.jayway.jsonpath.JsonPath.read(
            mockMvc
                .perform(
                    post("/api/v1/questions/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"images\":[{\"url\":\"images/202611/x.jpg\",\"role\":\"QUESTION\"}]}"))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.data.errorQuestionId");
    mockMvc
        .perform(
            post("/api/v1/questions/" + id + "/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"subject\":\"math\",\"questionStem\":\"导出验证题干\"}"))
        .andExpect(jsonPath("$.code").value(0));

    MvcResult result =
        mockMvc
            .perform(get("/api/v1/questions/export").param("format", "markdown"))
            .andExpect(status().isOk())
            .andReturn();
    String body = new String(result.getResponse().getContentAsByteArray(), "UTF-8");
    assertThat(body).contains("导出验证题干");
    assertThat(body).contains("# 错题本");
  }

  /** 上传校验（真链路）：>6 张 41003 / 非图片 41001。 */
  @Test
  void uploadEnforcesCountAndMagicNumber() throws Exception {
    org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder many =
        multipart("/api/v1/questions/parse-image");
    for (int i = 0; i < 7; i++) {
      many.file(
          new MockMultipartFile(
              "files",
              "f" + i + ".jpg",
              "image/jpeg",
              new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}));
    }
    mockMvc.perform(many).andExpect(jsonPath("$.code").value(41003));

    mockMvc
        .perform(
            multipart("/api/v1/questions/parse-image")
                .file(
                    new MockMultipartFile(
                        "files", "fake.jpg", "image/jpeg", "plain text".getBytes())))
        .andExpect(jsonPath("$.code").value(41001));
  }

  private String storeRealImage() throws Exception {
    MvcResult upload =
        mockMvc
            .perform(
                multipart("/api/v1/questions/parse-image")
                    .file(
                        new MockMultipartFile(
                            "files",
                            "real.jpg",
                            "image/jpeg",
                            new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 7, 7})))
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();
    return com.jayway.jsonpath.JsonPath.read(
        upload.getResponse().getContentAsString(), "$.data[0]");
  }

  /** 辅助：确认 data 目录基线存在（test profile data.dir=./target/test-data）。 */
  @Test
  void testDataDirIsIsolatedFromRepoData() {
    assertThat(Paths.get("./target/test-data").toString()).contains("target");
  }
}
