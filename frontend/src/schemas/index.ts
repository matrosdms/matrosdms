import { z } from 'zod'
import { EStage, EAttributeType } from '@/enums'

/**
 * Zod Schemas for Runtime Validation
 * Ensures data from the API matches our expectations before the UI tries to render it.
 */

// --- PRIMITIVES ---
const LifecycleSchema = z.object({
  dateCreated: z.string().optional(),
  dateUpdated: z.string().optional(),
})

const TagSchema = z.object({
  uuid: z.string(),
  name: z.string(),
  icon: z.string().optional().nullable(),
})

// --- ATTRIBUTES ---
export const AttributeSchema = z.object({
  uuid: z.string(),
  name: z.string(),
  value: z.any().optional(), // Flexible for now, can be strict later
  type: z.nativeEnum(EAttributeType).optional(),
})

// --- CORE ENTITIES ---

export const ContextSchema = z.object({
  uuid: z.string(),
  name: z.string(),
  icon: z.string().optional().nullable(),
  stage: z.nativeEnum(EStage).optional(),
  itemCount: z.number().optional(),
})

export const ItemSchema = z.object({
  uuid: z.string(),
  name: z.string(),
  description: z.string().optional().nullable(),
  icon: z.string().optional().nullable(),
  
  // Dates
  issueDate: z.union([z.string(), z.array(z.number())]).optional().nullable(),
  dateExpire: z.union([z.string(), z.array(z.number())]).optional().nullable(),
  
  // Relations
  storeIdentifier: z.string().optional().nullable(),
  storeItemNumber: z.string().optional().nullable(),
  context: ContextSchema.optional().nullable(),
  
  // Lists
  kindList: z.array(TagSchema).optional().default([]),
  attributeList: z.array(AttributeSchema).optional().default([]),
  
  // Metadata
  stage: z.nativeEnum(EStage).optional().default(EStage.ACTIVE),
  version: z.number().optional().default(0),
})

export const ItemListSchema = z.array(ItemSchema)