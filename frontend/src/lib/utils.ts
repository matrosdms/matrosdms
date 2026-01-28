import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/**
 * Robustly parses a date from various backend formats
 */
export function parseBackendDate(val: any): Date | null {
    if (!val) return null
    try {
        if (Array.isArray(val)) {
            return new Date(
                val[0], 
                val[1] - 1, 
                val[2], 
                val[3] || 0, 
                val[4] || 0, 
                val[5] || 0
            )
        }
        const d = new Date(val)
        if (isNaN(d.getTime())) return null
        return d
    } catch (e) { return null }
}

export function formatDateForInput(val: any): string {
    const date = parseBackendDate(val)
    if (!date) return ''
    const year = date.getFullYear()
    const month = (date.getMonth() + 1).toString().padStart(2, '0')
    const day = date.getDate().toString().padStart(2, '0')
    return `${year}-${month}-${day}`
}

export function getErrorMessage(err: any): string {
    if (!err) return 'An unknown error occurred';
    if (typeof err === 'string') return err;

    // Handle new standardized Error Schema
    let prefix = '';
    if (err.errorCode) {
        prefix = `[${err.errorCode}] `;
    }

    if (err.validationErrors && Array.isArray(err.validationErrors) && err.validationErrors.length > 0) {
        const details = err.validationErrors
            .map((e: any) => `• ${e.field}: ${e.message}`)
            .join('\n');
        return `${prefix}${err.message || 'Validation Failed'}:\n${details}`;
    }

    if (err.message) return `${prefix}${err.message}`;
    if (err.error) return `${prefix}${err.error}`; 

    try {
        return JSON.stringify(err);
    } catch {
        return 'An unknown error occurred';
    }
}

// --- NEW: Centralized API Error Handling ---

export interface APIError extends Error {
    errorCode?: string;
    validationErrors?: any[];
    status?: number;
}

export function createAPIError(error: unknown): APIError {
    const message = getErrorMessage(error);
    const apiError = new Error(message) as APIError;
    
    if (typeof error === 'object' && error !== null) {
        const errObj = error as Record<string, any>;
        apiError.errorCode = errObj.errorCode;
        apiError.validationErrors = errObj.validationErrors;
        apiError.status = errObj.status;
    }
    
    return apiError;
}

export function throwAPIError(error: unknown): never {
    throw createAPIError(error);
}

/**
 * Client-side SHA-256 Hashing
 */
export async function sha256(plain: string): Promise<string> {
    const encoder = new TextEncoder();
    const data = encoder.encode(plain);
    const hashBuffer = await crypto.subtle.digest('SHA-256', data);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
}

export function stringToColor(str: string): string {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
        hash = str.charCodeAt(i) + ((hash << 5) - hash);
    }
    const h = Math.abs(hash) % 360;
    return `hsl(${h}, 70%, 80%)`; 
}

export function stringToBorderColor(str: string): string {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
        hash = str.charCodeAt(i) + ((hash << 5) - hash);
    }
    const h = Math.abs(hash) % 360;
    return `hsl(${h}, 70%, 40%)`; 
}