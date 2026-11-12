<script setup lang="ts">
// 步骤1 图片上传器（prototype-spec §2.2 / FR-A1/A2）：
// 多图（≤6）、角色徽标切换、裁剪（CropDialog）、删除；加入时即完成压缩（长边 ≤2000px）。
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { ImageRole } from '../types'
import { compress } from '../utils/image'
import CropDialog from './CropDialog.vue'

export interface UploadedImage {
  file: File
  role: ImageRole
  /** 预览 objectURL（组件负责创建/释放） */
  url: string
}

const MAX_IMAGES = 6

const props = defineProps<{ modelValue: UploadedImage[] }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: UploadedImage[]): void }>()

const fileInput = ref<HTMLInputElement | null>(null)
const dragging = ref(false)
const cropVisible = ref(false)
const cropIndex = ref(-1)

async function addFiles(files: FileList | File[]): Promise<void> {
  const incoming = Array.from(files).filter((f) => f.type.startsWith('image/'))
  if (!incoming.length) return
  const space = MAX_IMAGES - props.modelValue.length
  if (incoming.length > space) ElMessage.warning(`最多上传 ${MAX_IMAGES} 张图片`)
  const take = incoming.slice(0, Math.max(0, space))
  if (!take.length) return
  // 压缩在上传前完成（架构 2026-11-11 决策：仅存处理结果图）；兜底包装为 File 保住文件名
  const added: UploadedImage[] = []
  for (const f of take) {
    const out = await compress(f)
    const file = out instanceof File ? out : new File([out], f.name, { type: out.type })
    added.push({ file, role: 'QUESTION', url: URL.createObjectURL(file) })
  }
  emit('update:modelValue', [...props.modelValue, ...added])
}

function onDrop(e: DragEvent): void {
  dragging.value = false
  void addFiles(e.dataTransfer?.files ?? [])
}

function onChange(e: Event): void {
  const input = e.target as HTMLInputElement
  void addFiles(input.files ?? [])
  input.value = ''
}

function pick(): void {
  fileInput.value?.click()
}

function toggleRole(i: number): void {
  const next = [...props.modelValue]
  next[i] = { ...next[i], role: next[i].role === 'QUESTION' ? 'ANSWER_SHEET' : 'QUESTION' }
  emit('update:modelValue', next)
}

function remove(i: number): void {
  URL.revokeObjectURL(props.modelValue[i].url)
  emit('update:modelValue', props.modelValue.filter((_, idx) => idx !== i))
}

function openCrop(i: number): void {
  cropIndex.value = i
  cropVisible.value = true
}

async function onCropped(blob: Blob): Promise<void> {
  const i = cropIndex.value
  if (i < 0 || i >= props.modelValue.length) return
  const cur = props.modelValue[i]
  const base = cur.file.name.replace(/\.[^.]+$/, '')
  const next = [...props.modelValue]
  next[i] = {
    file: new File([blob], `${base}-cropped.jpg`, { type: 'image/jpeg' }),
    role: cur.role,
    url: URL.createObjectURL(blob),
  }
  URL.revokeObjectURL(cur.url)
  emit('update:modelValue', next)
}
</script>

<template>
  <div class="uploader">
    <div
      class="dropzone"
      :class="{ dragging }"
      @click="pick"
      @dragover.prevent="dragging = true"
      @dragleave.prevent="dragging = false"
      @drop.prevent="onDrop"
    >
      <div class="dz-tip">拖拽图片到此处，或点击选择</div>
      <div class="dz-sub">支持 jpg/png，单次最多 {{ MAX_IMAGES }} 张；上传前自动压缩（长边 ≤2000px）</div>
      <input ref="fileInput" type="file" accept="image/*" multiple @change="onChange" />
    </div>

    <div v-if="modelValue.length" class="thumbs">
      <div v-for="(img, i) in modelValue" :key="img.url" class="thumb">
        <img :src="img.url" :alt="`图片${i + 1}`" />
        <button
          type="button"
          class="role-badge"
          :class="img.role"
          :title="img.role === 'QUESTION' ? '点击切换为解答图' : '点击切换为题目图'"
          @click.stop="toggleRole(i)"
        >
          {{ img.role === 'QUESTION' ? '题目图' : '解答图' }}
        </button>
        <div class="ops">
          <button type="button" @click.stop="openCrop(i)">裁剪</button>
          <button type="button" class="danger" @click.stop="remove(i)">删除</button>
        </div>
      </div>
    </div>

    <CropDialog
      v-model="cropVisible"
      :image="cropIndex >= 0 && cropIndex < modelValue.length
        ? { url: modelValue[cropIndex].url, name: modelValue[cropIndex].file.name }
        : null"
      @cropped="onCropped"
    />
  </div>
</template>

<style scoped>
.dropzone {
  border: 1px dashed var(--el-border-color);
  border-radius: 8px;
  padding: 28px 16px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s;
}
.dropzone.dragging {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.dropzone input[type='file'] {
  display: none;
}
.dz-tip {
  font-size: 15px;
  color: var(--el-text-color-primary);
}
.dz-sub {
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.thumbs {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 12px;
}
.thumb {
  position: relative;
  width: 132px;
}
.thumb img {
  width: 132px;
  height: 100px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid var(--el-border-color-light);
  display: block;
}
.role-badge {
  position: absolute;
  top: 0;
  left: 0;
  font-size: 12px;
  padding: 1px 6px;
  border: none;
  border-radius: 0 0 4px 0;
  color: #fff;
  background: var(--el-color-primary);
  cursor: pointer;
}
.role-badge.ANSWER_SHEET {
  background: var(--el-color-success);
}
.ops {
  display: flex;
  gap: 6px;
  margin-top: 4px;
}
.ops button {
  flex: 1;
  font-size: 12px;
  padding: 2px 0;
  border: 1px solid var(--el-border-color);
  border-radius: 3px;
  background: #fff;
  cursor: pointer;
}
.ops button.danger {
  color: var(--el-color-danger);
  border-color: var(--el-color-danger-light-5);
}
</style>
