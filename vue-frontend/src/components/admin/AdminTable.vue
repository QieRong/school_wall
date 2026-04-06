<script setup>
import { ref, computed } from 'vue'
import { ChevronUp, ChevronDown, ChevronsUpDown } from 'lucide-vue-next'

const props = defineProps({
  columns: {
    type: Array,
    required: true
  },
  data: {
    type: Array,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  selectable: {
    type: Boolean,
    default: false
  },
  pagination: {
    type: Object,
    default: () => ({
      current: 1,
      pageSize: 10,
      total: 0
    })
  }
})

const emit = defineEmits(['selection-change', 'sort-change', 'page-change'])

const selectedRows = ref([])
const sortColumn = ref(null)
const sortOrder = ref(null)

// 全选状态
const isAllSelected = computed(() => {
  return props.data.length > 0 && selectedRows.value.length === props.data.length
})

const isIndeterminate = computed(() => {
  return selectedRows.value.length > 0 && selectedRows.value.length < props.data.length
})

// 切换全选
const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedRows.value = []
  } else {
    selectedRows.value = [...props.data]
  }
  emit('selection-change', selectedRows.value)
}

// 切换单行选择
const toggleSelectRow = (row) => {
  const index = selectedRows.value.findIndex(r => r === row)
  if (index > -1) {
    selectedRows.value.splice(index, 1)
  } else {
    selectedRows.value.push(row)
  }
  emit('selection-change', selectedRows.value)
}

// 检查行是否被选中
const isRowSelected = (row) => {
  return selectedRows.value.includes(row)
}

// 排序（两态切换：同列 asc ↔ desc，切换到新列默认 desc）
const handleSort = (column) => {
  if (!column.sortable) return

  if (sortColumn.value === column.key) {
    // 同一列：asc → desc → asc 循环
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    // 切换到新列，默认降序（通常"更高的放前面"符合直觉）
    sortColumn.value = column.key
    sortOrder.value = 'desc'
  }

  emit('sort-change', { column: sortColumn.value, order: sortOrder.value })
}

// 分页
const handlePageChange = (page) => {
  emit('page-change', page)
}

// 获取排序图标，始终显示当前方向（无"未排序"状态）
const getSortIcon = (column) => {
  if (!column.sortable) return null
  if (sortColumn.value !== column.key) return ChevronsUpDown
  return sortOrder.value === 'asc' ? ChevronUp : ChevronDown
}

// 总页数
const totalPages = computed(() => {
  return Math.ceil(props.pagination.total / props.pagination.pageSize)
})

// 计算显示的页码，加入省略号逻辑
const pageNumbers = computed(() => {
  const current = props.pagination.current
  const total = totalPages.value
  const maxPages = 7 // 最多显示多少个按钮

  if (total <= maxPages) {
    return Array.from({ length: total }, (_, i) => i + 1)
  }

  const left = Math.max(2, current - 2)
  const right = Math.min(total - 1, current + 2)

  const pages = [1]

  if (left > 2) {
    pages.push('...')
  }

  for (let i = left; i <= right; i++) {
    pages.push(i)
  }

  if (right < total - 1) {
    pages.push('...')
  }

  pages.push(total)
  return pages
})

const jumpPage = ref('')

const handleJump = () => {
  const page = parseInt(jumpPage.value)
  if (!isNaN(page) && page >= 1 && page <= totalPages.value) {
    handlePageChange(page)
    jumpPage.value = ''
  }
}
</script>

<template>
  <div class="admin-table-wrapper">
    <!-- 表格容器 -->
    <div class="overflow-x-auto rounded-lg border border-gray-200 bg-white">
      <table class="w-full">
        <!-- 表头 -->
        <thead class="bg-gray-50 border-b border-gray-200">
          <tr>
            <!-- 多选列 -->
            <th v-if="selectable" class="w-12 px-4 py-3">
              <input type="checkbox" :checked="isAllSelected" :indeterminate="isIndeterminate" @change="toggleSelectAll"
                class="w-4 h-4 text-primary border-gray-300 rounded focus:ring-primary cursor-pointer" />
            </th>

            <!-- 数据列 -->
            <th v-for="column in columns" :key="column.key" :class="[
              'px-4 py-3 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider',
              column.sortable && 'cursor-pointer hover:bg-gray-100 transition-colors',
              column.width && `w-${column.width}`
            ]" @click="handleSort(column)">
              <div class="flex items-center gap-2">
                <span>{{ column.label }}</span>
                <component v-if="column.sortable" :is="getSortIcon(column)" class="w-4 h-4"
                  :class="sortColumn === column.key ? 'text-primary' : 'text-gray-400'" />
              </div>
            </th>
          </tr>
        </thead>

        <!-- 表体 -->
        <tbody class="divide-y divide-gray-200">
          <!-- 加载状态 -->
          <tr v-if="loading">
            <td :colspan="columns.length + (selectable ? 1 : 0)" class="px-4 py-12 text-center">
              <div class="flex flex-col items-center gap-3">
                <div class="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
                <p class="text-sm text-gray-500">加载中...</p>
              </div>
            </td>
          </tr>

          <!-- 空状态 -->
          <tr v-else-if="data.length === 0">
            <td :colspan="columns.length + (selectable ? 1 : 0)" class="px-4 py-12 text-center">
              <div class="flex flex-col items-center gap-3">
                <svg class="w-16 h-16 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
                </svg>
                <p class="text-sm text-gray-500">暂无数据</p>
              </div>
            </td>
          </tr>

          <!-- 数据行 -->
          <tr v-else v-for="(row, index) in data" :key="index" class="hover:bg-gray-50 transition-colors"
            :class="isRowSelected(row) && 'bg-primary/5'">
            <!-- 多选列 -->
            <td v-if="selectable" class="px-4 py-3">
              <input type="checkbox" :checked="isRowSelected(row)" @change="toggleSelectRow(row)"
                class="w-4 h-4 text-primary border-gray-300 rounded focus:ring-primary cursor-pointer" />
            </td>

            <!-- 数据列 -->
            <td v-for="column in columns" :key="column.key" class="px-4 py-3 text-sm text-gray-700 max-w-xs">
              <!-- 自定义插槽 -->
              <slot v-if="column.slot" :name="column.slot" :row="row" :value="row[column.key]" />
              <!-- 默认显示 -->
              <span v-else class="break-words">{{ row[column.key] }}</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分页 -->
    <div v-if="pagination && pagination.total > 0" class="flex flex-col sm:flex-row items-center justify-between mt-4 px-2 gap-4">
      <div class="text-sm text-gray-600 shrink-0">
        共 {{ pagination.total }} 条，每页 {{ pagination.pageSize }} 条
      </div>

      <div class="flex flex-wrap shadow-sm rounded-md items-center justify-center gap-1">
        <button @click="handlePageChange(pagination.current - 1)" :disabled="pagination.current === 1"
          class="px-3 py-1.5 text-sm border border-gray-300 bg-white rounded-l-md hover:bg-gray-50 focus:z-10 disabled:opacity-50 disabled:cursor-not-allowed transition-colors font-medium">
          上一页
        </button>

        <template v-for="(page, index) in pageNumbers">
          <span :key="`ellipsis-${index}`" v-if="page === '...'" class="px-3 py-1.5 text-sm border-y border-gray-300 bg-white text-gray-400 select-none">
            ...
          </span>
          <button :key="`page-${page}`" v-else @click="handlePageChange(page)" :class="[
            'px-3.5 py-1.5 text-sm font-medium border border-gray-300 focus:z-10 transition-colors',
            page === pagination.current
              ? 'bg-primary text-white border-primary z-10'
              : 'bg-white text-gray-700 hover:bg-gray-50'
          ]">
            {{ page }}
          </button>
        </template>

        <button @click="handlePageChange(pagination.current + 1)"
          :disabled="pagination.current === totalPages || totalPages === 0"
          class="px-3 py-1.5 text-sm border border-gray-300 bg-white rounded-r-md hover:bg-gray-50 focus:z-10 disabled:opacity-50 disabled:cursor-not-allowed transition-colors font-medium">
          下一页
        </button>
      </div>

      <div class="flex items-center gap-2 text-sm shrink-0">
        <span class="text-gray-500">前往</span>
        <input 
          type="number" 
          v-model="jumpPage" 
          @keyup.enter="handleJump"
          min="1" 
          :max="totalPages"
          class="w-14 h-8 px-2 border border-gray-300 rounded focus:ring-1 focus:ring-primary focus:border-primary outline-none text-center" 
        />
        <span class="text-gray-500">页</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-table-wrapper {
  @apply w-full;
}

/* 半选状态样式 */
input[type="checkbox"]:indeterminate {
  background-image: url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 16 16'%3e%3cpath stroke='white' stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M4 8h8'/%3e%3c/svg%3e");
  background-color: currentColor;
  border-color: transparent;
}
</style>
