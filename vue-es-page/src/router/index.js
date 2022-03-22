import Vue from 'vue'
import Router from 'vue-router'
import esPage from '@/views/es-page'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path: '/',
      name: 'es-page',
      component: esPage
    }
  ]
})
