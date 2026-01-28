import { ERootCategory } from '@/enums'

/**
 * CORE SYSTEM PRESETS
 * These ensure the main dimensions always look familiar/consistent.
 */
const PRESETS: Record<string, { bg: string, text: string, border: string }> = {
    [ERootCategory.WHO]:   { bg: '#eff6ff', text: '#1d4ed8', border: '#93c5fd' }, // Blue
    [ERootCategory.WHAT]:  { bg: '#f0fdf4', text: '#15803d', border: '#86efac' }, // Green
    [ERootCategory.WHERE]: { bg: '#fff7ed', text: '#c2410c', border: '#fdba74' }, // Orange
    [ERootCategory.KIND]:  { bg: '#faf5ff', text: '#7e22ce', border: '#d8b4fe' }  // Purple
};

/**
 * Generate a consistent HSL color from any string (UUID or Key).
 * Keeps Saturation and Lightness constant for a "Pastel" aesthetic.
 */
function generateDynamicColor(key: string) {
    let hash = 0;
    for (let i = 0; i < key.length; i++) {
        hash = key.charCodeAt(i) + ((hash << 5) - hash);
    }
    const h = Math.abs(hash) % 360;
    
    return {
        bg: `hsl(${h}, 90%, 96%)`,
        text: `hsl(${h}, 70%, 30%)`,
        border: `hsl(${h}, 60%, 80%)`
    };
}

export function getTagStyle(key: string) {
    if (!key) return {};
    const normalized = key.toUpperCase();
    
    // 1. Use Preset if available
    if (PRESETS[normalized]) {
        return {
            backgroundColor: PRESETS[normalized].bg,
            color: PRESETS[normalized].text,
            borderColor: PRESETS[normalized].border
        };
    }

    // 2. Dynamic Generation
    const dynamic = generateDynamicColor(key);
    return {
        backgroundColor: dynamic.bg,
        color: dynamic.text,
        borderColor: dynamic.border
    };
}

/**
 * Legacy Class Helper - Deprecated but kept for compatibility.
 * Now returns a generic base class, as colors are handled via style attribute.
 */
export function getTagClassByKey(key: string) {
    return 'border transition-colors duration-200'; 
}