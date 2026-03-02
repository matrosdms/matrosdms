import { ref, computed, watch, onMounted } from 'vue'
import { useDmsStore } from '@/stores/dms'
import { useAuthStore } from '@/stores/auth'
import { useWorkflowStore } from '@/stores/workflow'
import { useQueryClient, useQuery } from '@tanstack/vue-query'
import { ItemService } from '@/services/ItemService'
import { ActionService } from '@/services/ActionService'
import { AttributeTypeService } from '@/services/AttributeTypeService'
import { CategoryService } from '@/services/CategoryService'
import { ItemMapper } from '@/api/mappers/ItemMapper'
import { push } from 'notivue'
import { EStage, type EStageType, EActionPriority, ERootCategory } from '@/enums'
import { formatDateForInput } from '@/lib/utils'
import { queryKeys } from '@/composables/queries/queryKeys'

/** Per-field AI suggestion with confidence for diff display */
export interface AiFieldProposal {
    value: string;
    displayValue?: string; // human-readable label
    confidence: number;    // 0.0 – 1.0
}

export interface AiProposalSnapshot {
    name?: AiFieldProposal;
    date?: AiFieldProposal;
    kind?: AiFieldProposal;
    context?: AiFieldProposal;
    store?: AiFieldProposal;
    strategyId?: string;
    overallConfidence?: number;
}

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

    const aiHighlights = ref<Record<string, boolean>>({ name: false, date: false, category: false, context: false, store: false })
    const aiProposal = ref<AiProposalSnapshot>({})
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

    const { data: kindTree } = useQuery({
        queryKey: ['category', ERootCategory.KIND, true],
        queryFn: () => CategoryService.getTree(ERootCategory.KIND),
        staleTime: 10 * 60 * 1000
    })

    /** Resolve a KIND UUID to its display name using the cached tree */
    const resolveKindName = (uuid: string): string => {
        const findInTree = (node: any): string | null => {
            if (!node) return null
            if (node.uuid === uuid) return node.name
            for (const child of (node.children || [])) {
                const found = findInTree(child)
                if (found) return found
            }
            return null
        }
        return findInTree(kindTree.value) || uuid
    }

    const applyPrediction = (p: any) => {
        if (!p) return

        const fieldConf: Record<string, number> = p.fieldConfidences || {}
        const overall = p.confidence ?? 0
        const snapshot: AiProposalSnapshot = {
            strategyId: p.strategyId,
            overallConfidence: overall
        }

        // 1. Name / Summary
        const desc = p.summary || p.predictedDescription
        if (desc) {
            snapshot.name = { value: desc, confidence: fieldConf.summary ?? overall }
            if (!form.value.name) {
                form.value.name = desc
                aiHighlights.value.name = true
            }
        }

        // 2. Issue Date
        const date = p.documentDate || p.predictedDate
        if (date) {
            snapshot.date = { value: date, confidence: fieldConf.documentDate ?? overall }
            if (!form.value.issueDate) {
                form.value.issueDate = date
                aiHighlights.value.date = true
            }
        }

        // 3. Kind (document type) — backend field is p.kind
        const kindUuid = p.kind || p.predictedCategory
        if (kindUuid) {
            const kindName = resolveKindName(kindUuid)
            snapshot.kind = { value: kindUuid, displayValue: kindName, confidence: fieldConf.kind ?? overall }
            if (!form.value.kindId) {
                form.value.kindId = kindUuid
                form.value.kindName = kindName
                aiHighlights.value.category = true
            }
        }

        // 4. Context — just flag for highlight (ItemFormFields reads from dms.targetContextForDrop)
        const ctx = p.context || p.predictedContext
        if (ctx) {
            snapshot.context = { value: ctx, confidence: fieldConf.context ?? overall }
            aiHighlights.value.context = true
        }

        // 5. Store
        const store = p.store || p.predictedStore
        if (store) {
            snapshot.store = { value: store, confidence: fieldConf.store ?? overall }
            if (!form.value.storeId) {
                form.value.storeId = store
                aiHighlights.value.store = true
            }
        }

        // 6. Attributes
        if (p.attributes && attributeTypes.value) {
            const mappedAttrs: any[] = []
            Object.entries(p.attributes).forEach(([key, val]) => {
                const def = attributeTypes.value?.find((t: any) =>
                    t.key === key || t.uuid === key
                )
                if (def) {
                    mappedAttrs.push({
                        definitionId: (def as any).uuid,
                        typeKey: (def as any).key || (def as any).uuid,
                        name: (def as any).name,
                        value: val
                    })
                }
            })
            if (mappedAttrs.length > 0) form.value.attributes = mappedAttrs
        }

        aiProposal.value = snapshot
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
        aiProposal,
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