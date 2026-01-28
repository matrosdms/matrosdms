import { useQuery } from '@tanstack/vue-query';
import { computed } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useQueryHelpers } from './useQueryHelpers';
import { queryKeys } from './queryKeys';
import { StoreService } from '@/services/StoreService';
import { UserService } from '@/services/UserService';
import { AttributeTypeService } from '@/services/AttributeTypeService';

export function useAdminQueries() {
    const auth = useAuthStore();
    const { fetcher } = useQueryHelpers();

    const useStores = () => useQuery({
        queryKey: queryKeys.admin.stores,
        queryFn: fetcher(() => StoreService.getAll(), 'stores'),
        enabled: computed(() => auth.isAuthenticated),
        staleTime: 1000 * 60 * 60
    });

    const useUsers = () => useQuery({
        queryKey: queryKeys.admin.users,
        queryFn: fetcher(() => UserService.getAll(), 'users'),
        enabled: computed(() => auth.isAuthenticated)
    });

    const useAttributeTypes = () => useQuery({
        queryKey: queryKeys.admin.attributes,
        queryFn: fetcher(() => AttributeTypeService.getAll(), 'attribute-types'),
        enabled: computed(() => auth.isAuthenticated)
    });

    return { useStores, useUsers, useAttributeTypes };
}