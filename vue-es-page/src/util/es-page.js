const esPage = {
  _data: [],
  // 数据补充率
  addRate: 0.65,
  // searchAfter 参数名
  searchAfter: 'searchAfter',
  // 分页内容
  content: {
    // 单页数据
    pageSize: 5,
    // 当前页
    currentPage: 1,
    // 回调方法
    callback: null
  }
}

/**
 * 初始化
 * @param queryForm 查询条件
 * @param callback 回调方法
 */
esPage.init = function (queryForm, callback) {
  this.content.queryForm = queryForm
  this.content.data = []
  this.content.total = 0
  this.content.currentPage = 1
  this.content.callback = callback

  // 初始化分页
  // 执行回调查询数据
  this._applyCallback()

  // 刷新当前data数据
  return esPage._updateData(this.content.currentPage)
}

/**
 * 销毁
 */
esPage.destroy = function () {
  this._data = []
  this.content.data = []
  this.content.currentPage = 1
  delete this.content.queryForm
  delete this.content.callback
  delete this.content.total
  delete this.content.count
}

/**
 * 上一页
 */
esPage.prevPage = function () {
  return this.currentChange(this.content.currentPage - 1)
}

/**
 * 下一页
 */
esPage.nextPage = function () {
  return this.currentChange(this.content.currentPage + 1)
}

/**
 * 直接前往
 * 注：限制为不能超过当前最大分页数
 * @param {number} pageNo
 */
esPage.currentChange = function (pageNo) {
  // 处理非法数据，兜底数据为第一页
  if (pageNo <= 0) {
    pageNo = 1
  }

  // 下一页不可大于 当前总页数
  if (pageNo > this.content.count) {
    console.error('分页参数非法')
    return []
  }

  // 补数据
  // 取当前页 如果大于 预先设置的补充数据比率 则自动补充数据
  // 且不可大于最后一页数据进行补充
  // 如果当前页 等于最后一页需要请求新的数据
  // 同时 对比当前分页 的滚动状态 只有向后滚动才会涉及到补数据的操作
  if (pageNo > this.content.currentPage) {
    if (
      pageNo / this.content.count > this.addRate &&
      pageNo <= this.content.count
    ) {
      if (this._data.length > 0) {
        // 增加ES查询条件
        this.content.queryForm[this.searchAfter] =
          this._data[this._data.length - 1].sort.join(",")
        // 执行回调查询数据
        this._applyCallback()
      }
    }
  }

  // 刷新当前data数据
  return esPage._updateData(pageNo)
}

/**
 * 计算分页数量
 */
esPage._calculateCount = function () {
  let count = this._data.length / this.content.pageSize
  if (this._data.length % this.content.pageSize > 0) {
    count += 1
  }
  return count
}

/**
 * 执行回调函数
 */
esPage._applyCallback = function () {
  // 注意 这里需要拼接 ES特殊查询法
  // 执行回调查询数据
  if (this.content.callback != null) {
    this.content.callback(this.content.queryForm,
      esPage._passiveCallback)
  }
}

/**
 * 被动回调函数
 * 这里不能用 this
 */
esPage._passiveCallback = function (dataList) {
  if (dataList != null && dataList.length > 0) {
    // 增加分页总条数
    esPage.content.total += dataList.length
    // 合并数组
    esPage._data = esPage._data.concat(dataList)
    // 重新计算分页数量
    esPage.content.count = esPage._calculateCount()

    // 刷新当前data数据
    esPage._updateData(esPage.content.currentPage)
  }
}

/**
 * 更新数据
 * 这里不能用 this
 */
esPage._updateData = function (pageNo) {
  // 刷新当前data数据
  let end = pageNo * esPage.content.pageSize
  let begin = end - esPage.content.pageSize
  // 如果还得等于最后一页 则返回最后一页数据
  if (pageNo === esPage.content.count) {
    end = esPage._data.length
    begin = end - esPage.content.pageSize
    // 如果有余数 则需要展示余数页
    if (esPage._data.length % esPage.content.pageSize > 0) {
      begin = end - (esPage._data.length % esPage.content.pageSize)
    }
  }

  esPage.content.currentPage = pageNo
  esPage.content.data = esPage._data.slice(begin, end)
  return esPage.content.data
}


module.exports = {
  esPage
}
