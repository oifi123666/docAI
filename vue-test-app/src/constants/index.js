/**
 * @description 全局通用常量类
 */

// 正则表达式常量
export const REGEX = {
  EMAIL: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
  PHONE: /^1[3-9]\d{9}$/,
  PASSWORD_FULL: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/,

  HAS_UPPER: /[A-Z]/,        // 包含大写字母
  HAS_LOWER: /[a-z]/,        // 包含小写字母
  HAS_NUMBER: /\d/,          // 包含数字
  HAS_SPECIAL: /[@$!%*?&]/   // 包含特殊符号
}

// 浏览器缓存 Key 常量
export const STORAGE_KEYS = {
  ACCESS_TOKEN: 'accessToken',
  REFRESH_TOKEN: 'refreshToken',
  USER_INFO: 'userInfo'
}

// 后端响应状态码常量
export const RES_CODE = {
  SUCCESS: 200,
  UNAUTHORIZED: 401,
  INTERNAL_SERVER_ERROR: 500
}