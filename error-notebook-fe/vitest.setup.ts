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

/**
 * 资源加载过滤：resources=usable 时 jsdom 会真实抓取每个 <img src>（blob: 预览、
 * /files/ 服务端图）——这里替换内层 loader 的 fetch，仅放行 data: URL（image 压缩
 * 链路依赖其解码派发 load），其余立即拒绝（派发 error，不发网络请求）。
 * 注意：setup 里的 document 是 wrapper，内部属性须走 implForWrapper 取 impl 后原地替换
 * （跨模块图 instanceof 会失败，无法整体换 loader 实例）。
 */
type Loader = { fetch: (url: string, opts: unknown) => unknown }

function patchResourceLoader(): void {
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  const { implForWrapper } = require('jsdom/lib/jsdom/living/generated/utils') as {
    implForWrapper: (w: unknown) => { _resourceLoader?: { _resourceLoader?: Loader } } | null
  }
  const impl = implForWrapper(document)
  const innerLoader = impl?._resourceLoader?._resourceLoader
  if (!innerLoader || '___patchedByErrnote' in innerLoader) return
  const origFetch = innerLoader.fetch.bind(innerLoader)
  innerLoader.fetch = (url: string, opts: unknown) => {
    if (typeof url === 'string' && url.startsWith('data:')) return origFetch(url, opts)
    return Promise.reject(new Error(`test env: skip external resource ${url}`))
  }
  Object.defineProperty(innerLoader, '___patchedByErrnote', { value: true })
}

patchResourceLoader()

export {}
