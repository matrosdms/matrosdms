import createClient from "openapi-fetch";
import type { paths } from "@/types/schema";
import { useUIStore } from "@/stores/ui";
import { useAuthStore } from "@/stores/auth";
import { setBackendDisconnected } from "@/composables/useNetworkStatus"; 
import { pinia } from "@/main"
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
    if (!_uiStore) _uiStore = useUIStore(pinia);
    if (!_authStore) _authStore = useAuthStore(pinia);
    return { ui: _uiStore, auth: _authStore };
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
  
  async onResponse({ response, request }) {
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

    // 2. AUTH ERRORS
    if (status === 401) {
        if (auth.isAuthenticated && !url.includes("/auth/login") && !url.includes("/users")) {
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
        push.error({
            title: "Conflict Detected",
            message: "Data modified by another user or duplicate file. Refreshing...",
            duration: 5000
        });
        ui.addLog(`!! [CONFLICT] 409 Data mismatch`, 'error');
        
        // Auto-refresh logic could go here, for now we let the user react
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