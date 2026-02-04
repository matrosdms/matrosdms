import createClient from "openapi-fetch";
import type { paths } from "@/types/schema";
import { useUIStore } from "@/stores/ui";
import { useAuthStore } from "@/stores/auth";
import { setBackendDisconnected } from "@/composables/useNetworkStatus"; 
import { push } from "notivue";

export const client = createClient<paths>({ 
  baseUrl: "", 
  headers: {
    "Accept": "application/hal+json, application/json",
  },
});

// Helper to access stores outside of Vue components
let _uiStore: any = null;
let _authStore: any = null;
const getStores = () => {
    // Removed explicit 'pinia' argument to break circular dependency with main.ts
    // Pinia stores automatically use the active instance created in main.ts
    if (!_uiStore) _uiStore = useUIStore();
    if (!_authStore) _authStore = useAuthStore();
    return { ui: _uiStore, auth: _authStore };
}

// Queue for requests waiting for token refresh
let refreshPromise: Promise<boolean> | null = null;

async function refreshAccessToken(): Promise<boolean> {
    const { auth } = getStores();
    
    if (!auth.refreshToken) {
        return false;
    }
    
    try {
        const response = await fetch('/api/auth/refresh', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ refreshToken: auth.refreshToken }),
        });
        
        if (!response.ok) {
            return false;
        }
        
        const data = await response.json();
        if (data.accessToken && data.refreshToken) {
            auth.updateTokens(data.accessToken, data.refreshToken);
            return true;
        }
        return false;
    } catch (e) {
        console.error('[API] Token refresh failed:', e);
        return false;
    }
}

client.use({
  async onRequest({ request }) {
    const { auth } = getStores();
    request.headers.set("X-Requested-With", "XMLHttpRequest");
    if (auth.token) {
        request.headers.set("Authorization", `Bearer ${auth.token}`);
    }
    return request;
  },
  
  async onResponse({ response, request, options }) {
    const { ui, auth } = getStores();
    const url = request.url.replace(window.location.origin, '');
    const status = response.status;
    
    // 1. DETECT OFFLINE BACKEND (503 Service Unavailable)
    if (status === 503 || status === 504) {
        console.warn(`[API] Backend Unavailable (${status})`);
        setBackendDisconnected(true);
        // We do not return here immediately if we want to allow the UI to handle specific 503s, 
        // but usually, the global overlay takes over.
        return response; 
    }

    // 2. AUTH ERRORS - Handle 401 with token refresh
    if (status === 401) {
        // Don't try to refresh for auth endpoints themselves
        if (url.includes("/auth/login") || url.includes("/auth/refresh") || url.includes("/auth/register")) {
            return response;
        }
        
        // If we have a refresh token, try to refresh
        if (auth.refreshToken && auth.isAuthenticated) {
            // Use a single refresh promise to avoid concurrent refresh calls
            if (!refreshPromise) {
                auth.isRefreshing = true;
                refreshPromise = refreshAccessToken().finally(() => {
                    auth.isRefreshing = false;
                    refreshPromise = null;
                });
            }
            
            const refreshed = await refreshPromise;
            
            if (refreshed) {
                // Retry the original request with new token
                const newRequest = new Request(request, {
                    headers: new Headers(request.headers),
                });
                newRequest.headers.set("Authorization", `Bearer ${auth.token}`);
                
                // Make the retry request
                const retryResponse = await fetch(newRequest);
                return retryResponse;
            } else {
                // Refresh failed, logout
                ui.addLog(`!! [AUTH] Session Expired - Token refresh failed`, 'error');
                auth.logout();
            }
        } else if (auth.isAuthenticated) {
            ui.addLog(`!! [AUTH] Session Expired (${status}) - Logging out`, 'error');
            auth.logout();
        }
    }
    else if (status === 403) {
        if (url.includes("/user/") && request.method === 'GET') {
             ui.addLog(`!! [AUTH] User context invalid (403) - Logging out`, 'error');
             auth.logout();
        } else {
             ui.addLog(`!! [AUTH] Access Denied (403) for ${url}`, 'error');
        }
    }
    // 3. CONFLICT (409) - e.g. Concurrent Edits or Duplicate Uploads
    else if (status === 409) {
        // Let the caller handle 409 for specific use cases (e.g., context deletion)
        // We'll still log it but not show a generic toast
        ui.addLog(`!! [CONFLICT] 409 for ${url}`, 'error');
    }
    // 4. VALIDATION ERROR (422) - Business Logic Failure
    else if (status === 422) {
        // Try to parse the message
        try {
            const errData = await response.clone().json(); // Clone because body is read-once
            push.error({
                title: "Validation Failed",
                message: errData.message || "Invalid data submitted.",
                duration: 6000
            });
        } catch (e) {
            push.error("Validation failed (422).");
        }
    }

    return response;
  }
});