<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { CategoryService } from '@/services/CategoryService'
import { TemplateService } from '@/services/TemplateService'
import { push } from 'notivue'
import { 
    Play, RotateCcw, FileText, DownloadCloud, ChevronDown, 
    RefreshCw, Globe, ShieldCheck, CheckCircle2, Lock, AlignLeft, AlertTriangle
} from 'lucide-vue-next'
import { useQuery, useQueryClient } from '@tanstack/vue-query'
import IndentEditor from '@/components/ui/IndentEditor.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import { ERootCategory, type ERootCategoryType } from '@/enums'
import { getTagStyle } from '@/utils/tagStyles'
import { queryKeys } from '@/composables/queries/queryKeys'

const props = defineProps<{
    item?: { key: string; name: string }
}>()

const queryClient = useQueryClient()

const editorRef = ref<InstanceType<typeof IndentEditor> | null>(null)
const editorContent = ref('')
const logs = ref<string[]>([])
const isProcessing = ref(false)
const isTemplateMenuOpen = ref(false)
const replaceExisting = ref(false)

const templateCache = ref<Partial<Record<ERootCategoryType, string>>>({})
const successfulImports = ref<Set<string>>(new Set())

const selectedRootKey = computed(() => props.item?.key as ERootCategoryType | undefined)

const isLocked = computed(() => 
    selectedRootKey.value && successfulImports.value.has(selectedRootKey.value)
)

const headerStyle = computed(() => {
    if(!selectedRootKey.value) return {}
    const s = getTagStyle(selectedRootKey.value)
    return { backgroundColor: s.backgroundColor, color: s.color, borderColor: s.borderColor }
})

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
    queryKey: ['template-proposals', selectedRepoId],
    queryFn: () => selectedRepoId.value ? TemplateService.getProposals(selectedRepoId.value) : [],
    enabled: computed(() => !!selectedRepoId.value)
})

const splitYamlByRootKeys = (yaml: string): Partial<Record<ERootCategoryType, string>> => {
    const lines = yaml.split('\n')
    const result: Partial<Record<ERootCategoryType, string>> = {}
    const validCategories = Object.values(ERootCategory) as string[];
    
    let currentKey: ERootCategoryType | null = null
    let buffer: string[] = []

    const flush = () => {
        if (currentKey && buffer.length > 0) {
            const content = buffer
                .map(l => l.startsWith('  ') ? l.substring(2) : l) 
                .join('\n')
            result[currentKey] = content.trim()
        }
    }

    for (const line of lines) {
        const match = line.match(/^(\w+):\s*$/)
        if (match) {
            flush()
            const rawKey = match[1].toUpperCase();
            if (validCategories.includes(rawKey)) {
                currentKey = rawKey as ERootCategoryType;
                buffer = [];
                continue;
            }
            const strippedKey = rawKey.replace(/^ROOT_/, '');
            if (validCategories.includes(strippedKey)) {
                currentKey = strippedKey as ERootCategoryType;
                buffer = [];
                continue;
            }
            currentKey = null;
            buffer = [];
        } else {
            if (currentKey) buffer.push(line)
        }
    }
    flush()
    return result
}

const loadTemplate = async (templateId: string, templateName: string, explicitLang?: string) => {
    if (!selectedRepoId.value) return
    isTemplateMenuOpen.value = false
    
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
        const chunks = splitYamlByRootKeys(rawYaml)
        templateCache.value = chunks
        populateEditorFromCache()
        
        const foundKeys = Object.keys(chunks).join(', ')
        push.success(`Loaded. Dimensions found: ${foundKeys || 'None'}`)
        logs.value.push(`✅ Loaded template: ${templateName}`)
        logs.value.push(`   Mapped keys: ${foundKeys}`)
        
    } catch (e: any) {
        push.error("Failed to load template: " + e.message)
        logs.value.push(`❌ Load Error: ${e.message}`)
    }
}

const populateEditorFromCache = () => {
    logs.value = []
    if (!selectedRootKey.value) {
        editorContent.value = ''
        return
    }
    const cachedYaml = templateCache.value[selectedRootKey.value]
    editorContent.value = cachedYaml || ''
}

watch(() => props.item?.key, () => {
    populateEditorFromCache()
}, { immediate: true })

const clearEditor = () => {
    editorContent.value = ''
    logs.value = []
}

const performFormat = () => {
    if (editorRef.value) {
        editorRef.value.autoFormat()
        push.info("Code formatted")
    }
}

const executeProcess = async (simulate: boolean) => {
    if (!selectedRootKey.value) return
    if (!editorContent.value.trim()) {
        push.warning("Editor is empty")
        return
    }

    if (!simulate && replaceExisting.value) {
        if (!confirm(`WARNING: You are about to DELETE ALL existing categories in '${selectedRootKey.value}' and replace them with this structure. This cannot be undone. Continue?`)) {
            return;
        }
    }

    isProcessing.value = true
    const target = selectedRootKey.value
    const actionLabel = simulate ? 'VALIDATING' : (replaceExisting.value ? 'REPLACING' : 'MERGING')
    
    logs.value = [`➜ [${target}] ${actionLabel}...`]

    try {
        const resultText = await CategoryService.importToRoot(
            target, 
            editorContent.value, 
            simulate, 
            replaceExisting.value
        )

        const resultLines = (resultText as any).split('\n')
        logs.value.push(...resultLines)

        if (simulate) {
            push.success("Validation Successful")
            logs.value.push("✅ Syntax is valid.")
        } else {
            push.success("Import Completed")
            logs.value.push("✅ Database updated.")
            successfulImports.value.add(target)
            
            // Invalidate centralized key
            await queryClient.invalidateQueries({ 
                queryKey: queryKeys.category.roots,
                refetchType: 'all'
            })
            // Also invalidate specific tree
            await queryClient.invalidateQueries({
                queryKey: queryKeys.category.tree(target)
            })
            
            queryClient.invalidateQueries({ queryKey: queryKeys.context.all })
            logs.value.push("➜ Cache invalidated. Trees will refresh.")
        }

    } catch (e: any) {
        if (e.errorCode === 'VAL_100') {
             push.error("Validation Failed");
             logs.value.push(`❌ Validation Errors:\n${e.message}`)
        } else if (e.errorCode === 'PROC_101' || e.status === 409) {
             push.error("Import Blocked");
             logs.value.push(`❌ Target not empty. Enable 'Replace' to overwrite.`);
        } else {
             push.error("Import Failed");
             logs.value.push(`❌ Error: ${e.message}`);
        }
    } finally {
        isProcessing.value = false
    }
}

const refreshTemplates = () => {
    refetchRepos()
    if (selectedRepoId.value) refetchProposals()
}
</script>

<template>
  <div class="h-full flex flex-col bg-muted/30 transition-colors">
      <!-- Toolbar -->
      <div class="p-2 border-b bg-background shrink-0 h-[50px] relative z-40 flex justify-between items-center">
          
          <div class="flex items-center gap-2 text-xs font-bold text-muted-foreground">
              <FileText :size="16" class="text-primary"/>
              <span>Import Target:</span>
              <span v-if="selectedRootKey" class="px-2 py-0.5 rounded text-[10px] uppercase font-bold border transition-colors duration-300" :style="headerStyle">{{ selectedRootKey }}</span>
              <span v-else class="text-muted-foreground italic">None selected</span>
          </div>
          
          <div class="flex gap-2 items-center" v-if="selectedRootKey">
             
             <!-- Template Loader -->
             <div class="relative z-[100]">
                 <BaseButton variant="outline" size="sm" @click="isTemplateMenuOpen = !isTemplateMenuOpen" :disabled="isLoadingProposals || isLoadingRepos || isProcessing">
                    <DownloadCloud :size="14" class="mr-1.5" />
                    <span>{{ (isLoadingProposals || isLoadingRepos) ? 'Loading...' : 'Load Template' }}</span>
                    <ChevronDown :size="12" class="ml-1" />
                 </BaseButton>
                 
                 <!-- Dropdown Menu -->
                 <div v-if="isTemplateMenuOpen" class="absolute right-0 top-full mt-1 w-80 bg-popover border shadow-xl rounded-md overflow-hidden animate-in fade-in zoom-in-95 duration-100 z-[101]">
                     <div class="bg-muted p-2 text-[10px] font-bold text-muted-foreground uppercase border-b flex justify-between items-center">
                        <span>Select Manifest</span>
                        <button @click="refreshTemplates" class="text-primary hover:bg-primary/10 p-1 rounded transition-colors" title="Refresh"><RefreshCw :size="12"/></button>
                     </div>
                     <div class="max-h-72 overflow-y-auto">
                         <div v-if="isLoadingRepos || isLoadingProposals" class="p-4 flex justify-center text-primary"><BaseButton loading variant="ghost" /></div>
                         <div v-else-if="!proposals?.length" class="px-4 py-3 text-xs text-muted-foreground italic text-center">No templates found.</div>
                         <div v-else>
                             <div v-for="tpl in proposals" :key="tpl.id" class="px-3 py-2 border-b last:border-0 hover:bg-muted/50 transition-colors">
                                 <div class="text-xs font-bold text-foreground mb-1 flex items-center justify-between"><span>{{ tpl.name }}</span></div>
                                 <div v-if="tpl.description" class="text-[10px] text-muted-foreground mb-2 leading-tight">{{ tpl.description }}</div>
                                 <div class="flex flex-wrap gap-1.5 mt-1">
                                     <button v-for="lang in (tpl.availableLanguages?.length ? tpl.availableLanguages : ['default'])" :key="lang" @click="loadTemplate(tpl.id, tpl.name, lang === 'default' ? undefined : lang)" class="flex items-center gap-1 text-[10px] uppercase font-bold text-muted-foreground bg-background border px-2 py-0.5 rounded hover:border-primary hover:text-primary hover:bg-primary/5 transition-all shadow-sm"><Globe :size="10" /> {{ lang }}</button>
                                 </div>
                             </div>
                         </div>
                     </div>
                 </div>
                 <!-- Backdrop -->
                 <div v-if="isTemplateMenuOpen" @click="isTemplateMenuOpen = false" class="fixed inset-0 z-[-1] bg-black/5" style="width: 200vw; height: 200vh; left: -100vw; top: -100vh;"></div>
             </div>

             <div class="w-px h-6 bg-border mx-1"></div>
             
             <BaseButton variant="ghost" size="iconSm" @click="performFormat" :disabled="isLocked || isProcessing" title="Pretty Print (Format)"><AlignLeft :size="14"/></BaseButton>
             <BaseButton variant="ghost" size="iconSm" @click="clearEditor" :disabled="isLocked || isProcessing" title="Clear Editor"><RotateCcw :size="14"/></BaseButton>
             
             <!-- Replace Option -->
             <div class="flex items-center gap-2 border border-destructive/30 bg-destructive/10 rounded px-2 py-1 ml-1" :class="{'opacity-50 pointer-events-none': isLocked}" title="If checked, existing categories will be DELETED before import.">
                 <input type="checkbox" id="chkReplace" v-model="replaceExisting" class="h-3 w-3 text-destructive focus:ring-destructive border-destructive rounded cursor-pointer" />
                 <label for="chkReplace" class="text-[10px] text-destructive font-bold cursor-pointer select-none flex items-center gap-1">
                    <AlertTriangle v-if="replaceExisting" :size="10" /> Replace
                 </label>
             </div>

             <BaseButton variant="warning" size="sm" @click="executeProcess(true)" :disabled="isProcessing || !editorContent || isLocked" title="Verify YAML syntax">
                <ShieldCheck :size="14" class="mr-1.5" /> Validate
             </BaseButton>

             <BaseButton variant="default" size="sm" @click="executeProcess(false)" :disabled="isProcessing || !selectedRootKey || isLocked" class="min-w-[90px]">
                <template v-if="isLocked"><CheckCircle2 :size="14" class="mr-1.5" /> Completed</template>
                <template v-else-if="isProcessing"><RefreshCw :size="14" class="animate-spin mr-1.5" /> Processing</template>
                <template v-else><Play :size="14" class="mr-1.5" /> Import</template>
             </BaseButton>
          </div>
      </div>

      <div class="flex-1 flex flex-col min-h-0 relative z-0 p-4">
          <div v-if="isLocked" class="absolute inset-0 z-20 bg-background/80 flex flex-col items-center justify-center text-muted-foreground select-none backdrop-blur-[1px]">
              <Lock :size="32" class="mb-2 text-success" />
              <h3 class="text-sm font-bold text-foreground">Import Completed</h3>
              <p class="text-xs">Category tree for {{ selectedRootKey }} is updated.</p>
          </div>
          
          <IndentEditor 
            ref="editorRef"
            v-model="editorContent" 
            :disabled="!selectedRootKey || isLocked || isProcessing" 
            :placeholder="selectedRootKey ? '# YAML Content for ' + selectedRootKey + '\n- Category:\n    - Subcategory' : 'Select a dimension on the left...'" 
          />
      </div>

      <div class="h-1/3 border-t bg-[#1e1e1e] p-3 overflow-y-auto font-mono text-[11px] text-gray-300 shrink-0">
          <div v-if="!logs.length" class="opacity-40 italic">{{ selectedRootKey ? 'Ready. Load a template or paste YAML.' : 'Waiting for selection...' }}</div>
          <div v-for="(log, i) in logs" :key="i" class="whitespace-pre-wrap mb-0.5" :class="{'text-green-400 font-bold': log.includes('Done') || log.includes('Success') || log.includes('Completed') || log.includes('valid'), 'text-red-400 font-bold': log.includes('Error') || log.includes('Failed'), 'text-yellow-400': log.includes('Tip')}">{{ log }}</div>
      </div>
  </div>
</template>