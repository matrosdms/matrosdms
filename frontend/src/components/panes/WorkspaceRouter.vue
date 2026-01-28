<script setup lang="ts">
import { computed } from 'vue'
import { useUIStore } from '@/stores/ui'
import { ViewMode } from '@/enums'

// Components
import ContextCreateForm from '@/components/forms/ContextCreateForm.vue'
import ContextEditForm from '@/components/forms/ContextEditForm.vue'
import ContextArchiveForm from '@/components/forms/ContextArchiveForm.vue'
import CategoryCreateForm from '@/components/forms/CategoryCreateForm.vue'
import CategoryEditForm from '@/components/forms/CategoryEditForm.vue'
import ItemAddForm from '@/components/forms/ItemAddForm.vue'
import ItemEditForm from '@/components/forms/ItemEditForm.vue'
import ItemArchiveForm from '@/components/forms/ItemArchiveForm.vue'
import ActionForm from '@/components/forms/ActionForm.vue'
import PreviewPane from '@/components/panes/PreviewPane.vue'
import ContextItemList from '@/components/panes/ContextItemList.vue'

const ui = useUIStore()

defineProps<{
    layout?: number[]
}>()

defineEmits(['update:layout'])

// --- MAP VIEW MODE TO COMPONENT ---
const VIEW_COMPONENTS: Record<string, any> = {
  [ViewMode.CREATE_CONTEXT]: ContextCreateForm,
  [ViewMode.EDIT_CONTEXT]: ContextEditForm,
  [ViewMode.ARCHIVE_CONTEXT]: ContextArchiveForm,
  [ViewMode.CREATE_CATEGORY]: CategoryCreateForm,
  [ViewMode.EDIT_CATEGORY]: CategoryEditForm,
  [ViewMode.ADD_ITEM]: ItemAddForm,
  [ViewMode.EDIT_ITEM]: ItemEditForm,
  [ViewMode.ARCHIVE_ITEM]: ItemArchiveForm,
  [ViewMode.CREATE_ACTION]: ActionForm,
  [ViewMode.PREVIEW]: PreviewPane
}

const activeViewComponent = computed(() => VIEW_COMPONENTS[ui.rightPanelView])
</script>

<template>
  <div class="h-full w-full relative">
    <!-- 1. FORM / PREVIEW MODE (Replaces Table) -->
    <component 
      v-if="activeViewComponent" 
      :is="activeViewComponent" 
      v-bind="ui.panelData"
    />

    <!-- 2. DEFAULT LIST MODE (Passes Layout Props down) -->
    <ContextItemList 
      v-else 
      :layout="layout"
      @update:layout="$emit('update:layout', $event)"
    />
  </div>
</template>