/**
 * Vitest 全局 setup（F-1）：jsdom 缺口的运行时 polyfill。
 * URL.createObjectURL/revokeObjectURL 在 jsdom 未实现（下载与图片加载测试需要）。
 */
let blobUrlSeq = 0
const blobUrlStore = new Map<string, Blob>()

if (typeof URL.createObjectURL !== 'function') {
  URL.createObjectURL = (blob: Blob): string => {
    const url = `blob:mock-${++blobUrlSeq}`
    blobUrlStore.set(url, blob)
    return url
  }
}

if (typeof URL.revokeObjectURL !== 'function') {
  URL.revokeObjectURL = (url: string): void => {
    blobUrlStore.delete(url)
  }
}

export {}
