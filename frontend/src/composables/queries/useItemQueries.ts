import { useQuery } from '@tanstack/vue-query';
import { computed, unref, type Ref } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useQueryHelpers } from './useQueryHelpers';
import { queryKeys } from './queryKeys';
import { ItemService } from '@/services/ItemService';
import type { Item } from '@/types/models';

export function useItemQueries() {
    const auth = useAuthStore();
    const { fetcher } = useQueryHelpers();

    const useItemsForContext = (contextIdRef: Ref<string | undefined | null>) => {
        return useQuery<Item[]>({
            queryKey: computed(() => {
                const id = unref(contextIdRef);
                return id ? queryKeys.items.byContext(id) : ['items', 'disabled'];
            }),
            queryFn: async () => {
                const id = unref(contextIdRef);
                if (!id) return [];
                // Standardized Service Call
                return fetcher(() => ItemService.getByContext(id), 'items-context')();
            },
            enabled: computed(() => auth.isAuthenticated && !!unref(contextIdRef))
        });
    };

    return {
        useItemsForContext
    };
}