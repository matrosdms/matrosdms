// AUTO-GENERATED from OpenAPI definitions

export enum EJobType {
  INTEGRITY_CHECK = 'INTEGRITY_CHECK',
  EXPORT_ARCHIVE = 'EXPORT_ARCHIVE',
  REINDEX_SEARCH = 'REINDEX_SEARCH',
}

export const EJobTypeList = [
  EJobType.INTEGRITY_CHECK,
  EJobType.EXPORT_ARCHIVE,
  EJobType.REINDEX_SEARCH,
] as const;

export type EJobTypeType = typeof EJobTypeList[number];

export const EJobTypeLabels: Record<EJobTypeType, string> = {
  [EJobType.INTEGRITY_CHECK]: 'Integrity Check',
  [EJobType.EXPORT_ARCHIVE]: 'Export Archive',
  [EJobType.REINDEX_SEARCH]: 'Reindex Search',
};
