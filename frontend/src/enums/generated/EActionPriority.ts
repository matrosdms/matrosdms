// AUTO-GENERATED from OpenAPI definitions

export enum EActionPriority {
  LOW = 'LOW',
  NORMAL = 'NORMAL',
  HIGH = 'HIGH',
}

export const EActionPriorityList = [
  EActionPriority.LOW,
  EActionPriority.NORMAL,
  EActionPriority.HIGH,
] as const;

export type EActionPriorityType = typeof EActionPriorityList[number];

export const EActionPriorityLabels: Record<EActionPriorityType, string> = {
  [EActionPriority.LOW]: 'Low',
  [EActionPriority.NORMAL]: 'Normal',
  [EActionPriority.HIGH]: 'High',
};
