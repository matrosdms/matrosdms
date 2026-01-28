// AUTO-GENERATED from OpenAPI definitions

export enum EArchivedState {
  INCLUDEALL = 'INCLUDEALL',
  ONLYACTIVE = 'ONLYACTIVE',
  ONLYARCHIVED = 'ONLYARCHIVED',
}

export const EArchivedStateList = [
  EArchivedState.INCLUDEALL,
  EArchivedState.ONLYACTIVE,
  EArchivedState.ONLYARCHIVED,
] as const;

export type EArchivedStateType = typeof EArchivedStateList[number];

export const EArchivedStateLabels: Record<EArchivedStateType, string> = {
  [EArchivedState.INCLUDEALL]: 'Includeall',
  [EArchivedState.ONLYACTIVE]: 'Onlyactive',
  [EArchivedState.ONLYARCHIVED]: 'Onlyarchived',
};
