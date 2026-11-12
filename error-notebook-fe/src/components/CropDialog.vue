<script setup lang="ts">
// 裁剪弹窗（FR-A2）：cropperjs 框选，确定后输出 JPEG Blob 替换原图。
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import type Cropper from 'cropperjs'
import { ElMessage } from 'element-plus'
import { attachCropper, cropToBlob, destroyCropper } from '../utils/crop'

const props = defineProps<{
  modelValue: boolean
  image: { url: string; name: string } | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'cropped', blob: Blob): void
}>()

const imgEl = ref<HTMLImageElement | null>(null)
const cropper = ref<Cropper | null>(null)
const cropping = ref(false)

function release(): void {
  destroyCropper(cropper.value)
  cropper.value = null
}

/** 函数 ref：el-dialog 内容为懒渲染，img 实际挂载时机由此回调兜底。 */
function bindImg(el: unknown): void {
  imgEl.value = (el as HTMLImageElement) ?? null
  if (imgEl.value) ensureCropper()
}

function ensureCropper(): void {
  if (cropper.value || !imgEl.value || !props.image) return
  cropper.value = attachCropper(imgEl.value)
}

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) {
      release()
      return
    }
    await nextTick()
    ensureCropper() // 重开（内容未销毁）时 img 已在，此处补挂
  },
  { immediate: true },
)

function close(): void {
  release()
  emit('update:modelValue', false)
}

async function onConfirm(): Promise<void> {
  if (!cropper.value) return
  cropping.value = true
  try {
    const blob = await cropToBlob(cropper.value)
    emit('cropped', blob)
    close()
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    cropping.value = false
  }
}

onBeforeUnmount(release)
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="裁剪图片"
    width="620px"
    :close-on-click-modal="false"
    @update:model-value="(v: boolean) => (v || close())"
  >
    <div class="crop-body">
      <img v-if="image" :ref="bindImg" class="crop-img" :src="image.url" :alt="image.name" />
    </div>
    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="cropping" @click="onConfirm">确定裁剪</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.crop-body {
  height: 380px;
  overflow: hidden;
  background: var(--el-fill-color-darker);
}
.crop-img {
  display: block;
  max-width: 100%;
  max-height: 380px;
}
</style>
