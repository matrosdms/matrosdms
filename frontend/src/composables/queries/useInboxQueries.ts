import { useQuery, useQueryClient } from '@tanstack/vue-query';
import { computed } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useQueryHelpers } from './useQueryHelpers';
import { queryKeys } from './queryKeys';
import { InboxService } from '@/services/InboxService';

export function useInboxQueries() {
    const auth = useAuthStore();
    const { fetcher } = useQueryHelpers();
    const queryClient = useQueryClient();

    const { data: inboxFiles, isLoading: isLoadingInbox, refetch: refetchInbox } = useQuery({
        queryKey: queryKeys.inbox.list,
        // Direct Service Call
        queryFn: fetcher(() => InboxService.getAll(), 'inbox-list'),
        enabled: computed(() => auth.isAuthenticated),
        staleTime: 1000 * 30, // 30s stale time
    });

    const invalidateInbox = () => {
        queryClient.invalidateQueries({ queryKey: queryKeys.inbox.list });
    };

    return {
        inboxFiles: computed(() => inboxFiles.value || []),
        isLoadingInbox,
        refetchInbox,
        invalidateInbox
    };
}