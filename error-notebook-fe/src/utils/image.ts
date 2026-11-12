/**
 * 图片处理工具（F-1，FR-A1/A2：上传前压缩，长边 <= 2000px 等比，不放大小图）。
 * 裁剪（cropperjs）见 utils/crop.ts；压缩在上传前完成（仅存处理结果图，架构 2026-11-11 15:38 决策）。
 */

export interface CompressOptions {
  /** 长边上限，默认 2000（规约 FR-A2） */
  maxEdge?: number
  /** JPEG 质量，默认 0.85 */
  quality?: number
}

/** 纯函数：目标尺寸计算（等比、只缩不放）。 */
export function computeTargetSize(
  width: number,
  height: number,
  maxEdge: number,
): { width: number; height: number } {
  if (width <= 0 || height <= 0) throw new Error(`invalid image size ${width}x${height}`)
  if (width <= maxEdge && height <= maxEdge) return { width, height }
  const scale = maxEdge / Math.max(width, height)
  return { width: Math.round(width * scale), height: Math.round(height * scale) }
}

/** Blob → dataURL（jsdom/canvas 的 Image 可解码 dataURL；浏览器同兼容，量级一次性无性能问题）。 */
function blobToDataUrl(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = () => reject(new Error('file read failed'))
    reader.readAsDataURL(blob)
  })
}

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve(img)
    img.onerror = () => reject(new Error('image load failed'))
    img.src = src
  })
}

function canvasToBlob(
  canvas: HTMLCanvasElement,
  type = 'image/jpeg',
  quality?: number,
): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (b) => (b ? resolve(b) : reject(new Error('canvas.toBlob returned null'))),
      type,
      quality,
    )
  })
}

/**
 * 压缩图片：长边超过 maxEdge 时等比缩小并转 JPEG；小图原样返回（不放大、不重编码）。
 * 压缩分支包装为 File（沿用原文件名换 .jpg 后缀）——FormData 上传需要文件名。
 */
export async function compress(blob: Blob, opts: CompressOptions = {}): Promise<Blob> {
  const { maxEdge = 2000, quality = 0.85 } = opts
  const img = await loadImage(await blobToDataUrl(blob))
  const { width, height } = computeTargetSize(img.naturalWidth, img.naturalHeight, maxEdge)
  if (width === img.naturalWidth && height === img.naturalHeight) {
    return blob
  }
  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const ctx = canvas.getContext('2d')
  if (!ctx) throw new Error('canvas 2d context unavailable')
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(0, 0, width, height)
  ctx.drawImage(img, 0, 0, width, height)
  const out = await canvasToBlob(canvas, 'image/jpeg', quality)
  const base = (blob instanceof File ? blob.name : 'image').replace(/\.[^.]+$/, '')
  return new File([out], `${base}.jpg`, { type: 'image/jpeg' })
}
