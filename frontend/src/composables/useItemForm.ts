import { ref, computed, watch, onMounted } from 'vue'
import { useDmsStore } from '@/stores/dms'
import { useAuthStore } from '@/stores/auth'
import { useWorkflowStore } from '@/stores/workflow'
import { useQueryClient, useQuery } from '@tanstack/vue-query'
import { ItemService } from '@/services/ItemService'
import { ActionService } from '@/services/ActionService'
import { AttributeTypeService } from '@/services/AttributeTypeService'
import { ItemMapper } from '@/api/mappers/ItemMapper'
import { push } from 'notivue'
import { EStage, type EStageType, EActionPriority } from '@/enums'
import { formatDateForInput } from '@/lib/utils'
import { queryKeys } from '@/composables/queries/queryKeys'

export interface ItemFormData {
    uuid?: string;
    name: string;
    description: string;
    issueDate: string;
    dateExpire: string;
    kindId: string;
    kindName: string;
    storeId: string;
    storeItemNumber: string;
    icon: string;
    stage: EStageType;
    attributes: any[];
    version: number;
}

export function useItemForm(isEdit: boolean) {
    const dms = useDmsStore()
    const auth = useAuthStore()
    const workflow = useWorkflowStore()
    const queryClient = useQueryClient()

    const form = ref<ItemFormData>({ 
        uuid: undefined,
        name: '', 
        description: '', 
        issueDate: '', 
        dateExpire: '', 
        kindId: '', 
        kindName: '', 
        storeId: '', 
        storeItemNumber: '',
        icon: '',
        stage: EStage.ACTIVE, 
        attributes:[], 
        version: 0 
    })

    const hasReminder = ref(false)
    const reminderForm = ref({
        name: '',
        dueDate: '',
        assigneeIdentifier: auth.currentUser?.uuid || ''
    })

    watch(() => form.value.name, (val) => {
        if (!isEdit && val && !reminderForm.value.name) {
            reminderForm.value.name = `Review: ${val}`
        }
    })

    const aiHighlights = ref({ name: false, date: false, category: false, context: false, store: false })
    const touched = ref(false)
    const isLoading = ref(false)

    const file = computed(() => {
        const f = dms.pendingInboxFile
        if (f && f.sha256 && workflow.liveInboxFiles[f.sha256]) {
            return { ...f, ...workflow.liveInboxFiles[f.sha256] }
        }
        return f
    })

    const context = computed(() => isEdit ? dms.selectedContext : dms.targetContextForDrop)
    
    const rawPrediction = computed(() => {
        return dms.pendingInboxFile?.prediction || null
    })
    
    const { data: attributeTypes } = useQuery({
        queryKey: ['attribute-types'],
        queryFn: AttributeTypeService.getAll,
        staleTime: 5 * 60 * 1000
    })

    const applyPrediction = (p: any) => {
        if (!p) return

        // 1. Standard Fields
        const desc = p.summary || p.predictedDescription
        if (desc && !form.value.name) {
            form.value.name = desc
            aiHighlights.value.name = true
        }
        
        const date = p.documentDate || p.predictedDate
        if (date && !form.value.issueDate) {
            form.value.issueDate = date
            aiHighlights.value.date = true
        }
        
        const cat = p.category || p.predictedCategory
        if (cat && !form.value.kindId) {
            form.value.kindId = cat
            form.value.kindName = 'AI Suggested' 
            aiHighlights.value.category = true
        }
        
        const ctx = p.context || p.predictedContext
        if (ctx) {
            aiHighlights.value.context = true
        }
        
        const store = p.store || p.predictedStore
        if (store && !form.value.storeId) {
            form.value.storeId = store
            aiHighlights.value.store = true
        }
        
        // 2. Attributes Processing (Strict 'attributes' key)
        if (p.attributes && attributeTypes.value) {
            const mappedAttrs: any[] = [];
            
            Object.entries(p.attributes).forEach(([key, val]) => {
                // Map to Attribute Definition (Key -> UUID)
                const def = attributeTypes.value?.find(t => 
                    (t as any).key === key || t.uuid === key
                );

                if (def) {
                    mappedAttrs.push({
                        definitionId: def.uuid,
                        typeKey: (def as any).key || def.uuid,
                        name: def.name,
                        value: val
                    })
                }
            });
            
            if (mappedAttrs.length > 0) {
                form.value.attributes = mappedAttrs;
            }
        }
    }

    // Reactively re-apply if attribute definitions load later
    watch(attributeTypes, (val) => {
        if (val && !isEdit && dms.pendingInboxFile?.prediction) {
            applyPrediction(dms.pendingInboxFile.prediction)
        }
    })

    // Reactively fill form from file as soon as it is available (or when live data arrives).
    // AI prediction has priority; original filename is the fallback.
    watch(file, (f) => {
        if (!isEdit && f && !form.value.name) {
            if (f.prediction) applyPrediction(f.prediction)
            if (!form.value.name) {
                form.value.name = f.fileInfo?.originalFilename || f.displayName || ''
            }
        }
    }, { immediate: true })

    onMounted(() => {
        if (isEdit && dms.selectedItem) {
            const item = dms.selectedItem
            const k = item.kindList && item.kindList.length > 0 ? item.kindList[0] : null
            
            form.value = { 
                uuid: item.uuid,
                name: item.name || '', 
                description: item.description || '', 
                issueDate: formatDateForInput(item.issueDate),
                dateExpire: formatDateForInput(item.dateExpire),
                kindId: k?.uuid || '',
                kindName: k?.name || '',
                storeId: item.storeIdentifier || '',
                storeItemNumber: item.storeItemNumber || '',
                icon: item.icon || '', 
                stage: (item.stage as EStageType) || EStage.ACTIVE,
                version: item.version || 0,
                attributes: item.attributeList ? item.attributeList.map((attr: any) => {
                     let rawValue = attr.value;
                     if (rawValue && typeof rawValue === 'object' && 'value' in rawValue) {
                         rawValue = rawValue.value;
                     }
                     return {
                         uuid: attr.uuid, 
                         definitionId: attr.uuid, 
                         typeKey: attr.uuid, 
                         name: attr.name,
                         value: rawValue || '' 
                     }
                }) :[]
            }
        }
    })

    const save = async () => {
        if (!form.value.name) { touched.value = true; return }
        
        isLoading.value = true
        try {
            let itemUuid = '';

            if (isEdit) {
                if(!dms.selectedItem?.uuid) return
                itemUuid = dms.selectedItem.uuid;
                
                const payload = ItemMapper.toUpdatePayload(
                    form.value,
                    dms.selectedContext?.uuid || dms.selectedItem.context?.uuid,
                    auth.currentUser?.uuid
                )
                payload.version = form.value.version
                await ItemService.update(itemUuid, payload)
                const freshItem = await ItemService.getById(itemUuid)
                dms.setSelectedItem(freshItem)
                push.success('Document updated')
            } else {
                if (!context.value?.uuid) throw new Error("Target context is missing")
                if (!auth.currentUser?.uuid) throw new Error("User session invalid")
                
                const fileHash = file.value?.sha256 || '';
                
                if (!fileHash) {
                    throw new Error("File Hash is missing. Cannot create document link.");
                }
                
                const payload = ItemMapper.toCreatePayload(
                    form.value, 
                    context.value.uuid, 
                    auth.currentUser.uuid, 
                    fileHash
                )
                
                const createdItem = await ItemService.create(payload)
                itemUuid = createdItem.uuid!;
                
                // Remove from live list immediately
                if (fileHash) {
                    workflow.removeLiveFile(fileHash);
                }
                
                push.success('Document assigned')
            }
            
            if (hasReminder.value && reminderForm.value.name && itemUuid) {
                await ActionService.create({
                    name: reminderForm.value.name,
                    description: `Follow-up for document: ${form.value.name}`,
                    priority: EActionPriority.NORMAL,
                    dueDate: reminderForm.value.dueDate ? new Date(reminderForm.value.dueDate).toISOString() : undefined,
                    assigneeIdentifier: reminderForm.value.assigneeIdentifier,
                    itemIdentifier: itemUuid,
                    contextIdentifier: context.value?.uuid
                })
                push.success('Follow-up task created')
            }

            queryClient.invalidateQueries({ queryKey: queryKeys.inbox.list })
            queryClient.invalidateQueries({ queryKey: ['items'] })
            queryClient.invalidateQueries({ queryKey: ['contexts'] })
            dms.cancelCreation()
        } catch (e: any) {
            if (e.errorCode === 'DB_LOCK_CONCURRENT') {
                push.error("Critical: Data modified by another user. Reloading...")
                dms.cancelCreation(); 
            } else if (e.errorCode === 'PROC_101') {
                push.error("Duplicate file prevented saving.")
            } else {
                const msg = e.message || 'Unknown Error'
                if(!msg.includes('409')) push.error('Failed: ' + msg)
            }
        } finally {
            isLoading.value = false
        }
    }

    return { 
        form, 
        aiHighlights, 
        touched, 
        isLoading, 
        save, 
        file, 
        hasReminder, 
        reminderForm,
        applyPrediction,
        rawPrediction
    }
}