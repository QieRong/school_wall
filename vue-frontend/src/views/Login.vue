<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Loader2 } from 'lucide-vue-next'
import request from '@/api/request'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { logger } from '@/utils/logger'

logger.log('Login.vue 组件正在加载...')

const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()
const isLogin = ref(true)
const loading = ref(false)
const form = reactive({ account: '', password: '', confirmPassword: '' })

// 密码强度计算
const passwordStrength = computed(() => {
  const pwd = form.password
  if (!pwd) return { level: 0, text: '', color: '' }
  let score = 0
  if (pwd.length >= 6) score++
  if (pwd.length >= 8) score++
  if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) score++
  if (/\d/.test(pwd)) score++
  if (/[!@#$%^&*(),.?":{}|<>]/.test(pwd)) score++

  if (score <= 1) return { level: 1, text: '弱', color: 'bg-red-500' }
  if (score <= 2) return { level: 2, text: '较弱', color: 'bg-orange-500' }
  if (score <= 3) return { level: 3, text: '中等', color: 'bg-yellow-500' }
  if (score <= 4) return { level: 4, text: '强', color: 'bg-green-500' }
  return { level: 5, text: '很强', color: 'bg-emerald-500' }
})

const toggleMode = () => {
  isLogin.value = !isLogin.value
  form.password = ''
  form.confirmPassword = ''
}

const handleSubmit = async () => {
  if (!form.account || !form.password) {
    return appStore.showToast('账号和密码不能为空', 'error')
  }

  // 问题9修复：账号格式校验（登录和注册都需要，admin除外）
  if (form.account !== 'admin') {
    const accountReg = /^(\d{10}|\d{11})$/
    if (!accountReg.test(form.account)) {
      return appStore.showToast('账号格式错误，请输入10位学号或11位手机号', 'error')
    }
  }

  // 注册时的额外校验
  if (!isLogin.value) {
    if (!form.confirmPassword) {
      return appStore.showToast('请确认密码', 'error')
    }

    if (form.password !== form.confirmPassword) {
      return appStore.showToast('两次输入的密码不一致', 'error')
    }
  }

  loading.value = true
  try {
    const url = isLogin.value ? '/user/login' : '/user/register'
    const res = await request.post(url, form)

    if (res.code && String(res.code) !== '200') {
      throw new Error(res.msg || '操作失败')
    }

    if (isLogin.value) {
      appStore.showToast('🎉 欢迎回来，正在跳转...', 'success')

      //使用 Store 更新状态，而不是直接写 localStorage
      userStore.login(res.data)

      setTimeout(() => {
        // 管理员强制跳后台，普通用户跳前台
        if (res.data.role === 1 || form.account === 'admin') {
          window.location.href = '/admin/dashboard' // 强制刷新跳转，防止状态残留
        } else {
          router.replace('/home')
        }
      }, 500)
    } else {
      appStore.showToast('✅ 注册成功！请登录', 'success')
      isLogin.value = true
    }

  } catch (error) {
    appStore.showToast(error.message || error.msg || '请求失败', 'error')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // 确保loading状态被重置
  loading.value = false

  // 清除appStore的loading状态
  appStore.stopLoading()

  // 清空表单
  form.account = ''
  form.password = ''
  form.confirmPassword = ''

  // console.log('Login.vue 组件已挂载完成')
  // console.log('当前loading状态:', loading.value)
  // console.log('appStore.isLoading:', appStore.isLoading)
})

// 添加onUnmounted钩子清理状态
onUnmounted(() => {
  // 组件卸载时清理状态
  loading.value = false
})
</script>

<template>
  <!--使用最简单的结构，确保始终可见 -->
  <div class="min-h-screen flex items-center justify-center bg-zinc-900 p-4 overflow-hidden relative">
    <!-- 背景层 -->
    <div class="absolute inset-0 z-0 pointer-events-none">
      <!-- 背景图片：放大并左移，避免黑边 -->
      <img src="/login-bg.jpg" alt="背景" class="w-full h-full object-cover"
        style="object-position: 230% center; transform: scale(1.15);">
      <div class="absolute inset-0 bg-gradient-to-br from-[rgb(33,111,85)]/20 to-black/40" />
    </div>

    <!-- 主要内容 -->
    <div
      class="w-full max-w-5xl h-[600px] rounded-[3rem] shadow-2xl overflow-hidden relative flex animate-float z-20 border border-white/20">
      <!-- 左侧品牌区域 -->
      <div class="hidden md:flex flex-1 relative flex-col justify-between p-12 text-white bg-black/30 backdrop-blur-sm">
        <div class="flex items-center gap-4">
          <div
            class="w-16 h-16 rounded-full bg-white/90 p-1 shadow-lg flex items-center justify-center overflow-hidden">
            <img src="/Round_school_logo.png" alt="张家界学院校徽" class="w-full h-full object-contain">
          </div>
          <div>
            <h2 class="text-2xl font-bold tracking-widest">
              张家界学院
            </h2>
            <span class="text-sm opacity-80 tracking-[0.3em]">CONFESSION WALL</span>
          </div>
        </div>
        <div class="space-y-6">
          <h1 class="text-5xl font-bold leading-tight drop-shadow-lg">
            记录美好<br><span class="text-[#4ade80]">遇见心动</span>
          </h1>
          <p class="text-lg font-light opacity-90 leading-relaxed">
            这里是专属于 ZJJ U 的社交空间。<br>分享你的故事，倾听TA的声音。
          </p>
        </div>
        <div class="text-xs opacity-50 font-mono">
          Designed by TXY © 2025
        </div>
      </div>

      <!-- 右侧登录区域 -->
      <div class="flex-1 relative flex items-center justify-center p-8 bg-white/85 backdrop-blur-xl">
        <!-- 表单容器：z-30，确保在所有元素之上 -->
        <div class="w-full max-w-sm space-y-8 relative z-30">
          <div class="text-center">
            <h2 class="text-3xl font-bold text-[rgb(33,111,85)] mb-2">
              {{ isLogin ? '欢迎回来' : '加入我们' }}
            </h2>
            <p class="text-gray-500 text-sm">
              {{ isLogin ? '登录账号，开启你的校园之旅' : '只需几步，成为社区一员' }}
            </p>
          </div>

          <div class="space-y-5">
            <!-- 账号输入 -->
            <div class="space-y-2">
              <label class="text-xs font-bold text-gray-600 ml-1">账号</label>
              <Input v-model="form.account"
                class="bg-white/60 border-gray-300 focus:border-[rgb(33,111,85)] h-12 rounded-xl transition-all hover:bg-white"
                placeholder="请输入学号 / 手机号" aria-label="账号" aria-required="true" @keyup.enter="handleSubmit" />
            </div>

            <!-- 密码输入 -->
            <div class="space-y-2">
              <label class="text-xs font-bold text-gray-600 ml-1">密码</label>
              <Input v-model="form.password"
                class="bg-white/60 border-gray-300 focus:border-[rgb(33,111,85)] h-12 rounded-xl transition-all hover:bg-white"
                type="password" placeholder="请输入密码" aria-label="密码" aria-required="true" @keyup.enter="handleSubmit" />
            </div>

            <!-- 密码强度指示器（仅注册时显示） -->
            <div v-if="!isLogin && form.password" class="space-y-2">
              <div class="flex gap-1">
                <div v-for="i in 5" :key="i" class="h-1.5 flex-1 rounded-full transition-all duration-300"
                  :class="i <= passwordStrength.level ? passwordStrength.color : 'bg-gray-200'" />
              </div>
              <div class="flex justify-between items-center text-xs">
                <span
                  :class="passwordStrength.level >= 3 ? 'text-green-600' : passwordStrength.level >= 2 ? 'text-yellow-600' : 'text-red-500'">
                  密码强度: {{ passwordStrength.text }}
                </span>
                <span v-if="passwordStrength.level < 3" class="text-orange-500">
                  {{ passwordStrength.level < 2 ? '⚠️ 密码太弱' : '💡 建议增强' }} </span>
                    <span v-else class="text-green-500">
                      ✓ 安全
                    </span>
              </div>
            </div>

            <!-- 确认密码（仅注册时显示） -->
            <div v-if="!isLogin" class="space-y-2">
              <label class="text-xs font-bold text-gray-600 ml-1">确认密码</label>
              <Input v-model="form.confirmPassword"
                class="bg-white/60 border-gray-300 focus:border-primary h-12 rounded-xl transition-all hover:bg-white"
                type="password" placeholder="请再次输入密码" />
            </div>

            <!-- 提交按钮 -->
            <Button :disabled="loading" :aria-label="loading ? '正在处理' : (isLogin ? '登录' : '注册')" :aria-busy="loading"
              class="w-full h-12 text-lg font-bold bg-primary hover:bg-primary/90 text-white shadow-xl shadow-green-900/20 rounded-xl transition-all active:scale-95"
              @click="handleSubmit">
              <Loader2 v-if="loading" class="w-5 h-5 mr-2 animate-spin" />
              <span v-if="loading">处理中...</span>
              <span v-else>{{ isLogin ? '立 即 登 录' : '注 册 账 号' }}</span>
            </Button>
          </div>

          <!-- 切换登录/注册 -->
          <div class="text-center">
            <button
              class="text-sm text-gray-500 hover:text-primary transition underline decoration-dashed underline-offset-4"
              @click="toggleMode">
              {{ isLogin ? '没有账号？去注册' : '已有账号？去登录' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@keyframes float {

  0%,
  100% {
    transform: translateY(0px);
  }

  50% {
    transform: translateY(-15px);
  }
}

.animate-float {
  animation: float 6s ease-in-out infinite;
}
</style>