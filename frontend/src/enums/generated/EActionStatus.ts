// AUTO-GENERATED from OpenAPI definitions

export enum EActionStatus {
  OPEN = 'OPEN',
  IN_PROGRESS = 'IN_PROGRESS',
  ON_HOLD = 'ON_HOLD',
  DONE = 'DONE',
  REJECTED = 'REJECTED',
}

export const EActionStatusList = [
  EActionStatus.OPEN,
  EActionStatus.IN_PROGRESS,
  EActionStatus.ON_HOLD,
  EActionStatus.DONE,
  EActionStatus.REJECTED,
] as const;

export type EActionStatusType = typeof EActionStatusList[number];

export const EActionStatusLabels: Record<EActionStatusType, string> = {
  [EActionStatus.OPEN]: 'Open',
  [EActionStatus.IN_PROGRESS]: 'In Progress',
  [EActionStatus.ON_HOLD]: 'On Hold',
  [EActionStatus.DONE]: 'Done',
  [EActionStatus.REJECTED]: 'Rejected',
};
