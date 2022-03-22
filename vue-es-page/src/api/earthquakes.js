import request from '../util/request'

export default{
  // 查询分页列表
  page (data) {
    return request({
      url: '/es/page',
      method: 'get',
      params: data
    })
  }
}
