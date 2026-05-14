/**
 * @description 文件模块 API 接口
 */
import request from '../utils/request'

export const fileApi = {
    /**
     * 获取文件列表 (支持分页)
     * @param {Object} params { page: 1, size: 10, sortBy: 'createTime', direction: 'desc' }
     */
    getFileList: (params) => {
        return request.get('/files/list', { params })
    },

    /**
     * 搜索文件
     * @param {Object} params { keyword: '搜索词', page: 1, size: 10 }
     */
    searchFiles: (params) => {
        return request.get('/files/search', { params })
    },

    /**
     * 获取文件详细元数据
     * @param {String} fileId 文件唯一标识
     */
    getMetadata: (fileId) => {
        return request.get(`/files/metadata/${fileId}`)
    },


    /**
     * 单文件上传 (FormData 格式)
     * @param {File} rawFile 原生文件对象 (从 input type="file" 或 el-upload 中获取)
     */
    upload: (rawFile) => {
        const formData = new FormData()
        formData.append('file', rawFile)

        return request.post('/files/upload', formData, {
            // 更加健壮的进度条监听
            onUploadProgress: (progressEvent) => {
                // 如果后端没返回总大小，我们设为 0
                const total = progressEvent.total || 0;
                const loaded = progressEvent.loaded || 0;

                if (total > 0) {
                    const percent = Math.round((loaded * 100) / total);
                    console.log(`文件上传进度: ${percent}%`);
                } else {
                    console.log('正在上传，但无法计算具体百分比...');
                }
            }
        })
    },

    /**
     * 文件下载 (返回二进制数据流 Blob)
     * @param {String} fileId 文件ID
     */
    download: (fileId) => {
        return request.get(`/files/download/${fileId}`, {
            responseType: 'blob'
        })
    },

    /**
     * 文件预览 (主要用于图片、PDF，返回流)
     * @param {String} fileId 文件ID
     */
    preview: (fileId) => {
        return request.get(`/files/preview/${fileId}`, {
            responseType: 'blob'
        })
    },


    /**
     * 删除文件
     * @param {String} fileId 文件ID
     */
    delete: (fileId) => {
        return request.delete(`/files/${fileId}`)
    },

    /**
     * 重命名文件
     * @param {String} fileId 文件ID
     * @param {String} newName 新文件名
     */
    rename: (fileId, newName) => {
        // 注意：newName 后端要求通过 @RequestParam 接收，所以放在 params 里
        return request.put(`/files/${fileId}/rename`, null, {
            params: { newName }
        })
    }
}