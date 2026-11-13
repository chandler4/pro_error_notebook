// T9-1 walkthrough fixture: render a real math-question image for the live AI parse check (TC-N-05).
// Output: %TEMP%/errnote-walkthrough/q.png + a.png (question + answer sheet, two images like the real flow)
const path = require('path')
const fs = require('fs')
const os = require('os')
const { createCanvas } = require(path.join(__dirname, '..', 'node_modules', 'canvas'))

const outDir = path.join(os.tmpdir(), 'errnote-walkthrough')
fs.mkdirSync(outDir, { recursive: true })

function draw(file, title, lines) {
  const w = 900,
    h = 360
  const canvas = createCanvas(w, h)
  const ctx = canvas.getContext('2d')
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(0, 0, w, h)
  ctx.fillStyle = '#000000'
  ctx.font = 'bold 28px "Microsoft YaHei"'
  ctx.fillText(title, 40, 60)
  ctx.font = '26px "Microsoft YaHei"'
  lines.forEach((line, i) => ctx.fillText(line, 40, 130 + i * 46))
  fs.writeFileSync(path.join(outDir, file), canvas.toBuffer('image/png'))
  console.log(path.join(outDir, file))
}

draw('q.png', '2026-2027 学年度九年级数学期中试卷  第15题', [
  '已知二次函数 y = x^2 - 4x + 3。',
  '(1) 求该抛物线的顶点坐标；',
  '(2) 当 0 <= x <= 3 时，求 y 的取值范围。',
])

draw('a.png', '答题纸（学生答案）', [
  '解：y = x^2 - 4x + 3 = (x-2)^2 - 1',
  '顶点坐标为 (2, -1)。',
  '当 0 <= x <= 3 时，y 最小值 -1，最大值 3，',
  '所以 -1 <= y <= 3。',
])
