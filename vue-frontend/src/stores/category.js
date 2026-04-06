/**
 * 分类数据Store
 * 从后端动态获取分类列表，替代硬编码分类
 */
import { defineStore } from 'pinia'
import request from '@/api/request'

export const useCategoryStore = defineStore('category', {
  state: () => ({
    categories: [],
    loading: false,
    loaded: false
  }),

  getters: {
    /**
     * 获取分类名称
     */
    getCategoryName: (state) => (id) => {
      const cat = state.categories.find(c => c.id === id)
      return cat?.name || '未知分类'
    }
  },

  actions: {
    /**
     * 获取分类列表（带缓存）
     */
    async fetchCategories() {
      // 如果已加载，直接返回
      if (this.loaded) return

      this.loading = true
      try {
        const res = await request.get('/index/categories')
        if (res.code === '200' || res.code === 200) {
          this.categories = res.data || []
          this.loaded = true
        }
      } catch (error) {
        console.error('获取分类失败:', error)
      } finally {
        this.loading = false
      }
    },

    /**
     * 强制刷新分类
     */
    async refreshCategories() {
      this.loaded = false
      await this.fetchCategories()
    }
  }
})
