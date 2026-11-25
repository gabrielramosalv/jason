import { createRouter, createWebHashHistory } from 'vue-router'
import AppLayout from '@/app/layouts/AppLayout.vue'

// Roteamento por hash: o Peel Web não faz fallback para index.html em rota sem extensão (D-154).
const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      component: AppLayout,
      children: [
        {
          path: '',
          name: 'inspector',
          component: () => import('@/features/inspector/pages/InspectorPage.vue'),
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: { name: 'inspector' } },
  ],
})

export default router
