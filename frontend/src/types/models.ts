/**
 * Centralized Model Definitions
 * Aliases for OpenAPI generated types to ensure consistency and cleaner imports.
 */
import type { components } from './schema';

// Core Entities
export type Item = components['schemas']['MItem'];
export type Context = components['schemas']['MContext'];
export type Category = components['schemas']['MCategory'];
export type Action = components['schemas']['MAction'];
export type User = components['schemas']['MUser'];
export type Store = components['schemas']['MStore'];
export type AttributeType = components['schemas']['MAttributeType'];

// Data Transfer Objects
export type InboxFile = components['schemas']['InboxFile'];

// Search
export type SearchResult = components['schemas']['MSearchResult'];

// Union for Generic Components (like DetailPane)
export type AnyEntity = Item | User | Store | AttributeType;
