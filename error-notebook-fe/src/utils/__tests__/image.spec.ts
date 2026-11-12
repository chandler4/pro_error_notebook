import { describe, it, expect } from 'vitest'
import { compress, computeTargetSize } from '../image'
import { makeImage, imageSize } from '../../test-utils/makeImage'

/** TC-U-01 规约1：图片压缩纯逻辑——长边超过 2000px 等比压缩到 2000px，不放大小图。 */
describe('image.compress（TC-U-01）', () => {
  it('4000x3000 等比压缩至 2000x1500', async () => {
    const file = await makeImage(4000, 3000)
    const out = await compress(file, { maxEdge: 2000 })
    expect(await imageSize(out)).toEqual({ width: 2000, height: 1500 })
  })

  it('竖图 3000x6000 压缩至 1000x2000（长边=高）', async () => {
    const file = await makeImage(3000, 6000)
    const out = await compress(file, { maxEdge: 2000 })
    expect(await imageSize(out)).toEqual({ width: 1000, height: 2000 })
  })

  it('小图 800x600 不放大', async () => {
    const file = await makeImage(800, 600)
    const out = await compress(file, { maxEdge: 2000 })
    expect(await imageSize(out)).toEqual({ width: 800, height: 600 })
  })

  it('恰好 2000 宽不处理', async () => {
    const file = await makeImage(2000, 1000)
    const out = await compress(file, { maxEdge: 2000 })
    expect(await imageSize(out)).toEqual({ width: 2000, height: 1000 })
  })

  it('输出为 JPEG（压缩后体积可控）', async () => {
    const file = await makeImage(3000, 2000)
    const out = await compress(file, { maxEdge: 2000 })
    expect(out.type).toBe('image/jpeg')
  })
})

/** 纯函数：目标尺寸计算（表驱动）。 */
describe('computeTargetSize', () => {
  it.each([
    [4000, 3000, 2000, { width: 2000, height: 1500 }],
    [3000, 6000, 2000, { width: 1000, height: 2000 }],
    [800, 600, 2000, { width: 800, height: 600 }],
    [2000, 1000, 2000, { width: 2000, height: 1000 }],
    [100, 100, 2000, { width: 100, height: 100 }],
  ])('computeTargetSize(%i, %i, %i) -> %j', (w, h, max, expected) => {
    expect(computeTargetSize(w, h, max)).toEqual(expected)
  })
})
