// File: vue-frontend/src/api/post.js
import request from "@/api/request";

/**
 * 获取帖子列表
 * @param {number} pageNum - 页码
 * @param {number} pageSize - 每页数量
 * @param {number} currentUserId - 当前登录用户ID (用于判断点赞状态、黑名单过滤等)
 * @param {number} type - 0:推荐广场 1:关注流
 * @param {number} category - 分类ID (0表示全部)
 * @param {string} keyword - 搜索关键词
 * @param {number} userId - 目标用户ID (用于筛选特定用户的帖子，0表示不筛选)
 */
export function getPostList(
  pageNum = 1,
  pageSize = 10,
  currentUserId = 0,
  type = 0,
  category = 0,
  keyword = "",
  userId = 0,
) {
  return request.get("/post/list", {
    params: {
      pageNum,
      pageSize,
      currentUserId,
      userId,
      type,
      category,
      keyword,
    },
  });
}

export function getPostDetail(id, currentUserId = 0) {
  return request.get("/post/detail", { params: { id, userId: currentUserId } });
}

/**
 * 删除帖子
 * @param {number} id - 帖子ID
 * @param {number} userId - 当前用户ID
 */
export function deletePost(id, userId) {
  return request.delete(`/post/${id}`, { params: { userId } });
}
