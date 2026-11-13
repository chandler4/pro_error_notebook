// T9-1 walkthrough fixture 2: rework-mode image for edit-reparse-delete-old check (TC-N-06).
const path = require('path')
const fs = require('fs')
const os = require('os')
const { createCanvas } = require(path.join(__dirname, '..', 'node_modules', 'canvas'))

const outDir = path.join(os.tmpdir(), 'errnote-walkthrough')
const w = 900,
  h = 320
const canvas = createCanvas(w, h)
const ctx = canvas.getContext('2d')
ctx.fillStyle = '#ffffff'
ctx.fillRect(0, 0, w, h)
ctx.fillStyle = '#000000'
ctx.font = 'bold 28px "Microsoft YaHei"'
ctx.fillText('2026-2027 学年度九年级数学期中试卷  第16题（订正版）', 40, 60)
ctx.font = '26px "Microsoft YaHei"'
ctx.fillText('已知二次函数 y = x^2 - 4x + 3 的图像与 x 轴交于 A、B 两点，', 40, 130)
ctx.fillText('与 y 轴交于点 C。求三角形 ABC 的面积。', 40, 176)
fs.writeFileSync(path.join(outDir, 'q2.png'), canvas.toBuffer('image/png'))
console.log(path.join(outDir, 'q2.png'))
