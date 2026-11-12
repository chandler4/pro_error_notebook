/**
 * 测试工具：canvas 造指定尺寸图片（零网络零外部资源，test-strategy §5）。
 * 依赖 jsdom + node canvas（devDependency `canvas`，jsdom 自动挂接原生实现）。
 */
export async function makeImage(width: number, height: number): Promise<File> {
  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const ctx = canvas.getContext('2d')
  if (!ctx) throw new Error('canvas 2d context unavailable in test env')
  ctx.fillStyle = '#3366aa'
  ctx.fillRect(0, 0, width, height)
  ctx.fillStyle = '#ffffff'
  ctx.font = '16px sans-serif'
  ctx.fillText(`${width}x${height}`, 10, 24)
  const blob = await canvasToBlob(canvas)
  return new File([blob], `test-${width}x${height}.png`, { type: 'image/png' })
}

export function canvasToBlob(canvas: HTMLCanvasElement, type = 'image/png', quality?: number): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (b) => (b ? resolve(b) : reject(new Error('canvas.toBlob returned null'))),
      type,
      quality,
    )
  })
}

/** 读取图片 Blob 的自然尺寸（dataURL 加载，jsdom/canvas 可解码）。 */
export async function imageSize(blob: Blob): Promise<{ width: number; height: number }> {
  const dataUrl = await new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = () => reject(new Error('file read failed'))
    reader.readAsDataURL(blob)
  })
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve({ width: img.naturalWidth, height: img.naturalHeight })
    img.onerror = () => reject(new Error('image load failed'))
    img.src = dataUrl
  })
}
