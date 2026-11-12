import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import CropDialog from '../CropDialog.vue'
import { attachCropper, cropToBlob, destroyCropper } from '../../utils/crop'

vi.mock('../../utils/crop', () => ({
  attachCropper: vi.fn(() => ({ fake: 'cropper' })),
  cropToBlob: vi.fn(async () => new Blob(['cropped-bytes'], { type: 'image/jpeg' })),
  destroyCropper: vi.fn(),
}))

const image = { url: 'blob:mock-1', name: 'a.png' }

function mountDialog(visible: boolean) {
  return mount(CropDialog, {
    props: {
      modelValue: visible,
      image,
      'onUpdate:modelValue': (v: boolean) => void v,
      onCropped: (b: Blob) => void b,
    },
    global: { plugins: [ElementPlus] },
  })
}

/** 裁剪弹窗生命周期：挂接/输出/释放（FR-A2）。 */
describe('CropDialog', () => {
  beforeEach(() => vi.clearAllMocks())

  it('打开时挂接 cropper，关闭时释放', async () => {
    const wrapper = mountDialog(true)
    await flushPromises()
    expect(attachCropper).toHaveBeenCalledTimes(1)

    await wrapper.findAll('button').find((b) => b.text() === '取消')!.trigger('click')
    await flushPromises()
    expect(destroyCropper).toHaveBeenCalled()
  })

  it('确定裁剪：输出裁剪 Blob 并关闭', async () => {
    const wrapper = mountDialog(true)
    await flushPromises()
    await wrapper.findAll('button').find((b) => b.text() === '确定裁剪')!.trigger('click')
    await flushPromises()
    expect(cropToBlob).toHaveBeenCalledTimes(1)
    const cropped = wrapper.emitted('cropped')
    expect(cropped).toBeTruthy()
    expect((cropped![0][0] as Blob).type).toBe('image/jpeg')
    const closed = wrapper.emitted('update:modelValue')!
    expect(closed[closed.length - 1][0]).toBe(false)
  })
})
