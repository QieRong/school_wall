<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-content chalkboard">
      <button class="close-btn" @click="$emit('close')">×</button>

      <h2 class="chalk-title">📝 投稿热词</h2>

      <form @submit.prevent="handleSubmit">
        <!-- 热词名称 -->
        <div class="form-group">
          <label>热词名称 <span class="counter">{{ form.name.length }}/10</span></label>
          <input v-model="form.name" type="text" maxlength="10" placeholder="输入热词..." class="chalk-input"
            :class="{ shake: nameError }" @input="nameError = false" />
        </div>

        <!-- 释义 -->
        <div class="form-group">
          <label>释义 <span class="required">*</span> <span class="counter">{{ form.definition.length
              }}/200</span></label>
          <textarea v-model="form.definition" maxlength="200" placeholder="这个词是什么意思..." class="chalk-input"
            rows="3"></textarea>
        </div>

        <!-- 例句 -->
        <div class="form-group">
          <label>例句 <span class="counter">{{ form.example.length }}/100</span></label>
          <input v-model="form.example" type="text" maxlength="100" placeholder="举个例子..." class="chalk-input" />
        </div>

        <!-- 标签选择 -->
        <div class="form-group">
          <label>标签 <span class="hint">(选择1-3个)</span></label>
          <div class="tag-list">
            <span v-for="tag in availableTags" :key="tag" :class="['tag', { selected: form.tags.includes(tag) }]"
              @click="toggleTag(tag)">
              {{ tag }}
            </span>
          </div>
        </div>

        <!-- 配图上传 -->
        <div class="form-group">
          <label>配图 <span class="hint">(可选，最大10MB)</span></label>
          <div class="image-upload">
            <input type="file" accept="image/jpeg,image/png" @change="handleImageUpload" ref="fileInput" hidden />
            <div v-if="form.imageUrl" class="preview">
              <img :src="form.imageUrl" alt="预览" />
              <button type="button" class="remove-btn" @click="removeImage">×</button>
            </div>
            <button v-else type="button" class="upload-btn" @click="$refs.fileInput.click()">
              📷 上传图片
            </button>
          </div>
        </div>

        <button type="submit" class="submit-btn" :disabled="submitting">
          {{ submitting ? '投稿中...' : '🚀 发射上墙' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { createHotword } from '@/api/hotword'
import request from '@/api/request'

const emit = defineEmits(['close', 'success'])
const userStore = useUserStore()
const appStore = useAppStore()

const availableTags = [
  '老师梗', '地点梗', '事件梗', '食堂梗',
  '宿舍梗', '考试梗', '社团梗', '恋爱梗',
  '图书馆', '体育馆', '校园日常', '其他'
]

const form = reactive({
  name: '',
  definition: '',
  example: '',
  tags: [],
  imageUrl: ''
})

const nameError = ref(false)
const submitting = ref(false)
const fileInput = ref(null)

const toggleTag = (tag) => {
  const idx = form.tags.indexOf(tag)
  if (idx > -1) {
    form.tags.splice(idx, 1)
  } else if (form.tags.length < 3) {
    form.tags.push(tag)
  }
}

const handleImageUpload = async (e) => {
  const file = e.target.files[0]
  if (!file) return

  if (file.size > 10 * 1024 * 1024) {
    appStore.showToast('图片大小不能超过10MB', 'error')
    return
  }

  if (!['image/jpeg', 'image/png'].includes(file.type)) {
    appStore.showToast('仅支持JPG/PNG格式图片', 'error')
    return
  }

  const formData = new FormData()
  formData.append('file', file)
  formData.append('userId', userStore.user?.id)

  try {
    const res = await request.post('/file/upload', formData)
    form.imageUrl = res.data
  } catch (e) {
    appStore.showToast('图片上传失败', 'error')
  }
}

const removeImage = () => {
  form.imageUrl = ''
}

const handleSubmit = async () => {
  if (!form.name.trim()) {
    nameError.value = true
    return
  }
  if (!form.definition.trim()) {
    appStore.showToast('请填写释义', 'error')
    return
  }
  if (form.tags.length === 0) {
    appStore.showToast('请至少选择一个标签', 'error')
    return
  }

  submitting.value = true
  try {
    await createHotword({
      name: form.name.trim(),
      definition: form.definition.trim(),
      example: form.example.trim() || null,
      tags: form.tags,
      imageUrl: form.imageUrl || null
    }, userStore.user?.id)

    appStore.showToast('你的梗已上墙！🎉', 'success')
    emit('success')
  } catch (e) {
    appStore.showToast(e.response?.data?.message || '投稿失败', 'error')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  position: relative;
  width: 90%;
  max-width: 480px;
  max-height: 90vh;
  overflow-y: auto;
  padding: 32px;
  border-radius: 24px;
}

.chalkboard {
  background: linear-gradient(135deg,
      rgba(5, 150, 105, 0.95) 0%,
      rgba(16, 185, 129, 0.95) 50%,
      rgba(52, 211, 153, 0.95) 100%);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 25px 50px rgba(5, 150, 105, 0.4);
}

.close-btn {
  position: absolute;
  top: 16px;
  right: 20px;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: #fff;
  font-size: 24px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.3s;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: rotate(90deg);
}

.chalk-title {
  text-align: center;
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  text-shadow: 2px 2px 8px rgba(0, 0, 0, 0.2);
  margin-bottom: 28px;
}

.form-group {
  margin-bottom: 24px;
}

.form-group label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #fff;
  margin-bottom: 10px;
  font-weight: 600;
  font-size: 15px;
}

.required {
  color: #ffd700;
}

.counter {
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  font-weight: 400;
}

.hint {
  color: rgba(255, 255, 255, 0.6);
  font-size: 13px;
  font-weight: 400;
}

.chalk-input {
  width: 100%;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.2);
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 12px;
  color: #fff;
  font-size: 15px;
  outline: none;
  transition: all 0.3s;
  backdrop-filter: blur(10px);
}

.chalk-input::placeholder {
  color: rgba(255, 255, 255, 0.6);
}

.chalk-input:focus {
  border-color: rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.25);
  box-shadow: 0 0 20px rgba(255, 255, 255, 0.2);
}

.chalk-input.shake {
  animation: shake 0.5s;
  border-color: #ffd700;
}

@keyframes shake {

  0%,
  100% {
    transform: translateX(0);
  }

  25% {
    transform: translateX(-8px);
  }

  75% {
    transform: translateX(8px);
  }
}

.tag-list {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

@media (max-width: 480px) {
  .tag-list {
    grid-template-columns: repeat(3, 1fr);
  }
}

.tag {
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.2);
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  color: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  transition: all 0.3s;
  font-weight: 500;
  font-size: 13px;
  text-align: center;
  white-space: nowrap;
}

.tag:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-2px);
}

.tag.selected {
  background: rgba(255, 255, 255, 0.95);
  border-color: white;
  color: rgb(5, 150, 105);
  font-weight: 600;
  box-shadow: 0 4px 15px rgba(255, 255, 255, 0.3);
  transform: scale(1.02);
}

.image-upload {
  display: flex;
  align-items: center;
}

.upload-btn {
  padding: 14px 28px;
  background: rgba(255, 255, 255, 0.2);
  border: 2px dashed rgba(255, 255, 255, 0.4);
  border-radius: 12px;
  color: #fff;
  cursor: pointer;
  font-size: 15px;
  transition: all 0.3s;
}

.upload-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  border-color: rgba(255, 255, 255, 0.6);
}

.preview {
  position: relative;
  width: 120px;
  height: 120px;
}

.preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 12px;
  border: 3px solid rgba(255, 255, 255, 0.3);
}

.remove-btn {
  position: absolute;
  top: -10px;
  right: -10px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #ff6b6b;
  border: 2px solid white;
  color: #fff;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.3s;
}

.remove-btn:hover {
  transform: scale(1.1);
  background: #ff4757;
}

.submit-btn {
  width: 100%;
  padding: 16px;
  background: white;
  border: none;
  border-radius: 30px;
  color: rgb(5, 150, 105);
  font-size: 17px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
  margin-top: 8px;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 12px 35px rgba(0, 0, 0, 0.2);
  background: rgba(255, 255, 255, 0.95);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

/* 滚动条美化 */
.modal-content::-webkit-scrollbar {
  width: 6px;
}

.modal-content::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
}

.modal-content::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 3px;
}
</style>
