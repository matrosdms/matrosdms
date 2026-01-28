import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query';
import { computed, unref, type Ref } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useUIStore } from '@/stores/ui';
import { useQueryHelpers } from './useQueryHelpers';
import { queryKeys } from './queryKeys';
import { ContextService } from '@/services/ContextService';
import { CategoryService } from '@/services/CategoryService';
import type { Category, Context } from '@/types/models';
import { ERootCategoryType } from '@/enums';

export function useContextQueries() {
    const auth = useAuthStore();
    const ui = useUIStore();
    const queryClient = useQueryClient();
    const { fetcher } = useQueryHelpers();

    // --- CONTEXTS ---
    const { data: contexts, isLoading: isLoadingContexts } = useQuery({
        queryKey: queryKeys.context.all,
        queryFn: fetcher(() => ContextService.getAll(), 'context-list'),
        enabled: computed(() => auth.isAuthenticated),
        staleTime: 1000 * 60 * 5, // 5 min
    });

    // --- CATEGORY TREE ---
    const useCategoryTree = (identifierRef: Ref<string | ERootCategoryType | undefined | null> | string) => {
        return useQuery<Category | null>({
            queryKey: computed(() => {
                const id = unref(identifierRef);
                return id ? queryKeys.category.tree(id) : ['category', 'disabled'];
            }),
            queryFn: async () => {
                const id = unref(identifierRef);
                if (!id) return null;
                // Direct Service Call via Fetcher Wrapper
                return await fetcher(() => CategoryService.getTree(id), 'category-tree')();
            },
            enabled: computed(() => auth.isAuthenticated && !!unref(identifierRef)),
            staleTime: 1000 * 60 * 60, 
        });
    };

    // --- MUTATIONS ---
    const { mutate: createCategory, isPending: isCreatingCategory } = useMutation({
        mutationFn: async ({ parentId, payload }: { parentId: string, payload: any }) => {
            ui.addLog(`Creating category '${payload.name}'...`, 'info');
            return await CategoryService.create(parentId, payload);
        },
        onSuccess: () => {
            ui.addLog(`Category created.`, 'success');
            queryClient.invalidateQueries({ queryKey: ['category'] });
        },
        onError: (err: any) => ui.addLog(`Failed to create category: ${err.message}`, 'error')
    });

    return {
        contexts: computed(() => (contexts.value || []) as Context[]),
        isLoadingContexts,
        useCategoryTree,
        createCategory,
        isCreatingCategory
    };
}