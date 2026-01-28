// AUTO-GENERATED from OpenAPI definitions

export enum ECategoryScope {
  CONTEXT = 'CONTEXT',
  DOCUMENT = 'DOCUMENT',
  ANY = 'ANY',
}

export const ECategoryScopeList = [
  ECategoryScope.CONTEXT,
  ECategoryScope.DOCUMENT,
  ECategoryScope.ANY,
] as const;

export type ECategoryScopeType = typeof ECategoryScopeList[number];

export const ECategoryScopeLabels: Record<ECategoryScopeType, string> = {
  [ECategoryScope.CONTEXT]: 'Context',
  [ECategoryScope.DOCUMENT]: 'Document',
  [ECategoryScope.ANY]: 'Any',
};
