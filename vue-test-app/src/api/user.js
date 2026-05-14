/**
 * @description 用户模块 API 接口
 */
import request from '../utils/request'

export const userApi = {
    // 登录接口
    login: (data) => request.post('/users/login', data),
    // 注册接口
    register: (data) => request.post('/users/register', data),

    // 获取用户信息
    getUserInfo: (id) => {
        return request.get(`/users/${id}`)
    }
}