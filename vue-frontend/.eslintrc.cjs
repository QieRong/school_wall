module.exports = {
  root: true,
  env: {
    browser: true,
    es2021: true,
    node: true
  },
  extends: [
    'plugin:vue/vue3-recommended'
  ],
  parserOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module'
  },
  rules: {
    // 允许 Vue 3 的 v-model 自定义参数（如 v-model:open）
    'vue/no-v-model-argument': 'off',
    
    // 关闭过于严格的格式规则
    'vue/html-self-closing': 'off',
    'vue/max-attributes-per-line': 'off',
    'vue/first-attribute-linebreak': 'off',
    'vue/html-closing-bracket-newline': 'off',
    'vue/singleline-html-element-content-newline': 'off',
    'vue/multiline-html-element-content-newline': 'off',
    'vue/html-indent': 'off',
    'vue/attributes-order': 'off',
    
    // 其他常用规则
    'vue/multi-word-component-names': 'off',
    'vue/no-unused-vars': 'warn',
    'no-console': 'off',
    'no-debugger': 'warn',
    'no-unused-vars': 'warn'
  }
}
