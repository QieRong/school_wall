<template>
  <div class="skeleton-loader" :class="variant">
    <!-- 帖子骨架屏 -->
    <template v-if="type === 'post'">
      <div class="skeleton-post" v-for="i in count" :key="i">
        <div class="skeleton-header">
          <div class="skeleton-avatar"></div>
          <div class="skeleton-info">
            <div class="skeleton-line skeleton-name"></div>
            <div class="skeleton-line skeleton-time"></div>
          </div>
        </div>
        <div class="skeleton-content">
          <div class="skeleton-line skeleton-text"></div>
          <div class="skeleton-line skeleton-text"></div>
          <div class="skeleton-line skeleton-text-short"></div>
        </div>
        <div class="skeleton-images">
          <div class="skeleton-image" v-for="j in 3" :key="j"></div>
        </div>
      </div>
    </template>

    <!-- 列表项骨架屏 -->
    <template v-else-if="type === 'list'">
      <div class="skeleton-list-item" v-for="i in count" :key="i">
        <div class="skeleton-avatar-small"></div>
        <div class="skeleton-list-content">
          <div class="skeleton-line skeleton-title"></div>
          <div class="skeleton-line skeleton-subtitle"></div>
        </div>
      </div>
    </template>

    <!-- 卡片骨架屏 -->
    <template v-else-if="type === 'card'">
      <div class="skeleton-card" v-for="i in count" :key="i">
        <div class="skeleton-card-image"></div>
        <div class="skeleton-card-body">
          <div class="skeleton-line skeleton-title"></div>
          <div class="skeleton-line skeleton-text"></div>
          <div class="skeleton-line skeleton-text-short"></div>
        </div>
      </div>
    </template>

    <!-- 文本骨架屏 -->
    <template v-else>
      <div class="skeleton-text-block" v-for="i in count" :key="i">
        <div class="skeleton-line skeleton-text"></div>
        <div class="skeleton-line skeleton-text"></div>
        <div class="skeleton-line skeleton-text-short"></div>
      </div>
    </template>
  </div>
</template>

<script setup>
defineProps({
  type: {
    type: String,
    default: 'post', // post, list, card, text
    validator: (value) => ['post', 'list', 'card', 'text'].includes(value)
  },
  count: {
    type: Number,
    default: 3
  },
  variant: {
    type: String,
    default: 'default', // default, compact
    validator: (value) => ['default', 'compact'].includes(value)
  }
})
</script>

<style scoped>
.skeleton-loader {
  width: 100%;
}

/* 基础骨架元素 */
.skeleton-line,
.skeleton-avatar,
.skeleton-avatar-small,
.skeleton-image,
.skeleton-card-image {
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

/* 帖子骨架屏 */
.skeleton-post {
  background: white;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.skeleton-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.skeleton-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  flex-shrink: 0;
}

.skeleton-info {
  flex: 1;
}

.skeleton-name {
  width: 120px;
  height: 16px;
  margin-bottom: 8px;
}

.skeleton-time {
  width: 80px;
  height: 12px;
}

.skeleton-content {
  margin-bottom: 16px;
}

.skeleton-text {
  height: 14px;
  margin-bottom: 8px;
  width: 100%;
}

.skeleton-text-short {
  height: 14px;
  width: 60%;
}

.skeleton-images {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.skeleton-image {
  aspect-ratio: 1;
  border-radius: 8px;
}

/* 列表项骨架屏 */
.skeleton-list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: white;
  border-radius: 12px;
  margin-bottom: 8px;
}

.skeleton-avatar-small {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  flex-shrink: 0;
}

.skeleton-list-content {
  flex: 1;
}

.skeleton-title {
  width: 150px;
  height: 14px;
  margin-bottom: 6px;
}

.skeleton-subtitle {
  width: 100px;
  height: 12px;
}

/* 卡片骨架屏 */
.skeleton-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 16px;
}

.skeleton-card-image {
  width: 100%;
  height: 180px;
}

.skeleton-card-body {
  padding: 16px;
}

/* 文本块骨架屏 */
.skeleton-text-block {
  background: white;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
}

/* 紧凑变体 */
.skeleton-loader.compact .skeleton-post {
  padding: 12px;
  margin-bottom: 8px;
}

.skeleton-loader.compact .skeleton-list-item {
  padding: 8px;
}
</style>
