/**
 * Single source of truth for TanStack Query Keys.
 * Ensures cache invalidation works predictably across the app.
 */
import type { ERootCategoryType, EJobStatusType, EArchivedState } from '@/enums';

export const queryKeys = {
  // Core Domain
  context: {
    all: ['contexts'] as const,
    detail: (id: string) => ['contexts', id] as const,
  },
  category: {
    tree: (id: string | ERootCategoryType) => ['category', 'tree', id] as const,
    roots: ['category', 'roots'] as const,
  },
  items: {
    all: ['items'] as const,
    byContext: (contextId: string) => ['items', 'context', contextId] as const,
    detail: (uuid: string) => ['items', 'detail', uuid] as const,
  },
  inbox: {
    list: ['inbox'] as const,
  },
  actions: {
    list: (filters: any) => ['actions', filters] as const,
    byItem: (itemId: string) => ['actions', 'item', itemId] as const,
  },
  
  // Admin / Master Data
  admin: {
    stores: ['stores'] as const,
    users: ['users'] as const,
    attributes: ['attribute-types'] as const,
    jobs: (status: EJobStatusType | 'ALL') => ['jobs', status] as const,
  },
  
  // System
  system: {
    version: ['system', 'version'] as const,
  }
};