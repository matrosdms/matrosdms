import { useContextQueries } from '@/composables/queries/useContextQueries';
import { useItemQueries } from '@/composables/queries/useItemQueries';
import { useInboxQueries } from '@/composables/queries/useInboxQueries';
import { useNetworkStatus } from '@/composables/useNetworkStatus';

/**
 * @deprecated Legacy Façade.
 * Please import specific composables from @/composables/queries/*
 */
export function useMatrosData() {
    const { contexts, isLoadingContexts, useCategoryTree, createCategory, isCreatingCategory } = useContextQueries();
    const { useItemsForContext } = useItemQueries();
    const { inboxFiles, isLoadingInbox, refetchInbox } = useInboxQueries();
    const { isBackendDisconnected, checkConnection, handleQueryError } = useNetworkStatus();

    return {
        // Contexts & Categories
        useCategoryTree,
        contexts,
        isLoadingContexts,
        refetchContexts: () => { /* no-op in v2 architecture, reactively handled */ },
        createCategory,
        isCreatingCategory,

        // Items
        useItemsForContext,

        // Inbox
        inboxFiles,
        isLoadingInbox,
        refetchInbox,

        // Network
        isBackendDisconnected,
        checkConnection,
        handleQueryError
    };
}