<script setup>
import { ref, onMounted, computed, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Button } from '@/components/ui/button'
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Cropper } from 'vue-advanced-cropper'
import 'vue-advanced-cropper/dist/style.css'
import {
  MapPin, Edit, Camera, Eye, ThumbsUp, MessageCircle,
  Trash2, Mail, UserPlus, ChevronLeft, Clock, BarChart2, Star, ShieldAlert, Check, Loader2, Ban
} from 'lucide-vue-next'
import { getUserProfile, updateUserProfile, getVisitors, getUserPosts } from '@/api/user'
import { followUser, unfollowUser, checkFollowStatus, getMyCollections } from '@/api/social'
import request from '@/api/request'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { getAvatarUrl, getCoverUrl } from '@/utils/imageUtils'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

const loading = ref(false)
const userInfo = ref({})
const visitors = ref([])
const showAllVisitors = ref(false) // 控制访客列表展开/收起
const displayedVisitors = computed(() => {
  if (showAllVisitors.value) return visitors.value
  return visitors.value.slice(0, 4) // 默认显示4个（一行）
})
const posts = ref([])
const activeTab = ref('posts')
const isSelf = ref(false)
const showEditDialog = ref(false)
const isFollowed = ref(false)

const editForm = reactive({ nickname: '', avatar: '', coverImage: '' })
const tempAvatar = ref('') // 本地头像预览URL

// 头像裁剪相关
const showAvatarCropper = ref(false)
const avatarCropperSrc = ref('')
const avatarCropperRef = ref(null)
const avatarUploading = ref(false)
const avatarFileInput = ref(null)

// 封面裁剪相关
const showCoverCropper = ref(false)
const coverCropperSrc = ref('')
const coverCropperRef = ref(null)
const coverUploading = ref(false)

const currentUser = computed(() => userStore.user)
const isCurrentUserAdmin = computed(() => currentUser.value?.role === 1)
const isTargetUserAdmin = computed(() => userInfo.value?.role === 1)

// 计算属性：处理默认昵称显示 (张院+账号)
const displayName = computed(() => {
  if (userInfo.value.nickname) return userInfo.value.nickname
  if (userInfo.value.account) {
    // 取账号后4位
    const suffix = userInfo.value.account.slice(-4)
    return `张院${suffix}`
  }
  return '神秘同学'
})

// 计算属性：标准化头像 URL（修复部分页面头像不显示问题）
const avatarUrl = computed(() => getAvatarUrl(userInfo.value.avatar))

// 计算属性：标准化封面 URL
const coverUrl = computed(() => getCoverUrl(userInfo.value.coverImage))

const loadData = async () => {
  const targetId = route.params.id
  if (!targetId) return

  const currentId = currentUser.value?.id
  isSelf.value = (String(targetId) === String(currentId))

  loading.value = true
  try {
    const res = await getUserProfile(targetId, currentId)
    if (res.code === '200' && res.data) {
      userInfo.value = res.data

      if (!isSelf.value && currentId) {
        const fRes = await checkFollowStatus(currentId, targetId)
        isFollowed.value = fRes.data
      }
      if (isSelf.value) {
        // 用 updateInfo 保留 token，避免覆盖
        userStore.updateInfo(res.data)
      }
    } else {
      appStore.showToast(res.msg || '用户不存在', 'error')
      router.replace('/home')
      return
    }

    if (!isTargetUserAdmin.value) {
      const vRes = await getVisitors(targetId)
      if (vRes.code === '200') visitors.value = vRes.data || []
    }

    loadList()

  } catch (e) {
    console.error('加载用户信息失败:', e)
    appStore.showToast('用户不存在或加载失败', 'error')
    router.replace('/home')
  } finally {
    loading.value = false
  }
}

const loadList = async () => {
  posts.value = []
  try {
    const targetId = route.params.id
    const currentId = currentUser.value?.id || 0
    let res;

    if (activeTab.value === 'posts') {
      res = await getUserPosts(targetId, currentId)
    } else if (activeTab.value === 'scheduled') {
      res = await request.get('/post/scheduled', { params: { userId: targetId } })
    } else if (activeTab.value === 'polls') {
      res = await request.get('/post/polls', { params: { userId: targetId } })
    }

    if (res && res.code === '200') {
      posts.value = res.data.list || res.data
      posts.value.forEach(p => {
        //兼容JSON和逗号分隔两种格式
        if (p.images) {
          try {
            p.imgList = p.images.startsWith('[') ? JSON.parse(p.images) : p.images.split(',')
          } catch (e) {
            p.imgList = p.images.split(',')
          }
        } else {
          p.imgList = []
        }
      })
    }
  } catch (e) { }
}

watch(activeTab, loadList)
watch(() => route.params.id, (newId) => { if (newId) loadData() })

//监听弹窗打开，初始化表单数据
watch(showEditDialog, (val) => {
  if (val) {
    editForm.nickname = userInfo.value.nickname
    editForm.avatar = userInfo.value.avatar
    editForm.coverImage = userInfo.value.coverImage
    tempAvatar.value = '' // 重置临时预览
  }
})

const handleUpload = async (event, type) => {
  const file = event.target.files[0]
  if (!file) return

  // 限制文件大小：头像5MB，封面10MB
  const maxSize = type === 'avatar' ? 5 * 1024 * 1024 : 10 * 1024 * 1024
  const limitLabel = type === 'avatar' ? '5MB' : '10MB'
  const typeLabel = type === 'avatar' ? '头像' : '封面'
  if (file.size > maxSize) {
    const sizeMB = (file.size / 1024 / 1024).toFixed(2)
    appStore.showToast(`图片过大 (${sizeMB}MB)！${typeLabel}不能超过 ${limitLabel}，请压缩后重试`, 'error')
    event.target.value = ''
    return
  }

  if (type === 'avatar') {
    // 头像进入裁剪流程
    const reader = new FileReader()
    reader.onload = (ev) => {
      avatarCropperSrc.value = ev.target.result
      showAvatarCropper.value = true
    }
    reader.readAsDataURL(file)
    event.target.value = ''
    return
  }

  if (type === 'cover') {
    // 封面进入裁剪流程
    const reader = new FileReader()
    reader.onload = (ev) => {
      coverCropperSrc.value = ev.target.result
      showCoverCropper.value = true
    }
    reader.readAsDataURL(file)
    event.target.value = ''
    return
  }
}

// 确认封面裁剪并保存
const confirmCoverCrop = async () => {
  if (!coverCropperRef.value) return
  coverUploading.value = true
  try {
    const { canvas } = coverCropperRef.value.getResult()
    if (!canvas) { appStore.showToast('裁剪失败', 'error'); return }

    const finalCanvas = document.createElement('canvas')
    finalCanvas.width = canvas.width
    finalCanvas.height = canvas.height
    const ctx = finalCanvas.getContext('2d')
    ctx.fillStyle = '#ffffff'
    ctx.fillRect(0, 0, finalCanvas.width, finalCanvas.height)
    ctx.drawImage(canvas, 0, 0)

    const blob = await new Promise(resolve => finalCanvas.toBlob(resolve, 'image/jpeg', 0.9))
    showCoverCropper.value = false
    coverCropperSrc.value = ''

    const formData = new FormData()
    formData.append('file', blob, 'cover.jpg')
    formData.append('userId', currentUser.value.id)

    const res = await request.post('/file/upload', formData)
    if (res.code === '200') {
      const updateData = {
        id: currentUser.value.id,
        nickname: userInfo.value.nickname || currentUser.value.nickname,
        avatar: userInfo.value.avatar || currentUser.value.avatar,
        coverImage: res.data
      }
      const updateRes = await updateUserProfile(updateData)
      if (updateRes.code === '200') {
        userInfo.value.coverImage = res.data
        userStore.login({ ...currentUser.value, coverImage: res.data })
        appStore.showToast('封面更新成功 ✅', 'success')
      } else {
        appStore.showToast(updateRes.msg || '封面更新失败', 'error')
      }
    } else {
      appStore.showToast(res.msg || '上传失败', 'error')
    }
  } catch (e) {
    appStore.showToast('上传失败，请重试', 'error')
  } finally {
    coverUploading.value = false
  }
}

// 确认头像裁剪并上传
const confirmAvatarCrop = async () => {
  if (!avatarCropperRef.value) return
  avatarUploading.value = true
  try {
    const { canvas } = avatarCropperRef.value.getResult()
    if (!canvas) { appStore.showToast('裁剪失败', 'error'); return }

    // 填充白色背景，避免透明区域转 JPEG 变灰
    const finalCanvas = document.createElement('canvas')
    finalCanvas.width = canvas.width
    finalCanvas.height = canvas.height
    const ctx = finalCanvas.getContext('2d')
    ctx.fillStyle = '#ffffff'
    ctx.fillRect(0, 0, finalCanvas.width, finalCanvas.height)
    ctx.drawImage(canvas, 0, 0)

    const blob = await new Promise(resolve => finalCanvas.toBlob(resolve, 'image/jpeg', 0.9))

    // 立即显示本地预览
    tempAvatar.value = URL.createObjectURL(blob)
    showAvatarCropper.value = false
    avatarCropperSrc.value = ''

    // 上传到服务器
    const formData = new FormData()
    formData.append('file', blob, 'avatar.jpg')
    formData.append('userId', currentUser.value.id)

    const res = await request.post('/file/upload', formData)
    if (res.code === '200') {
      editForm.avatar = res.data
      appStore.showToast('头像上传成功 ✅', 'success')
    } else {
      tempAvatar.value = ''
      appStore.showToast(res.msg || '上传失败', 'error')
    }
  } catch (e) {
    tempAvatar.value = ''
    appStore.showToast('上传失败，请重试', 'error')
  } finally {
    avatarUploading.value = false
  }
}

//保存loading状态
const saving = ref(false)

const saveProfile = async (silent = false) => {
  if (saving.value) return
  saving.value = true

  try {
    const data = {
      id: currentUser.value.id,
      nickname: editForm.nickname,
      avatar: editForm.avatar,
      coverImage: editForm.coverImage
    }
    const res = await updateUserProfile(data)
    if (res.code === '200') {
      if (!silent) {
        appStore.showToast('✅ 保存成功', 'success')
        showEditDialog.value = false
      }
      const newUser = { ...currentUser.value, ...data }
      userStore.login(newUser)
      loadData()
    } else {
      appStore.showToast(res.msg, 'error')
    }
  } catch (e) {
    appStore.showToast('保存失败，请重试', 'error')
  } finally {
    saving.value = false
  }
}

//删除帖子确认弹窗
const showDeletePostDialog = ref(false)
const deletePostTarget = ref(null)
const deletingPost = ref(false)

const openDeletePostDialog = (postId) => {
  deletePostTarget.value = postId
  showDeletePostDialog.value = true
}

const confirmDeletePost = async () => {
  if (!deletePostTarget.value || deletingPost.value) return

  deletingPost.value = true
  try {
    const res = await request.delete(`/post/${deletePostTarget.value}`, { params: { userId: currentUser.value.id } })
    if (res.code === '200') {
      posts.value = posts.value.filter(p => p.id !== deletePostTarget.value)
      appStore.showToast('✅ 删除成功', 'success')
      showDeletePostDialog.value = false
      deletePostTarget.value = null
    } else {
      appStore.showToast(res.msg || '删除失败', 'error')
    }
  } catch (e) {
    console.error('删除帖子失败:', e)
    appStore.showToast('删除失败，请重试', 'error')
  } finally {
    deletingPost.value = false
  }
}

//私信按钮 - 直接跳转到消息页面并自动创建会话
const handleChat = async () => {
  if (!currentUser.value) {
    appStore.showToast('请先登录', 'error')
    return
  }
  // 跳转到消息页面，带上目标用户ID
  router.push({ path: '/message', query: { targetId: userInfo.value.id, targetName: userInfo.value.nickname || displayName.value } })
}

// 取消关注确认弹窗
const showUnfollowConfirm = ref(false)

//关注按钮 - 添加错误处理和取消关注确认
const followLoading = ref(false)
const handleFollow = async () => {
  if (!currentUser.value) {
    appStore.showToast('请先登录', 'error')
    return
  }
  if (followLoading.value) return

  // 取消关注时需要二次确认
  if (isFollowed.value) {
    showUnfollowConfirm.value = true
    return
  }

  await doFollow()
}

// 确认取消关注
const confirmUnfollow = async () => {
  showUnfollowConfirm.value = false
  await doFollow()
}

// 执行关注/取关操作
const doFollow = async () => {
  followLoading.value = true
  try {
    if (isFollowed.value) {
      const res = await unfollowUser({ userId: currentUser.value.id, targetId: userInfo.value.id })
      if (res.code === '200') {
        isFollowed.value = false
        appStore.showToast('已取消关注', 'success')
        // 重新获取用户信息,确保统计数据准确
        await loadData()
      } else {
        appStore.showToast(res.msg || '操作失败', 'error')
      }
    } else {
      const res = await followUser({ userId: currentUser.value.id, targetId: userInfo.value.id })
      if (res.code === '200') {
        isFollowed.value = true
        appStore.showToast('关注成功', 'success')
        // 重新获取用户信息,确保统计数据准确
        await loadData()
      } else {
        appStore.showToast(res.msg || '操作失败', 'error')
      }
    }
  } catch (e) {
    console.error('关注操作失败:', e)
    appStore.showToast('网络错误，请重试', 'error')
  } finally {
    followLoading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="min-h-screen bg-[#f0f2f5] pb-20 relative">

    <Button variant="secondary" size="icon"
      class="fixed top-4 left-4 z-50 rounded-full bg-white/80 backdrop-blur shadow-lg hover:bg-white"
      @click="router.back()">
      <ChevronLeft class="w-6 h-6 text-gray-700" />
    </Button>

    <div class="relative h-[320px] w-full group overflow-hidden">
      <img :src="coverUrl" loading="lazy"
        class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-105" />
      <div class="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent"></div>

      <label v-if="isSelf && !isCurrentUserAdmin"
        class="absolute top-6 right-6 cursor-pointer opacity-0 group-hover:opacity-100 transition-opacity duration-300">
        <div
          class="bg-black/40 hover:bg-[rgb(33,111,85)] text-white px-4 py-2 rounded-full backdrop-blur-md text-sm flex items-center gap-2 border border-white/20">
          <Camera class="w-4 h-4" /> 裁剪封面
        </div>
        <input type="file" hidden accept="image/*" @change="handleUpload($event, 'cover')" />
      </label>

      <div class="absolute bottom-0 left-0 w-full container mx-auto px-4 pb-8 flex items-end gap-6 max-w-7xl">
        <div class="relative -mb-4">
          <Avatar class="w-32 h-32 border-4 border-white shadow-2xl bg-white">
            <AvatarImage :src="avatarUrl" class="object-cover" />
            <AvatarFallback class="text-4xl bg-gray-200">{{ userInfo.nickname?.[0] }}</AvatarFallback>
          </Avatar>
          <div v-if="userInfo.status === 0"
            class="absolute bottom-0 right-0 bg-red-600 text-white text-xs px-2 py-0.5 rounded-full shadow-sm">已封禁</div>
        </div>

        <div class="flex-1 text-white mb-1">
          <div class="flex items-center gap-3">
            <h1 class="text-3xl font-bold drop-shadow-md tracking-wide">{{ displayName }}</h1>

            <Badge v-if="userInfo.role === 1" class="bg-yellow-500 border-none text-black font-bold flex gap-1">
              <ShieldAlert class="w-3 h-3" /> 管理员
            </Badge>

          </div>
          <p class="text-white/80 text-sm mt-2 flex items-center gap-4 font-light">
            <span class="opacity-80">ID: {{ userInfo.account }}</span>
            <span class="flex items-center gap-1 opacity-80">
              <MapPin class="w-3 h-3" /> 张家界学院
            </span>
          </p>
        </div>

        <div class="flex gap-3 mb-2">
          <template v-if="isSelf && !isCurrentUserAdmin">
            <Button class="bg-white/10 hover:bg-white/20 text-white border border-white/30 backdrop-blur-md"
              @click="showEditDialog = true">
              <Edit class="w-4 h-4 mr-2" /> 编辑资料
            </Button>
            <Button class="bg-white/10 hover:bg-white/20 text-white border border-white/30 backdrop-blur-md"
              @click="router.push('/settings/blacklist')">
              <Ban class="w-4 h-4 mr-2" /> 黑名单
            </Button>
          </template>
          <template v-else-if="!isSelf && !isTargetUserAdmin && !isCurrentUserAdmin">
            <Button :class="isFollowed ? 'bg-white/20' : 'bg-[rgb(33,111,85)]'"
              class="text-white border-none shadow-lg disabled:opacity-50 disabled:cursor-not-allowed transition-all"
              :disabled="followLoading" @click="handleFollow">
              <Loader2 v-if="followLoading" class="w-4 h-4 mr-2 animate-spin" />
              <component v-else :is="isFollowed ? Check : UserPlus" class="w-4 h-4 mr-2" />
              {{ followLoading ? '处理中' : (isFollowed ? '已关注' : '关注') }}
            </Button>
            <Button class="bg-white/10 hover:bg-white/20 text-white border border-white/30 backdrop-blur-md"
              @click="handleChat">
              <Mail class="w-4 h-4 mr-2" /> 私信
            </Button>
          </template>
        </div>
      </div>
    </div>

    <div class="container mx-auto px-4 mt-8 grid grid-cols-1 md:grid-cols-12 gap-6 max-w-7xl">

      <div class="md:col-span-3 space-y-6" v-if="!isTargetUserAdmin">
        <Card class="border-none shadow-sm overflow-hidden">
          <CardHeader class="pb-2 bg-gray-50/50 border-b border-gray-100">
            <CardTitle class="text-base flex items-center gap-2 text-gray-800">📊 数据中心</CardTitle>
          </CardHeader>
          <CardContent class="pt-4">
            <div class="grid grid-cols-2 gap-3 text-center">
              <!-- 关注/粉丝入口 -->
              <div
                class="p-3 bg-emerald-50 rounded-xl border border-emerald-100 cursor-pointer hover:bg-emerald-100 transition"
                @click="router.push(`/follow/followings/${userInfo.id}`)">
                <div class="text-xl font-bold text-[rgb(33,111,85)]">{{ userInfo.followingCount || 0 }}</div>
                <div class="text-xs text-gray-500 mt-1">关注</div>
              </div>
              <div
                class="p-3 bg-purple-50 rounded-xl border border-purple-100 cursor-pointer hover:bg-purple-100 transition"
                @click="router.push(`/follow/followers/${userInfo.id}`)">
                <div class="text-xl font-bold text-purple-600">{{ userInfo.followerCount || 0 }}</div>
                <div class="text-xs text-gray-500 mt-1">粉丝</div>
              </div>
              <div class="p-3 bg-pink-50 rounded-xl border border-pink-100">
                <div class="text-xl font-bold text-pink-600">{{ userInfo.totalLikes || 0 }}</div>
                <div class="text-xs text-gray-500 mt-1">获赞总数</div>
              </div>
              <div class="p-3 bg-blue-50 rounded-xl border border-blue-100">
                <div class="text-xl font-bold text-blue-600">{{ userInfo.totalVisits || 0 }}</div>
                <div class="text-xs text-gray-500 mt-1">主页访客</div>
              </div>
              <div
                class="col-span-2 p-3 bg-gray-50 rounded-xl border border-gray-100 flex items-center justify-between px-6">
                <div class="text-left">
                  <div class="text-xl font-bold text-[rgb(33,111,85)]">{{ userInfo.creditScore || 100 }}</div>
                  <div class="text-xs text-gray-500 mt-1">当前信誉分</div>
                </div>
                <div class="h-12 w-12 rounded-full border-4 flex items-center justify-center font-bold bg-white text-lg"
                  :class="(userInfo.creditScore || 100) >= 80 ? 'border-[rgb(33,111,85)] text-[rgb(33,111,85)]' : (userInfo.creditScore || 100) >= 60 ? 'border-yellow-500 text-yellow-500' : 'border-red-500 text-red-500'">
                  {{ (userInfo.creditScore || 100) >= 80 ? '优' : (userInfo.creditScore || 100) >= 60 ? '良' : '差' }}
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card class="border-none shadow-sm">
          <CardHeader class="pb-2 flex flex-row items-center justify-between border-b border-gray-50">
            <CardTitle class="text-base text-gray-800">👣 谁看过我</CardTitle>
            <span v-if="visitors.length > 0" class="text-xs text-gray-400">共 {{ visitors.length }} 人</span>
          </CardHeader>
          <CardContent class="pt-4">
            <div v-if="visitors.length === 0" class="text-center text-sm text-gray-400 py-4">
              暂无访客记录
            </div>
            <template v-else>
              <div class="grid grid-cols-4 gap-4">
                <div v-for="v in displayedVisitors" :key="v.id" class="flex flex-col items-center cursor-pointer group"
                  @click="router.push(`/user/${v.visitorId}`)">
                  <Avatar class="w-10 h-10 border border-gray-100 group-hover:border-[rgb(33,111,85)] transition-colors">
                    <AvatarImage v-if="v.visitorAvatar" :src="v.visitorAvatar" />
                    <AvatarFallback class="text-xs bg-gray-100">{{ v.visitorName?.[0] }}</AvatarFallback>
                  </Avatar>
                  <span
                    class="text-[10px] text-gray-500 mt-1 truncate w-full text-center group-hover:text-[rgb(33,111,85)]">{{
                      v.visitorName }}</span>
                </div>
              </div>
              <!-- 展开/收起按钮 -->
              <div v-if="visitors.length > 4" class="text-center mt-4">
                <button @click="showAllVisitors = !showAllVisitors"
                  class="text-xs text-[rgb(33,111,85)] hover:underline">
                  {{ showAllVisitors ? '收起' : `展开更多 (${visitors.length - 4})` }}
                </button>
              </div>
            </template>
          </CardContent>
        </Card>
      </div>

      <div class="md:col-span-3" v-else>
        <Card class="border-l-4 border-yellow-500">
          <CardContent class="p-6 text-sm text-gray-600">
            该用户为系统管理员，负责维护社区秩序。
          </CardContent>
        </Card>
      </div>

      <div class="md:col-span-9">
        <div class="bg-white rounded-2xl shadow-sm min-h-[600px] border border-gray-100 overflow-hidden">
          <div class="flex border-b border-gray-100 overflow-x-auto">
            <div class="px-8 py-4 text-sm font-bold cursor-pointer whitespace-nowrap transition-colors"
              :class="activeTab === 'posts' ? 'text-[rgb(33,111,85)] border-b-2 border-[rgb(33,111,85)] bg-emerald-50/30' : 'text-gray-500 hover:text-gray-700'"
              @click="activeTab = 'posts'">{{ isSelf ? '我' : 'TA' }}的发布</div>

            <template v-if="isSelf && !isCurrentUserAdmin">
              <!-- 已移除收藏功能 -->
              <div
                class="px-8 py-4 text-sm font-bold cursor-pointer whitespace-nowrap transition-colors flex items-center gap-2"
                :class="activeTab === 'scheduled' ? 'text-[rgb(33,111,85)] border-b-2 border-[rgb(33,111,85)] bg-emerald-50/30' : 'text-gray-500 hover:text-gray-700'"
                @click="activeTab = 'scheduled'">
                <Clock class="w-4 h-4" /> 定时发送
              </div>
              <div
                class="px-8 py-4 text-sm font-bold cursor-pointer whitespace-nowrap transition-colors flex items-center gap-2"
                :class="activeTab === 'polls' ? 'text-[rgb(33,111,85)] border-b-2 border-[rgb(33,111,85)] bg-emerald-50/30' : 'text-gray-500 hover:text-gray-700'"
                @click="activeTab = 'polls'">
                <BarChart2 class="w-4 h-4" /> 我的投票
              </div>
            </template>
          </div>

          <div class="p-6 space-y-6">
            <!--空状态提示 - 更友好的UI和引导 -->
            <div v-if="posts.length === 0" class="flex flex-col items-center justify-center py-20 px-6">
              <div
                class="w-24 h-24 bg-gradient-to-br from-emerald-100 to-teal-100 rounded-full flex items-center justify-center shadow-lg mb-6">
                <span class="text-5xl">
                  {{ activeTab === 'posts' ? '📭' : activeTab === 'collections' ? '⭐' : activeTab === 'scheduled' ? '⏰'
                    :
                    '📊' }}
                </span>
              </div>
              <h3 class="text-lg font-bold text-gray-700 mb-2">
                {{ activeTab === 'posts' ? (isSelf ? '还没有发布内容' : 'TA还没有发布内容') :
                  activeTab === 'collections' ? '还没有收藏内容' :
                    activeTab === 'scheduled' ? '没有定时发送的内容' :
                      '还没有参与投票' }}
              </h3>
              <p class="text-gray-400 text-sm mb-6 text-center max-w-md">
                {{ activeTab === 'posts' ? (isSelf ? '分享你的校园生活，让更多人看到你的精彩' : '期待TA的第一条动态') :
                  activeTab === 'collections' ? '浏览精彩内容并点击收藏按钮' :
                    activeTab === 'scheduled' ? '创建定时发送的内容，在指定时间自动发布' :
                      '参与投票，表达你的观点' }}
              </p>
              <div v-if="isSelf" class="flex gap-3 flex-wrap justify-center">
                <Button v-if="activeTab === 'posts'"
                  class="bg-gradient-to-r from-[rgb(33,111,85)] to-emerald-600 hover:from-[rgb(28,95,72)] hover:to-emerald-700 text-white rounded-full px-6"
                  @click="router.push('/home')">
                  ✍️ 去发布内容
                </Button>
                <Button v-else-if="activeTab === 'collections'"
                  class="bg-gradient-to-r from-[rgb(33,111,85)] to-emerald-600 hover:from-[rgb(28,95,72)] hover:to-emerald-700 text-white rounded-full px-6"
                  @click="router.push('/home')">
                  🔍 浏览精彩内容
                </Button>
                <Button v-else-if="activeTab === 'scheduled'"
                  class="bg-gradient-to-r from-[rgb(33,111,85)] to-emerald-600 hover:from-[rgb(28,95,72)] hover:to-emerald-700 text-white rounded-full px-6"
                  @click="router.push('/home')">
                  📅 创建定时内容
                </Button>
                <Button v-else
                  class="bg-gradient-to-r from-[rgb(33,111,85)] to-emerald-600 hover:from-[rgb(28,95,72)] hover:to-emerald-700 text-white rounded-full px-6"
                  @click="router.push('/home')">
                  📊 去参与投票
                </Button>
              </div>
              <div v-else class="flex gap-3">
                <Button
                  class="bg-gradient-to-r from-[rgb(33,111,85)] to-emerald-600 hover:from-[rgb(28,95,72)] hover:to-emerald-700 text-white rounded-full px-6"
                  @click="router.push('/home')">
                  🏠 返回首页
                </Button>
              </div>
            </div>
            <div v-else v-for="post in posts" :key="post.id"
              class="bg-gray-50 rounded-xl p-4 flex gap-4 group hover:bg-gray-100 transition relative cursor-pointer"
              @click="router.push(`/post/${post.id}`)">
              <div class="flex flex-col items-center min-w-[50px] pt-1">
                <div class="text-2xl font-bold text-gray-700 leading-none">{{ new Date(post.scheduleTime || post.createTime).getDate() }}
                </div>
                <div class="text-xs text-gray-400 uppercase font-medium mt-1">{{ new
                  Date(post.scheduleTime || post.createTime).toLocaleString('default', { month: 'short' }) }}</div>
              </div>
              <div class="flex-1">
                <p class="text-gray-800 mb-2 text-sm whitespace-pre-wrap line-clamp-3">{{ post.content }}</p>
                <!-- 位置显示 -->
                <div v-if="post.location" class="flex items-center gap-1 text-xs text-gray-400 mb-2">
                  <MapPin class="w-3 h-3" /> {{ post.location }}
                </div>
                <!-- 媒体展示：图片+视频 -->
                <div v-if="(post.imgList && post.imgList.length) || post.video" class="flex gap-2 mb-2 flex-wrap">
                  <img v-for="(img, i) in (post.imgList || []).slice(0, 3)" :key="'img-' + i" :src="img" loading="lazy"
                    class="w-20 h-20 object-cover rounded-lg bg-gray-200"
                    @error="(e) => e.target.src = '/default.png'" />
                  <!-- 视频缩略图 -->
                  <div v-if="post.video" class="w-20 h-20 rounded-lg bg-gray-200 relative overflow-hidden">
                    <video :src="post.video" class="w-full h-full object-cover" preload="metadata"></video>
                    <div class="absolute inset-0 bg-black/30 flex items-center justify-center">
                      <div class="w-8 h-8 bg-white/90 rounded-full flex items-center justify-center">
                        <div
                          class="w-0 h-0 border-t-[6px] border-t-transparent border-l-[10px] border-l-gray-800 border-b-[6px] border-b-transparent ml-1">
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="flex items-center justify-between text-xs text-gray-400 mt-2">
                  <div class="flex gap-4">
                    <span class="flex items-center">
                      <Eye class="w-3 h-3 mr-1" /> {{ post.viewCount || 0 }}
                    </span>
                    <span class="flex items-center">
                      <ThumbsUp class="w-3 h-3 mr-1" /> {{ post.likeCount || 0 }}
                    </span>
                    <span class="flex items-center">
                      <MessageCircle class="w-3 h-3 mr-1" /> {{ post.commentCount || 0 }}
                    </span>
                  </div>
                  <Button v-if="isSelf" variant="ghost" size="sm"
                    class="h-6 text-red-400 hover:text-red-600 hover:bg-red-50 opacity-0 group-hover:opacity-100 transition-opacity"
                    @click.stop="openDeletePostDialog(post.id)">
                    <Trash2 class="w-3 h-3 mr-1" /> 删除
                  </Button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 取消关注确认弹窗 -->
    <Dialog :open="showUnfollowConfirm" @update:open="showUnfollowConfirm = $event">
      <DialogContent class="sm:max-w-[380px] bg-white rounded-xl">
        <DialogHeader>
          <DialogTitle class="text-lg font-bold text-gray-800">取消关注</DialogTitle>
        </DialogHeader>
        <div class="py-4 text-gray-600">
          <p>确定要取消关注 <span class="font-bold text-[rgb(33,111,85)]">{{ displayName }}</span> 吗？</p>
          <p class="text-sm text-gray-400 mt-2">取消后将不再收到TA的动态更新</p>
        </div>
        <DialogFooter class="gap-2">
          <Button variant="outline" @click="showUnfollowConfirm = false">再想想</Button>
          <Button class="bg-red-500 hover:bg-red-600 text-white" @click="confirmUnfollow">确认取消</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog :open="showEditDialog" @update:open="showEditDialog = $event">
      <DialogContent class="sm:max-w-[425px] bg-white">
        <DialogHeader>
          <DialogTitle>编辑个人资料</DialogTitle>
        </DialogHeader>
        <div class="space-y-6 py-4">
          <div class="flex justify-center">
            <label class="relative cursor-pointer group">
              <div
                class="w-24 h-24 rounded-full overflow-hidden border-2 border-dashed border-gray-300 group-hover:border-[rgb(33,111,85)] transition relative">
                <img v-if="tempAvatar || editForm.avatar" :src="tempAvatar || getAvatarUrl(editForm.avatar)"
                  class="w-full h-full object-cover" />
                <div v-else class="w-full h-full bg-gray-50 flex items-center justify-center text-gray-400 text-xs">上传头像
                </div>
                <div
                  class="absolute inset-0 bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition text-white text-xs font-bold">
                  裁剪更换
                </div>
              </div>
              <input type="file" ref="avatarFileInput" hidden accept="image/*" @change="handleUpload($event, 'avatar')" />
            </label>
          </div>
          <div class="space-y-2">
            <Label class="text-gray-700">新昵称</Label>
            <Input v-model="editForm.nickname" placeholder="请输入新昵称" />
            <p class="text-[10px] text-orange-400">⚠️ 注意：昵称每 30 天只能修改一次</p>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="showEditDialog = false" :disabled="saving">取消</Button>
          <Button class="bg-[rgb(33,111,85)] hover:bg-[rgb(28,95,72)] text-white disabled:opacity-50"
            @click="saveProfile(false)" :disabled="saving">
            <Loader2 v-if="saving" class="w-4 h-4 mr-2 animate-spin" />
            {{ saving ? '保存中...' : '保存修改' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- 头像裁剪弹窗 -->
    <Dialog :open="showAvatarCropper" @update:open="val => { if (!val) { showAvatarCropper = false; avatarCropperSrc = '' } }">
      <DialogContent class="sm:max-w-[500px] max-h-[90vh] flex flex-col"
        @interactOutside="(e) => e.preventDefault()"
        @pointerDownOutside="(e) => e.preventDefault()">
        <DialogHeader class="flex-shrink-0">
          <DialogTitle class="flex items-center gap-2">
            <Camera class="w-5 h-5 text-[rgb(33,111,85)]" /> 裁剪头像
          </DialogTitle>
        </DialogHeader>
        <div class="flex-1 overflow-y-auto min-h-0 py-4">
          <p class="text-xs text-gray-500 mb-3">拖动选框调整裁剪区域（1:1 比例，建议面部居中）</p>
          <div class="h-72 bg-gray-100 rounded-xl overflow-hidden">
            <Cropper
              ref="avatarCropperRef"
              :src="avatarCropperSrc"
              :stencil-props="{ aspectRatio: 1 }"
              class="h-full"
            />
          </div>
          <p class="text-xs text-gray-400 mt-2 text-center">头像文件不超过 5MB，JPG/PNG/GIF 均可</p>
        </div>
        <DialogFooter class="flex-shrink-0">
          <Button variant="outline" @click="showAvatarCropper = false; avatarCropperSrc = ''" :disabled="avatarUploading">
            取消
          </Button>
          <Button class="bg-[rgb(33,111,85)] hover:bg-[rgb(28,95,72)]" @click="confirmAvatarCrop" :disabled="avatarUploading">
            <Loader2 v-if="avatarUploading" class="w-4 h-4 mr-2 animate-spin" />
            <Camera v-else class="w-4 h-4 mr-2" />
            {{ avatarUploading ? '上传中...' : '确认裁剪' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- 封面裁剪弹窗 -->
    <Dialog :open="showCoverCropper" @update:open="val => { if (!val) { showCoverCropper = false; coverCropperSrc = '' } }">
      <DialogContent class="sm:max-w-[680px] max-h-[90vh] flex flex-col"
        @interactOutside="(e) => e.preventDefault()"
        @pointerDownOutside="(e) => e.preventDefault()">
        <DialogHeader class="flex-shrink-0">
          <DialogTitle class="flex items-center gap-2">
            <Camera class="w-5 h-5 text-[rgb(33,111,85)]" /> 裁剪个人封面
          </DialogTitle>
        </DialogHeader>
        <div class="flex-1 overflow-y-auto min-h-0 py-4">
          <p class="text-xs text-gray-500 mb-3">拖动选框调整裁剪区域（16:5 宽幅比例，建议构图居中）</p>
          <div class="h-48 bg-gray-100 rounded-xl overflow-hidden">
            <Cropper
              ref="coverCropperRef"
              :src="coverCropperSrc"
              :stencil-props="{ aspectRatio: 16 / 5 }"
              class="h-full"
            />
          </div>
          <p class="text-xs text-gray-400 mt-2 text-center">封面文件不超过 10MB，JPG/PNG 均可，建议使用横向风景图</p>
        </div>
        <DialogFooter class="flex-shrink-0">
          <Button variant="outline" @click="showCoverCropper = false; coverCropperSrc = ''" :disabled="coverUploading">
            取消
          </Button>
          <Button class="bg-[rgb(33,111,85)] hover:bg-[rgb(28,95,72)]" @click="confirmCoverCrop" :disabled="coverUploading">
            <Loader2 v-if="coverUploading" class="w-4 h-4 mr-2 animate-spin" />
            <Camera v-else class="w-4 h-4 mr-2" />
            {{ coverUploading ? '保存中...' : '确认裁剪' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!--删除帖子确认弹窗 -->
    <Dialog :open="showDeletePostDialog" @update:open="showDeletePostDialog = $event">
      <DialogContent class="sm:max-w-[400px] bg-white rounded-xl">
        <DialogHeader>
          <DialogTitle class="text-lg font-bold text-red-600 flex items-center gap-2">
            <Trash2 class="w-5 h-5" /> 删除确认
          </DialogTitle>
        </DialogHeader>
        <div class="py-4">
          <p class="text-gray-600 mb-2">确定要删除这条内容吗？</p>
          <p class="text-sm text-gray-400">删除后将无法恢复，相关的评论和点赞也会一并删除。</p>
        </div>
        <DialogFooter class="gap-2">
          <Button variant="outline" @click="showDeletePostDialog = false" :disabled="deletingPost">取消</Button>
          <Button class="bg-red-500 hover:bg-red-600 text-white" @click="confirmDeletePost" :disabled="deletingPost">
            <Loader2 v-if="deletingPost" class="w-4 h-4 mr-2 animate-spin" />
            {{ deletingPost ? '删除中...' : '确认删除' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>