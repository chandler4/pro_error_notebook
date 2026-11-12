<script setup lang="ts">
// P2 新增/编辑三步流（prototype-spec §2.2）：上传 →(≥1图+解析)→ 解析中 →(成功)→ 分项确认 → 完成；
// 失败态：重试 / 转手动（POST drafts 空草稿，零 AI）；编辑 ?id= 直达步骤3；重解析 confirm 后删旧（TC-N-06）。
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  parseImage,
  parseQuestion,
  createEmptyDraft,
  confirmQuestion,
  getQuestion,
  updateQuestion,
  deleteQuestion,
} from '../api/question'
import { getEnums, getKnowledgePoints } from '../api/meta'
import type { ImageRef, QuestionDraftInput, WrongQuestion } from '../types'
import type { KnowledgePointCandidate } from '../types'
import ImageUploader, { type UploadedImage } from '../components/ImageUploader.vue'
import { fileUrl } from '../api/request'

/** 表单态：必填数组字段在 FE 侧恒为数组（提交时按 QuestionDraftInput 序列化）。 */
interface FormState extends QuestionDraftInput {
  questionImages: ImageRef[]
  standardAnswer: string[]
  knowledgePoints: string[]
  tags: string[]
}

const route = useRoute()
const router = useRouter()

// ---- 状态机 ----
const step = ref(0) // 0 上传 / 1 解析 / 2 确认
const done = ref(false)
const parsing = ref(false)
const parseError = ref('')
const submitting = ref(false)
const images = ref<UploadedImage[]>([])

// ---- 草稿/编辑态 ----
const draft = ref<WrongQuestion | null>(null)
/** 编辑模式的旧 CONFIRMED 记录 id（PUT 目标；重解析成功后 DELETE 目标） */
const editingId = ref('')
/** 编辑模式下离开步骤3 重新走解析流（含「重新解析」按钮与「返回上一步」换图） */
const reparsed = ref(false)

// ---- 枚举 ----
const enums = reactive({ subjects: [] as string[], gradeLevels: [] as string[], questionTypes: [] as string[] })

// ---- 表单 ----
const form = reactive<FormState>({
  subject: '',
  gradeLevel: '',
  questionType: '',
  questionStem: '',
  questionImages: [],
  sourceName: '',
  questionNumber: '',
  standardAnswer: [''],
  knowledgePoints: [],
  tags: [],
  knowledgeAnalysis: '',
  wrongDate: '',
})

// ---- 知识点联想 / 标签输入 ----
const kpKeyword = ref('')
const kpCandidates = ref<KnowledgePointCandidate[]>([])
const tagInput = ref('')

const canParse = computed(() => images.value.length > 0 && !parsing.value)
const roleLabel: Record<string, string> = { QUESTION: '题目图', ANSWER_SHEET: '解答图' }

function fillForm(d: WrongQuestion): void {
  form.subject = d.subject ?? ''
  form.gradeLevel = d.gradeLevel ?? ''
  form.questionType = d.questionType ?? ''
  form.questionStem = d.questionStem ?? ''
  form.questionImages = d.questionImages ? [...d.questionImages] : []
  form.sourceName = d.sourceName ?? ''
  form.questionNumber = d.questionNumber ?? ''
  form.standardAnswer = d.standardAnswer?.length ? [...d.standardAnswer] : ['']
  form.knowledgePoints = d.knowledgePoints ? [...d.knowledgePoints] : []
  form.tags = d.tags ? [...d.tags] : []
  form.knowledgeAnalysis = d.knowledgeAnalysis ?? ''
  form.wrongDate = d.wrongDate ?? ''
}

/** 解析流程：parseImage（上传压缩结果）→ parse（同步等待，120s 超时在 api 层）。 */
async function runParse(): Promise<void> {
  parsing.value = true
  parseError.value = ''
  step.value = 1
  try {
    const files = images.value.map((i) => i.file)
    const urls = await parseImage(files)
    const refs: ImageRef[] = urls.map((url, i) => ({ url, role: images.value[i].role }))
    draft.value = await parseQuestion(refs)
    fillForm(draft.value)
    step.value = 2
  } catch (e) {
    parseError.value = (e as Error).message
  } finally {
    parsing.value = false
  }
}

/** 转手动填写：POST questions/drafts 建空草稿（零 AI 调用，FR-A4）。 */
async function toManual(): Promise<void> {
  try {
    draft.value = await createEmptyDraft()
    fillForm(draft.value)
    parseError.value = ''
    step.value = 2
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
}

/** 确认：返回上一步（图保留）。编辑模式下离开即视为重解析换图。 */
function onBack(): void {
  if (editingId.value) reparsed.value = true
  step.value = 0
}

/** 「重新解析」：编辑模式换图回步骤1（TC-N-06 前置）。 */
function onReparse(): void {
  reparsed.value = true
  images.value = []
  step.value = 0
}

/** 校验必填并提交（TC-N-04）。 */
async function onConfirm(): Promise<void> {
  if (!form.questionStem.trim()) {
    ElMessage.warning('题干为必填项，请填写题干')
    return
  }
  if (!form.subject) {
    ElMessage.warning('科目为必填项，请选择科目')
    return
  }
  const payload: QuestionDraftInput = {
    ...form,
    standardAnswer: form.standardAnswer.filter((a) => a.trim() !== ''),
  }
  submitting.value = true
  try {
    if (editingId.value && !reparsed.value) {
      // 编辑既有 CONFIRMED 记录：PUT（3.6）
      await updateQuestion(editingId.value, payload)
    } else {
      const id = draft.value?.errorQuestionId ?? editingId.value
      await confirmQuestion(id, payload)
      // 重解析语义（2026-11-12 确认）：confirm 新记录成功后联动删除旧记录（TC-N-06）
      if (reparsed.value && editingId.value) {
        await deleteQuestion(editingId.value)
      }
    }
    done.value = true
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    submitting.value = false
  }
}

// ---- 答案行 ----
function addAnswer(): void {
  form.standardAnswer.push('')
}
function removeAnswer(i: number): void {
  form.standardAnswer.splice(i, 1)
}

// ---- 知识点（BR-03 归并：≥1 字联想，点选复用；也可自由输入）----
watch(kpKeyword, async (v) => {
  const kw = v.trim()
  if (!kw) {
    kpCandidates.value = []
    return
  }
  try {
    kpCandidates.value = await getKnowledgePoints(kw, 10)
  } catch {
    kpCandidates.value = []
  }
})

function pickKp(name: string): void {
  if (!form.knowledgePoints.includes(name)) form.knowledgePoints.push(name)
  kpKeyword.value = ''
  kpCandidates.value = []
}
function addKpOnEnter(): void {
  const v = kpKeyword.value.trim()
  if (v) pickKp(v)
}
function removeKp(i: number): void {
  form.knowledgePoints.splice(i, 1)
}

// ---- 标签（FR-B6：自由多标签，回车添加、无联想、与知识点独立）----
function addTagOnEnter(): void {
  const v = tagInput.value.trim()
  if (v && !form.tags.includes(v)) form.tags.push(v)
  tagInput.value = ''
}
function removeTag(i: number): void {
  form.tags.splice(i, 1)
}

function backToList(): void {
  void router.push('/questions')
}

/** 继续录入下一题：重置全部状态。 */
function resetAll(): void {
  void router.replace('/questions/new')
  step.value = 0
  done.value = false
  parsing.value = false
  parseError.value = ''
  images.value = []
  draft.value = null
  editingId.value = ''
  reparsed.value = false
  kpKeyword.value = ''
  kpCandidates.value = []
  tagInput.value = ''
  fillForm({
    errorQuestionId: '',
    subject: '',
    questionStem: '',
    questionImages: [],
    status: 'PENDING_CONFIRM',
  } as WrongQuestion)
  form.standardAnswer = ['']
}

onMounted(async () => {
  try {
    const e = await getEnums()
    enums.subjects = e.subjects ?? []
    enums.gradeLevels = e.gradeLevels ?? []
    enums.questionTypes = e.questionTypes ?? []
  } catch {
    ElMessage.error('枚举字典加载失败')
  }
  const id = typeof route.query.id === 'string' ? route.query.id : ''
  if (!id) return
  // 编辑（CONFIRMED）/草稿续编（PENDING_CONFIRM）均直达步骤3 预填
  try {
    const q = await getQuestion(id)
    draft.value = q
    fillForm(q)
    step.value = 2
    if (q.status === 'CONFIRMED') editingId.value = id
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
})
</script>

<template>
  <div class="page">
    <div class="head-row">
      <h1 class="page-title">{{ done ? '录入完成' : '新增错题' }}</h1>
      <el-button link @click="backToList">返回列表</el-button>
    </div>

    <el-steps :active="done ? 3 : step" finish-status="success" align-center>
      <el-step title="上传图片" />
      <el-step title="AI 解析" />
      <el-step title="分项确认" />
    </el-steps>

    <!-- ===== 完成态 ===== -->
    <div v-if="done" class="center-card">
      <el-result icon="success" title="录入成功" sub-title="错题已确认入库，可在列表中查看。">
        <template #extra>
          <el-button type="primary" @click="backToList">返回列表</el-button>
          <el-button @click="resetAll">继续录入下一题</el-button>
        </template>
      </el-result>
    </div>

    <!-- ===== 步骤1 上传 ===== -->
    <template v-else-if="step === 0">
      <div class="step-body">
        <ImageUploader v-model="images" />
        <div class="parse-bar">
          <el-button type="primary" size="large" :disabled="!canParse" @click="runParse">
            开始解析（{{ images.length }} 张图）
          </el-button>
          <span v-if="!images.length" class="hint">请先上传至少 1 张图片（题目图优先，解答图次之）</span>
        </div>
      </div>
    </template>

    <!-- ===== 步骤2 解析中 / 失败态 ===== -->
    <div v-else-if="step === 1" class="step-body center-card">
      <div v-if="parsing" v-loading="true" class="parsing-box" element-loading-text="AI 解析中">
        <p class="p-title">AI 解析中……</p>
        <p class="hint">预计 10~60 秒，请勿关闭页面。超时上限 120 秒。</p>
      </div>
      <el-result
        v-else-if="parseError"
        icon="error"
        title="解析失败"
        :sub-title="parseError"
      >
        <template #extra>
          <el-button type="primary" @click="runParse">重试</el-button>
          <el-button @click="toManual">转手动填写</el-button>
        </template>
      </el-result>
    </div>

    <!-- ===== 步骤3 分项确认 ===== -->
    <div v-else class="step-body">
      <el-form label-width="90px" class="confirm-form" @submit.prevent>
        <!-- 基本信息 -->
        <h3 class="sec">基本信息</h3>
        <div class="grid-row">
          <el-form-item label="科目" required>
            <el-select v-model="form.subject" placeholder="选择科目">
              <el-option v-for="s in enums.subjects" :key="s" :label="s" :value="s" />
            </el-select>
          </el-form-item>
          <el-form-item label="年级">
            <el-select v-model="form.gradeLevel" placeholder="选择年级" clearable>
              <el-option v-for="g in enums.gradeLevels" :key="g" :label="g" :value="g" />
            </el-select>
          </el-form-item>
          <el-form-item label="题型">
            <el-select v-model="form.questionType" placeholder="选择题型" clearable>
              <el-option v-for="t in enums.questionTypes" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
          <el-form-item label="错题日期">
            <el-date-picker v-model="form.wrongDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
          </el-form-item>
          <el-form-item label="来源">
            <el-input v-model="form.sourceName" placeholder="如：期中试卷" />
          </el-form-item>
          <el-form-item label="题号">
            <el-input v-model="form.questionNumber" placeholder="如：12" />
          </el-form-item>
        </div>

        <!-- 题干 -->
        <h3 class="sec">
          题干
          <el-button v-if="editingId" link type="warning" size="small" class="reparse" @click="onReparse">
            重新解析（换图）
          </el-button>
        </h3>
        <el-input
          v-model="form.questionStem"
          type="textarea"
          :rows="4"
          placeholder="题干为必填项；支持 LaTeX（$...$）"
        />

        <!-- 图片预览 -->
        <template v-if="form.questionImages.length">
          <h3 class="sec">图片预览</h3>
          <div class="img-row">
            <div v-for="(img, i) in form.questionImages" :key="i" class="img-cell">
              <span class="role-badge" :class="img.role">{{ roleLabel[img.role] ?? img.role }}</span>
              <img :src="fileUrl(img.url)" :alt="`图片${i + 1}`" />
            </div>
          </div>
        </template>

        <!-- 标准答案 -->
        <h3 class="sec">标准答案</h3>
        <div v-for="(_, i) in form.standardAnswer" :key="i" class="answer-row">
          <el-input v-model="form.standardAnswer[i]" :placeholder="`答案 ${i + 1}`" type="textarea" :rows="1" />
          <el-button link type="danger" @click="removeAnswer(i)">删</el-button>
        </div>
        <el-button size="small" @click="addAnswer">＋ 添加答案</el-button>

        <!-- 知识点（联想归并） -->
        <h3 class="sec">知识点</h3>
        <div class="kp-wrap">
          <div class="kp-list">
            <span v-for="(kp, i) in form.knowledgePoints" :key="kp" class="kp-item">
              {{ kp }}
              <button type="button" class="kp-remove" @click="removeKp(i)">×</button>
            </span>
          </div>
          <div class="kp-input-box">
            <el-input
              v-model="kpKeyword"
              placeholder="输入知识点，回车添加（≥1 字联想已有知识点）"
              @keydown.enter.prevent="addKpOnEnter"
            />
            <ul v-if="kpCandidates.length" class="kp-sug">
              <li v-for="c in kpCandidates" :key="c.name">
                <button type="button" class="kp-candidate" @click="pickKp(c.name)">
                  {{ c.name }}<span class="use-count">（{{ c.useCount }} 次）</span>
                </button>
              </li>
            </ul>
          </div>
        </div>

        <!-- 标签 -->
        <h3 class="sec">标签</h3>
        <div class="kp-wrap">
          <div class="kp-list">
            <span v-for="(t, i) in form.tags" :key="t" class="free-tag">
              {{ t }}
              <button type="button" class="kp-remove" @click="removeTag(i)">×</button>
            </span>
          </div>
          <el-input
            v-model="tagInput"
            placeholder="输入标签，回车添加（自由标注，不联想）"
            @keydown.enter.prevent="addTagOnEnter"
          />
        </div>

        <!-- 解析 -->
        <h3 class="sec">解析</h3>
        <el-input
          v-model="form.knowledgeAnalysis"
          type="textarea"
          :rows="6"
          placeholder="支持 Markdown 与 LaTeX（$$...$$）"
        />
      </el-form>

      <div class="foot-bar">
        <el-button @click="onBack">返回上一步</el-button>
        <el-button type="primary" :loading="submitting" @click="onConfirm">确认录入</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-title {
  font-size: 18px;
  margin: 0;
}
.head-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.step-body {
  margin-top: 20px;
}
.center-card {
  display: flex;
  justify-content: center;
  padding: 32px 0;
}
.parsing-box {
  width: 420px;
  height: 140px;
  text-align: center;
  padding-top: 30px;
}
.p-title {
  font-size: 16px;
  margin: 0 0 8px;
}
.hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.parse-bar {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}
.sec {
  font-size: 14px;
  margin: 18px 0 8px;
}
.grid-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  column-gap: 16px;
}
.reparse {
  margin-left: 12px;
}
.img-row {
  display: flex;
  gap: 12px;
  overflow-x: auto;
}
.img-cell {
  position: relative;
  flex: 0 0 auto;
}
.img-cell img {
  height: 110px;
  border-radius: 4px;
  border: 1px solid var(--el-border-color-light);
  display: block;
}
.role-badge {
  position: absolute;
  left: 0;
  bottom: 0;
  font-size: 12px;
  padding: 1px 6px;
  border-radius: 0 4px 0 0;
  color: #fff;
  background: var(--el-color-primary);
  z-index: 1;
}
.role-badge.ANSWER_SHEET {
  background: var(--el-color-success);
}
.answer-row {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-bottom: 6px;
}
.kp-wrap {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.kp-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.kp-item {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 3px;
  background: var(--el-color-primary-light-9);
  border: 1px solid var(--el-color-primary-light-7);
  color: var(--el-color-primary);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.free-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 3px;
  background: var(--el-fill-color-light);
  border: 1px dashed var(--el-border-color);
  color: var(--el-text-color-secondary);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.kp-remove {
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 0 2px;
  color: inherit;
  font-size: 12px;
  line-height: 1;
}
.kp-input-box {
  position: relative;
  max-width: 420px;
}
.kp-sug {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 10;
  list-style: none;
  margin: 2px 0 0;
  padding: 4px 0;
  background: #fff;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  box-shadow: var(--el-box-shadow-light);
}
.kp-sug li {
  list-style: none;
}
.kp-candidate {
  width: 100%;
  text-align: left;
  border: none;
  background: transparent;
  padding: 5px 12px;
  cursor: pointer;
  font-size: 13px;
}
.kp-candidate:hover {
  background: var(--el-fill-color-light);
}
.use-count {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.foot-bar {
  margin-top: 24px;
  display: flex;
  justify-content: center;
  gap: 12px;
}
</style>
