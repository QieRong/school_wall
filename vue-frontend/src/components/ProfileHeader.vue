<script setup>
import { ref } from 'vue'
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { MapPin, Calendar, Users, Heart, FileText, Settings } from 'lucide-vue-next'

const props = defineProps({
  user: {
    type: Object,
    required: true
  },
  isOwnProfile: {
    type: Boolean,
    default: false
  },
  isFollowing: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['follow', 'unfollow', 'message', 'edit'])

// 格式化日期
const formatDate = (date) => {
  return new Date(date).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long'
  })
}
</script>

<template>
  <Card variant="elevated" padding="none" class="overflow-hidden">
    <!-- 封面图 -->
    <div class="relative h-48 bg-gradient-to-r from-primary-400 to-primary-600">
      <img v-if="user.coverImage" :src="user.coverImage" alt="封面" class="w-full h-full object-cover" />

      <!-- 编辑按钮 - 仅自己可见 -->
      <Button v-if="isOwnProfile" variant="secondary" size="sm" class="absolute top-4 right-4" @click="emit('edit')">
        <Settings class="w-4 h-4 mr-2" />
        编辑资料
      </Button>
    </div>

    <!-- 用户信息 -->
    <div class="relative px-6 pb-6">
      <!-- 头像 -->
      <div class="relative -mt-16 mb-4">
        <Avatar class="w-32 h-32 ring-4 ring-white shadow-lg">
          <AvatarImage :src="user.avatar" :alt="user.nickname" />
          <AvatarFallback class="text-3xl">{{ user.nickname?.[0] || 'U' }}</AvatarFallback>
        </Avatar>
      </div>

      <!-- 用户名和简介 -->
      <div class="mb-4">
        <h1 class="text-2xl font-bold text-gray-900 mb-2">{{ user.nickname }}</h1>
        <p v-if="user.bio" class="text-gray-600 leading-relaxed">{{ user.bio }}</p>
        <p v-else class="text-gray-400 italic">这个人很懒，什么都没留下</p>
      </div>

      <!-- 用户元信息 -->
      <div class="flex flex-wrap gap-4 text-sm text-gray-600 mb-4">
        <div v-if="user.location" class="flex items-center gap-1">
          <MapPin class="w-4 h-4" />
          <span>{{ user.location }}</span>
        </div>
        <div v-if="user.createTime" class="flex items-center gap-1">
          <Calendar class="w-4 h-4" />
          <span>{{ formatDate(user.createTime) }} 加入</span>
        </div>
      </div>

      <!-- 统计数据 -->
      <div class="flex gap-6 mb-4">
        <div class="text-center">
          <div class="text-2xl font-bold text-gray-900">{{ user.postCount || 0 }}</div>
          <div class="text-sm text-gray-500">帖子</div>
        </div>
        <div class="text-center cursor-pointer hover:text-primary-600 transition-colors">
          <div class="text-2xl font-bold text-gray-900">{{ user.followingCount || 0 }}</div>
          <div class="text-sm text-gray-500">关注</div>
        </div>
        <div class="text-center cursor-pointer hover:text-primary-600 transition-colors">
          <div class="text-2xl font-bold text-gray-900">{{ user.followerCount || 0 }}</div>
          <div class="text-sm text-gray-500">粉丝</div>
        </div>
        <div class="text-center">
          <div class="text-2xl font-bold text-gray-900">{{ user.likeCount || 0 }}</div>
          <div class="text-sm text-gray-500">获赞</div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div v-if="!isOwnProfile" class="flex gap-3">
        <Button v-if="isFollowing" variant="outline" size="lg" class="flex-1" @click="emit('unfollow')">
          <Users class="w-4 h-4 mr-2" />
          已关注
        </Button>
        <Button v-else variant="primary" size="lg" class="flex-1" @click="emit('follow')">
          <Users class="w-4 h-4 mr-2" />
          关注
        </Button>

        <Button variant="outline" size="lg" class="flex-1" @click="emit('message')">
          <FileText class="w-4 h-4 mr-2" />
          私信
        </Button>
      </div>
    </div>
  </Card>
</template>
