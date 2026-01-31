<script setup lang="ts">
import { ref, computed } from 'vue'
import { DownloadCloud, ChevronDown, RefreshCw, LayoutTemplate, Globe } from 'lucide-vue-next'
import { TemplateService } from '@/services/TemplateService'
import { useQuery } from '@tanstack/vue-query'
import { push } from 'notivue'
import { useImportState } from '@/composables/useImportState'
import BaseButton from '@/components/ui/BaseButton.vue'

// Bridge to the Editor Component
const { setImportYaml } = useImportState()

const isOpen = ref(false)

const { 
    data: repositories, 
    isLoading: isLoadingRepos,
    refetch: refetchRepos
} = useQuery({
    queryKey: ['template-repos'],
    queryFn: TemplateService.getRepositories,
    staleTime: 1000 * 60 * 5 
})

const selectedRepoId = computed(() => repositories.value?.[0]?.id)

const { 
    data: proposals, 
    isLoading: isLoadingProposals,
    refetch: refetchProposals
} = useQuery({
    queryKey: computed(() => ['template-proposals', selectedRepoId.value]),
    queryFn: () => selectedRepoId.value ? TemplateService.getProposals(selectedRepoId.value) : [],
    enabled: computed(() => !!selectedRepoId.value)
})

const loadTemplate = async (templateId: string, templateName: string, explicitLang?: string) => {
    if (!selectedRepoId.value) return
    isOpen.value = false
    
    try {
        let lang = explicitLang || 'en'
        if (!explicitLang) {
            const proposal = proposals.value?.find(p => p.id === templateId)
            const available = proposal?.availableLanguages || []
            if (available.length > 0) {
                const browser = navigator.language.substring(0, 2).toLowerCase()
                lang = available.includes(browser) ? browser : (available.includes('en') ? 'en' : available[0])
            }
        }

        push.info(`Fetching template '${templateName}' (${lang})...`)
        const rawYaml = await TemplateService.getPreview(selectedRepoId.value, templateId, lang)
        
        // Pass to the editor via shared state
        setImportYaml(rawYaml)
        
    } catch (e: any) {
        push.error("Failed to load template: " + e.message)
    }
}

const refreshTemplates = () => {
    refetchRepos()
    if (selectedRepoId.value) refetchProposals()
}
</script>

<template>
  <div class="relative z-50">
    <BaseButton 
      variant="default" 
      size="sm" 
      @click="isOpen = !isOpen" 
      :disabled="isLoadingProposals || isLoadingRepos"
      class="flex items-center gap-2 font-bold transition-all shadow-sm active:scale-95 bg-white dark:bg-gray-800 text-foreground border border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700"
    >
      <DownloadCloud v-if="isLoadingProposals" class="animate-spin" :size="14" />
      <DownloadCloud v-else :size="14" />
      <span>Load Template</span>
      <ChevronDown :size="14" class="opacity-70" />
    </BaseButton>

    <!-- Dropdown Menu -->
    <div v-if="isOpen" class="absolute right-0 top-full mt-1 w-96 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-2xl z-[100] overflow-hidden animate-in fade-in zoom-in-95 duration-100 origin-top-right">
      <div class="bg-muted/30 px-3 py-2 border-b border-gray-100 dark:border-gray-700 flex justify-between items-center">
        <span class="text-[10px] font-bold text-muted-foreground uppercase tracking-wider">Select Manifest</span>
        <button @click="refreshTemplates" class="text-primary hover:bg-primary/10 p-1 rounded transition-colors" title="Refresh"><RefreshCw :size="12"/></button>
      </div>
      
      <div class="max-h-[400px] overflow-y-auto p-1 custom-scrollbar">
         <div v-if="!proposals?.length" class="px-4 py-3 text-xs text-muted-foreground italic text-center">No templates found.</div>
         <div v-else>
             <div 
                v-for="tpl in proposals" 
                :key="tpl.id" 
                class="w-full flex items-start gap-3 p-2.5 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-md group transition-colors cursor-default"
             >
                 <!-- Icon -->
                 <div class="p-2 bg-gray-100 dark:bg-gray-700 group-hover:bg-white dark:group-hover:bg-gray-600 rounded-md text-gray-500 dark:text-gray-400 group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors shadow-sm shrink-0">
                    <LayoutTemplate :size="18" />
                 </div>
                 
                 <div class="flex-1 min-w-0">
                     <div class="text-sm font-bold text-gray-800 dark:text-gray-200 group-hover:text-blue-700 dark:group-hover:text-blue-300 mb-0.5 truncate">{{ tpl.name }}</div>
                     <div v-if="tpl.description" class="text-[11px] text-gray-500 dark:text-gray-400 leading-tight mb-2 line-clamp-2">{{ tpl.description }}</div>
                     
                     <div class="flex flex-wrap gap-1.5">
                         <button 
                            v-for="lang in (tpl.availableLanguages?.length ? tpl.availableLanguages : ['default'])" 
                            :key="lang" 
                            @click="loadTemplate(tpl.id, tpl.name, lang === 'default' ? undefined : lang)" 
                            class="flex items-center gap-1 text-[10px] font-bold text-gray-500 dark:text-gray-400 hover:text-white border border-gray-200 dark:border-gray-600 hover:border-blue-500 hover:bg-blue-500 px-2 py-0.5 rounded transition-all uppercase shadow-sm"
                         >
                            <Globe :size="10" /> {{ lang }}
                         </button>
                     </div>
                 </div>
             </div>
         </div>
      </div>
    </div>

    <!-- Backdrop -->
    <div v-if="isOpen" @click="isOpen = false" class="fixed inset-0 z-[99] bg-black/5 cursor-default"></div>
  </div>
</template>