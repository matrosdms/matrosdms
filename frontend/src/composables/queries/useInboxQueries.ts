import { useQuery, useQueryClient, type QueryClient } from '@tanstack/vue-query';
import { computed } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useQueryHelpers } from './useQueryHelpers';
import { queryKeys } from './queryKeys';
import { InboxService } from '@/services/InboxService';
import type { InboxFile } from '@/types/events';

/**
 * Timestamp of the last cache update per file hash.
 * Used by the inbox watchdog to detect stale PROCESSING files.
 */
export const inboxUpdateTimes: Record<string, number> = {};

/**
 * Upserts a file into the ['inbox'] query cache by sha256.
 * Deep-merges fileInfo/emailInfo/prediction so partial SSE updates never lose metadata.
 * Creates the cache entry if the inbox query has not been fetched yet.
 */
export function upsertInboxFile(queryClient: QueryClient, file: Partial<InboxFile>) {
    if (!file.sha256) return;
    const hash = file.sha256;

    queryClient.setQueryData<InboxFile[]>(queryKeys.inbox.list, (old) => {
        const list = old || [];
        const existing = list.find(f => f.sha256 === hash) || ({} as InboxFile);

        // Merge Strategy: Overlay new fields onto existing state, preserving metadata
        const merged = {
            ...existing,
            ...file,
            fileInfo: { ...(existing.fileInfo || {}), ...(file.fileInfo || {}) },
            emailInfo: { ...(existing.emailInfo || {}), ...(file.emailInfo || {}) },
            prediction: { ...(existing.prediction || {}), ...(file.prediction || {}) }
        } as InboxFile;

        return list.some(f => f.sha256 === hash)
            ? list.map(f => (f.sha256 === hash ? merged : f))
            : [...list, merged];
    });

    inboxUpdateTimes[hash] = Date.now();
}

/** Removes a file from the ['inbox'] query cache by sha256. */
export function removeInboxFile(queryClient: QueryClient, hash: string) {
    queryClient.setQueryData<InboxFile[]>(queryKeys.inbox.list, (old) =>
        old ? old.filter(f => f.sha256 !== hash) : old
    );
    delete inboxUpdateTimes[hash];
}

export function useInboxQueries() {
    const auth = useAuthStore();
    const { fetcher } = useQueryHelpers();
    const queryClient = useQueryClient();

    const { data: inboxFiles, isLoading: isLoadingInbox, refetch: refetchInbox } = useQuery({
        queryKey: queryKeys.inbox.list,
        // Direct Service Call; client-only manual context assignments survive refetches
        queryFn: fetcher(async () => {
            const fresh = await InboxService.getAll();
            const prev = queryClient.getQueryData<InboxFile[]>(queryKeys.inbox.list);
            if (!prev?.length) return fresh;
            return fresh.map((f) => {
                const old = prev.find(p => p.sha256 === f.sha256);
                if (old?.prediction?.manuallyAssigned) {
                    return { ...f, prediction: { ...(f.prediction || {}), ...old.prediction } };
                }
                return f;
            });
        }, 'inbox-list'),
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
