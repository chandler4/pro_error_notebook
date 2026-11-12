/**
 * cropperjs 裁剪封装（F-1，FR-A2：人工框选有效区域，仅上传裁剪结果）。
 * 组件用法：CropDialog 内 attachCropper(imgEl) → 用户框选 → cropToBlob(cropper) 输出替换原图。
 */
import Cropper from 'cropperjs'
import 'cropperjs/dist/cropper.css'

export interface CropOptions {
  aspectRatio?: number
}

/** 在图片元素上挂接裁剪器（viewMode 1：裁剪框不越界）。 */
export function attachCropper(imgEl: HTMLImageElement, opts: CropOptions = {}): Cropper {
  return new Cropper(imgEl, {
    viewMode: 1,
    autoCropArea: 1,
    background: false,
    aspectRatio: opts.aspectRatio ?? NaN,
  })
}

/** 输出裁剪结果（上限 2000px，与压缩规格一致；JPEG 0.9）。 */
export function cropToBlob(cropper: Cropper): Promise<Blob> {
  return new Promise((resolve, reject) => {
    try {
      const canvas = cropper.getCroppedCanvas({ maxWidth: 2000, maxHeight: 2000 })
      if (!canvas) {
        reject(new Error('crop canvas unavailable'))
        return
      }
      canvas.toBlob(
        (b: Blob | null) => (b ? resolve(b) : reject(new Error('crop toBlob failed'))),
        'image/jpeg',
        0.9,
      )
    } catch (e) {
      reject(e instanceof Error ? e : new Error(String(e)))
    }
  })
}

/** 释放裁剪器（弹窗关闭时必调，防内存泄漏）。 */
export function destroyCropper(cropper: Cropper | null | undefined): void {
  cropper?.destroy()
}
