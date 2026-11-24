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

const DATE_PARTS: Intl.DateTimeFormatOptions = { day: '2-digit', month: '2-digit', year: 'numeric' }

/**
 * A date the way the browser's locale writes it, but always with a two-digit day and month:
 * "05.01.2026", never "5.1.2026", so dates line up in tables.
 */
export function formatDate(val: any, fallback = '-'): string {
    const d = parseBackendDate(val)
    return d ? d.toLocaleDateString(undefined, DATE_PARTS) : fallback
}

/** Date and time, two-digit throughout: "05.01.2026, 09:04:07". */
export function formatDateTime(val: any, fallback = '-'): string {
    const d = parseBackendDate(val)
    return d ? d.toLocaleString(undefined, { ...DATE_PARTS, hour: '2-digit', minute: '2-digit', second: '2-digit' }) : fallback
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

/**
 * A file name without its extension, for use as a document title: "Rente 2014.pdf" -> "Rente 2014".
 * Only a trailing extension that starts with a letter is removed, so "Rechnung 2024.11" stays as it is.
 */
export function stripExtension(filename: string): string {
    const stripped = filename.replace(/\.[a-z][a-z0-9]{0,4}$/i, '')
    return stripped.trim() ? stripped : filename
}
