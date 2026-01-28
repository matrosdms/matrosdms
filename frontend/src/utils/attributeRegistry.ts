import type { Component } from 'vue'
import { EAttributeType } from '@/enums'

// Import all available input components
import TextAttrInput from '@/components/attributes/inputs/TextAttrInput.vue'
import NumberAttrInput from '@/components/attributes/inputs/NumberAttrInput.vue'
import BooleanAttrInput from '@/components/attributes/inputs/BooleanAttrInput.vue'
import DateAttrInput from '@/components/attributes/inputs/DateAttrInput.vue'

// Define the mapping using the STRICT Enum as keys
const REGISTRY: Record<string, Component> = {
  [EAttributeType.TEXT]: TextAttrInput,
  [EAttributeType.LINK]: TextAttrInput, // Fallback for now
  [EAttributeType.NUMBER]: NumberAttrInput,
  [EAttributeType.CURRENCY]: NumberAttrInput, // Re-use Number input
  [EAttributeType.BOOLEAN]: BooleanAttrInput,
  [EAttributeType.DATE]: DateAttrInput,
}

// Default fallback
const DEFAULT_COMPONENT = TextAttrInput

/**
 * Returns the specific Vue component for a given attribute data type.
 * @param dataType - The backend type string (e.g., 'TEXT', 'BOOLEAN')
 */
export function getAttributeInputComponent(dataType: string | undefined | null): Component {
  if (!dataType) return DEFAULT_COMPONENT
  
  // Strict check: if the type exists in registry, return it
  if (Object.prototype.hasOwnProperty.call(REGISTRY, dataType)) {
      return REGISTRY[dataType]
  }
  
  // Fallback for case-insensitivity or legacy strings
  const upper = dataType.toUpperCase()
  return REGISTRY[upper] || DEFAULT_COMPONENT
}

/**
 * Returns the initial empty value for a specific data type
 */
export function getAttributeInitialValue(dataType: string | undefined | null): any {
    if (dataType === EAttributeType.BOOLEAN || dataType === 'BOOLEAN') return false
    return ''
}