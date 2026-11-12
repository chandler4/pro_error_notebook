package com.errnote.log.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.errnote.common.IdGenerator;
import com.errnote.dto.PageResult;
import com.errnote.log.entity.BusinessLog;
import com.errnote.log.mapper.BusinessLogMapper;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** 业务日志服务单测：TC-G-03（AI 调用记录原文全文）+ TC-G-04（入库失败吞异常）+ 查询收敛。 */
class BusinessLogServiceImplTest {

  private final BusinessLogMapper mapper = mock(BusinessLogMapper.class);
  private final IdGenerator idGenerator = mock(IdGenerator.class);
  private final BusinessLogServiceImpl service = new BusinessLogServiceImpl(mapper, idGenerator);

  // TC-G-03 规约5：recordAi 保存模型返回原文（responseContent 完整不截断）、logType=AI_CALL、action 含模型名
  @Test
  void recordAiSavesRawResponseVerbatim() {
    String raw =
        "{\"choices\":[{\"message\":{\"content\":\"{\\\"subject\\\":\\\"physics\\\"}\"}}]}";

    service.recordAi("glm-5.3-flash", 18342, "images=2", raw);

    ArgumentCaptor<BusinessLog> captor = ArgumentCaptor.forClass(BusinessLog.class);
    verify(mapper).insert(captor.capture());
    BusinessLog saved = captor.getValue();
    assertThat(saved.getLogType()).isEqualTo("AI_CALL");
    assertThat(saved.getModule()).isEqualTo("OpenAiVisionAdapter#parse");
    assertThat(saved.getAction()).contains("glm-5.3-flash");
    assertThat(saved.getResponseContent()).isEqualTo(raw);
    assertThat(saved.getSuccess()).isTrue();
    assertThat(saved.getDurationMs()).isEqualTo(18342);
  }

  // TC-G-04 规约5：入库失败仅告警不抛出
  @Test
  void saveSwallowsPersistFailure() {
    doThrow(new RuntimeException("db down")).when(mapper).insert(any(BusinessLog.class));

    assertThatCode(() -> service.recordAi("glm-5.3-flash", 1, "s", "raw"))
        .doesNotThrowAnyException();
  }

  // TC-L-04 同族：分页参数收敛（page>=1、size<=50）
  @Test
  void queryNormalizesPaging() {
    Page<BusinessLog> page = new Page<>(1, 10);
    page.setRecords(Collections.singletonList(new BusinessLog()));
    page.setTotal(1);
    when(mapper.selectPage(any(), any())).thenReturn(page);
    when(idGenerator.newId()).thenReturn("t-1");

    PageResult<BusinessLog> result = service.query(null, null, null, null, null, 0, 500);

    assertThat(result.getPage()).isEqualTo(1);
    assertThat(result.getSize()).isEqualTo(50);
    assertThat(result.getList()).hasSize(1);
  }
}
