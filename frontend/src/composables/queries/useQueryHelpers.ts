import { useNetworkStatus } from '@/composables/useNetworkStatus';

export function useQueryHelpers() {
    const { handleQueryError, isBackendDisconnected } = useNetworkStatus();

    /**
     * Wraps an API promise to handle global errors (like 503 Offline) automatically.
     */
    const fetcher = <T>(fn: () => Promise<T>, contextName: string) => async (): Promise<T> => {
        try {
            const result = await fn();
            // If successful, we can assume backend is back online
            if (isBackendDisconnected.value) isBackendDisconnected.value = false;
            return result;
        } catch (error) {
            handleQueryError(error, contextName);
            throw error;
        }
    };

    return { fetcher };
}