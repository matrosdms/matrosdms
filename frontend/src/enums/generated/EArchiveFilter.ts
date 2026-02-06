// AUTO-GENERATED from OpenAPI definitions

export enum EArchiveFilter {
  ALL = 'ALL',
  ACTIVE_ONLY = 'ACTIVE_ONLY',
  ARCHIVED_ONLY = 'ARCHIVED_ONLY',
}

export const EArchiveFilterList = [
  EArchiveFilter.ALL,
  EArchiveFilter.ACTIVE_ONLY,
  EArchiveFilter.ARCHIVED_ONLY,
] as const;

export type EArchiveFilterType = typeof EArchiveFilterList[number];

export const EArchiveFilterLabels: Record<EArchiveFilterType, string> = {
  [EArchiveFilter.ALL]: 'All',
  [EArchiveFilter.ACTIVE_ONLY]: 'Active Only',
  [EArchiveFilter.ARCHIVED_ONLY]: 'Archived Only',
};
