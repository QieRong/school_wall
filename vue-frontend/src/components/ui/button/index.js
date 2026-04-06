export { default as Button } from './Button.vue'

// 导出按钮变体配置供其他组件使用
export const buttonConfig = {
  variants: ['primary', 'secondary', 'outline', 'ghost', 'danger'],
  sizes: ['sm', 'md', 'lg'],
  defaultVariant: 'primary',
  defaultSize: 'md'
}
