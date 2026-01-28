/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  // Removed 'any' to encourage stricter typing in Phase 2
  const component: DefineComponent<{}, {}, any>
  export default component
}

declare module 'splitpanes' {
    import { DefineComponent } from 'vue';
    export const Splitpanes: DefineComponent<any, any, any>;
    export const Pane: DefineComponent<any, any, any>;
}