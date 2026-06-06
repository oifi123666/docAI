/**
 * @description 文档业务 API
 */
import request from '../utils/request'

export const docApi = {
    // 获取用户所有的文档列表
    getUserDocs: (userId) => {
        return request.get(`/documents/user/${userId}`)
    },

    // 获取单个文档详情（含 content 和 summary）
    getDocDetail: (id) => {
        return request.get(`/documents/${id}`)
    },

    // 新增全文搜索接口
    searchDocs: (keyword) => {
        return request.get('/documents/search', {
            params: { keyword }
        })
    },

    // 创建文档
    createDoc: (data) => {
        return request.post('/documents', data) // data 对应后端的 DocumentCreateDTO
    },

    // 更新文档内容 (保存编辑)
    updateDoc: (id, data) => {
        return request.put(`/documents/${id}`, data)
    },

    // 获取文档所有历史版本
    getVersions: (id) => {
        return request.get(`/documents/${id}/versions`)
    },

    // 恢复到指定版本
    restoreVersion: (id, versionNumber) => {
        return request.post(`/documents/${id}/restore/${versionNumber}`)
    },

    // 删除文档
    deleteDoc: (id) => {
        return request.delete(`/documents/${id}`)
    },

    grantCollaborator: (id, collaboratorUserId, role = 'editor') => {
        return request.post(`/documents/${id}/collaborators/${collaboratorUserId}`, null, {
            params: { role }
        })
    }
}
