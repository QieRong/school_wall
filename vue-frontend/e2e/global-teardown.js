/**
 * Playwright全局清理脚本
 * 在所有测试完成后执行，清理测试过程中产生的数据
 */

export default async function globalTeardown() {
  console.log('开始清理测试数据...')
  
  // 这里可以添加数据库清理逻辑
  // 例如：删除测试过程中创建的用户、帖子等
  
  // 注意：由于数据库连接需要额外配置，这里仅作为示例
  // 实际使用时可以通过MySQL客户端或执行SQL脚本来清理
  
  console.log('测试数据清理完成')
}
