<script lang="ts">
// Async wrapper: keeps the full lucide-vue-next namespace import (needed for
// dynamic icon-by-name lookup) out of the main bundle. Props are forwarded
// transparently to DynamicIconInner by defineAsyncComponent.
import { defineAsyncComponent } from 'vue'

export default defineAsyncComponent(() => import('./DynamicIconInner.vue'))
</script>
