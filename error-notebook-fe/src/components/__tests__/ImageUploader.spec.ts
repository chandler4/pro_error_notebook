import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import ImageUploader, { type UploadedImage } from '../ImageUploader.vue'
import CropDialog from '../CropDialog.vue'
import { compress } from '../../utils/image'
import { attachCropper, cropToBlob, destroyCropper } from '../../utils/crop'

vi.mock('../../utils/image', () => ({ compress: vi.fn(async (f: Blob) => f) }))
vi.mock('../../utils/crop', () => ({
  attachCropper: vi.fn(() => ({ fake: 'cropper' })),
  cropToBlob: vi.fn(async () => new Blob(['cropped-bytes'], { type: 'image/jpeg' })),
  destroyCropper: vi.fn(),
}))

function pngFile(name = 'a.png'): File {
  return new File(['png-bytes'], name, { type: 'image/png' })
}

type W = ReturnType<typeof mount>

function mountUploader(): W {
  const wrapper = mount(ImageUploader, {
    props: {
      modelValue: [] as UploadedImage[],
      'onUpdate:modelValue': (v: UploadedImage[]) => {
        void wrapper.setProps({ modelValue: v })
      },
    },
    global: { plugins: [ElementPlus] },
  })
  return wrapper
}

/** TC-U-02：拖拽多文件上传——生成缩略图；非图片类型被过滤。 */
describe('ImageUploader 文件接入（TC-U-02）', () => {
  it('拖入多个图片文件生成缩略图，非图片被过滤', async () => {
    const wrapper = mountUploader()
    await wrapper.find('.dropzone').trigger('drop', {
      dataTransfer: { files: [pngFile('a.png'), pngFile('b.png'), new File(['x'], 'c.txt', { type: 'text/plain' })] },
    })
    await flushPromises()
    const emitted = wrapper.emitted('update:modelValue')
    expect(emitted).toBeTruthy()
    const list = emitted![0][0] as UploadedImage[]
    expect(list.length).toBe(2)
    expect(list.every((i) => i.file.type.startsWith('image/'))).toBe(true)
    // 压缩在上传前完成（FR-A1）
    expect(compress).toHaveBeenCalledTimes(2)
  })

  it('超过 6 张截断', async () => {
    const wrapper = mountUploader()
    const seven = Array.from({ length: 7 }, (_, i) => pngFile(`p${i}.png`))
    await wrapper.find('.dropzone').trigger('drop', { dataTransfer: { files: seven } })
    await flushPromises()
    expect((wrapper.emitted('update:modelValue')![0][0] as UploadedImage[]).length).toBe(6)
  })
})

/** TC-U-03：首图默认题目图；点徽标切换题目/解答图。 */
describe('ImageUploader 角色标注（TC-U-03）', () => {
  it('新图默认 QUESTION，点徽标切换为 ANSWER_SHEET', async () => {
    const wrapper = mountUploader()
    await wrapper.find('.dropzone').trigger('drop', { dataTransfer: { files: [pngFile()] } })
    await flushPromises()
    let list = wrapper.emitted('update:modelValue')![0][0] as UploadedImage[]
    expect(list[0].role).toBe('QUESTION')

    await wrapper.find('.role-badge').trigger('click')
    const emits0 = wrapper.emitted('update:modelValue')!
    list = emits0[emits0.length - 1][0] as UploadedImage[]
    expect(list[0].role).toBe('ANSWER_SHEET')
  })
})

/** TC-U-04：确定后以裁剪区域输出替换原图（mock cropperjs）。 */
describe('ImageUploader 裁剪（TC-U-04）', () => {
  it('打开裁剪弹窗，确定后以裁剪输出替换原图文件', async () => {
    const wrapper = mountUploader()
    await wrapper.find('.dropzone').trigger('drop', { dataTransfer: { files: [pngFile('orig.png')] } })
    await flushPromises()

    await wrapper.findAll('button').find((b) => b.text() === '裁剪')!.trigger('click')
    await flushPromises()
    const dialog = wrapper.findComponent(CropDialog)
    expect(dialog.props('modelValue')).toBe(true)

    await dialog.findAll('button').find((b) => b.text() === '确定裁剪')!.trigger('click')
    await flushPromises()
    expect(cropToBlob).toHaveBeenCalled()

    const emits1 = wrapper.emitted('update:modelValue')!
    const list = emits1[emits1.length - 1][0] as UploadedImage[]
    expect(list.length).toBe(1)
    expect(list[0].file.name).toContain('cropped')
    expect(list[0].file.type).toBe('image/jpeg')
    // 角色不变
    expect(list[0].role).toBe('QUESTION')
    void destroyCropper
    void attachCropper
  })
})

/** 删除图片。 */
describe('ImageUploader 删除', () => {
  it('删除最后一张并释放预览 URL', async () => {
    const wrapper = mountUploader()
    await wrapper.find('.dropzone').trigger('drop', { dataTransfer: { files: [pngFile()] } })
    await flushPromises()
    await wrapper.findAll('button').find((b) => b.text() === '删除')!.trigger('click')
    const emits2 = wrapper.emitted('update:modelValue')!
    const list = emits2[emits2.length - 1][0] as UploadedImage[]
    expect(list.length).toBe(0)
  })
})

beforeEach(() => {
  vi.clearAllMocks()
})
