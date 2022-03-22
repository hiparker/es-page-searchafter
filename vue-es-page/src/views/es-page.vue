<template>
  <div class="es-page">
    <el-row>
      <el-table
        :data="page.data"
        stripe
        style="width: 100%">
        <el-table-column
          prop="time"
          label="日期"
          width="180">
        </el-table-column>
        <el-table-column
          prop="magnitude"
          label="地址等级"
          width="180">
        </el-table-column>
        <el-table-column
          prop="lon"
          label="经度">
        </el-table-column>
        <el-table-column
          prop="lat"
          label="纬度">
        </el-table-column>
        <el-table-column
          prop="depth"
          label="深度">
        </el-table-column>
        <el-table-column
          prop="area"
          label="地址">
        </el-table-column>
      </el-table>
      <el-pagination
        layout="prev, pager, next"
        :current-page="page.currentPage"
        :page-size="page.pageSize"
        :total="page.total"
        @current-change="handleCurrentChange"
      >
      </el-pagination>
    </el-row>
  </div>
</template>

<script>
import earthquakesApi from '../api/earthquakes'
import {esPage} from '../util/es-page'

export default {
  name: 'es-page',
  data () {
    return {
      queryForm: {
        "tes": 123
      },
      page: {}
    }
  },
  mounted () {
    // 初始化
    // 参数1 为 当前查询条件
    // 参数2 为 回调函数（当前查询条件, 回调通知函数）
    esPage.init(this.queryForm, (queryForm, callback) => {
      earthquakesApi.page(queryForm).then(response => {
        // 执行回调函数 将数据再传送回去
        callback(response)
      })
    })
    this.page = esPage.content
  },
  destroyed () {
    // 销毁
    esPage.destroy()
  },
  methods: {
    handleCurrentChange(val) {
      // 分页
      esPage.currentChange(val)
    }
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
.es-page{
  width: 80%;
  margin: 0 auto;
}
</style>
