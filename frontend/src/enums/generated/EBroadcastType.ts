// AUTO-GENERATED from OpenAPI definitions

export enum EBroadcastType {
  FILE_ADDED = 'FILE_ADDED',
  STATUS = 'STATUS',
  PROGRESS = 'PROGRESS',
  COMPLETE = 'COMPLETE',
  ERROR = 'ERROR',
}

export const EBroadcastTypeList = [
  EBroadcastType.FILE_ADDED,
  EBroadcastType.STATUS,
  EBroadcastType.PROGRESS,
  EBroadcastType.COMPLETE,
  EBroadcastType.ERROR,
] as const;

export type EBroadcastTypeType = typeof EBroadcastTypeList[number];

export const EBroadcastTypeLabels: Record<EBroadcastTypeType, string> = {
  [EBroadcastType.FILE_ADDED]: 'File Added',
  [EBroadcastType.STATUS]: 'Status',
  [EBroadcastType.PROGRESS]: 'Progress',
  [EBroadcastType.COMPLETE]: 'Complete',
  [EBroadcastType.ERROR]: 'Error',
};
