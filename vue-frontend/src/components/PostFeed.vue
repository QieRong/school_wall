<script setup>
import { ref } from 'vue'
import PostCard from './PostCard.vue'
import SkeletonLoader from './SkeletonLoader.vue'
import ListEndIndicator from './ListEndIndicator.vue'

const props = defineProps({
  posts: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  hasMore: {
    type: Boolean,
    default: true
  },
  currentUser: {
    type: Object,
    default: null
  }
})

const emit = defineEmits([
  'like',
  'comment',
  'share',
  'report',
  'delete',
  'viewLocation',
  'loadMore'
])

// 转发所有事件
const handleLike = (postId) => emit('like', postId)
const handleComment = (postId) => emit('comment', postId)
const handleShare = (postId) => emit('share', postId)
const handleReport = (postId) => emit('report', postId)
const handleDelete = (postId) => emit('delete', postId)
const handleViewLocation = (post) => emit('viewLocation', post)
</script>

<template>
  <div class="w-full max-w-3xl mx-auto space-y-4">
    <!-- 帖子列表 -->
    <TransitionGroup name="list" tag="div" class="space-y-4">
      <PostCard v-for="post in posts" :key="post.id" :post="post" :current-user="currentUser" @like="handleLike"
        @comment="handleComment" @share="handleShare" @report="handleReport" @delete="handleDelete"
        @view-location="handleViewLocation" />
    </TransitionGroup>

    <!-- 加载中骨架屏 -->
    <div v-if="loading" class="space-y-4">
      <SkeletonLoader v-for="i in 3" :key="`skeleton-${i}`" />
    </div>

    <!-- 列表结束指示器 -->
    <ListEndIndicator v-if="!loading && !hasMore && posts.length > 0" />

    <!-- 空状态 -->
    <div v-if="!loading && posts.length === 0" class="text-center py-12">
      <div class="text-gray-400 text-lg mb-2">暂无内容</div>
      <div class="text-gray-500 text-sm">换个筛选条件试试吧</div>
    </div>
  </div>
</template>

<style scoped>
/* 列表进入动画 */
.list-enter-active {
  transition: all 0.3s ease-out;
}

.list-leave-active {
  transition: all 0.2s ease-in;
}

.list-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.list-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

.list-move {
  transition: transform 0.3s ease;
}
</style>
