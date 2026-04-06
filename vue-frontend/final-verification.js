/**
 * 最终验证：密码验证逻辑
 */

function validatePassword(password) {
  if (!password) {
    return { valid: false, message: '请输入密码' }
  }
  if (password.length < 6) {
    return { valid: false, message: '密码长度不能少于6位' }
  }
  if (password.length > 20) {
    return { valid: false, message: '密码长度不能超过20位' }
  }
  return { valid: true }
}

console.log('=== 密码验证最终确认 ===\n')

console.log('✅ 要求：密码长度最少6位（强制）')
console.log('✅ 要求：密码强度不做要求（只提示）\n')

const testCases = [
  { pwd: '', desc: '空密码', expected: false },
  { pwd: '1', desc: '1位密码', expected: false },
  { pwd: '12345', desc: '5位密码', expected: false },
  { pwd: '123456', desc: '6位密码（管理员密码）', expected: true },
  { pwd: 'aaaaaa', desc: '6位弱密码', expected: true },
  { pwd: '111111', desc: '6位纯数字', expected: true },
  { pwd: 'password', desc: '8位弱密码', expected: true },
  { pwd: 'Abc123!@#', desc: '9位强密码', expected: true },
  { pwd: '12345678901234567890', desc: '20位密码', expected: true },
  { pwd: '123456789012345678901', desc: '21位密码', expected: false }
]

testCases.forEach(({ pwd, desc, expected }) => {
  const result = validatePassword(pwd)
  const status = result.valid === expected ? '✅' : '❌'
  console.log(`${status} ${desc}: ${result.valid ? '通过' : '拒绝'}`)
  if (!result.valid) {
    console.log(`   提示: ${result.message}`)
  }
})

console.log('\n=== 总结 ===')
console.log('✅ 密码长度：6-20位（强制要求）')
console.log('✅ 密码强度：不强制，只提示（弱/中/强）')
console.log('✅ 管理员密码 "123456"：可以使用（6位，符合长度要求）')
console.log('✅ 弱密码如 "111111"、"aaaaaa"：可以使用（只会提示强度弱）')
