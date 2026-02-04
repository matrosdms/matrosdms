// AUTO-GENERATED from OpenAPI definitions

export enum EStage {
  ACTIVE = 'ACTIVE',
  CLOSED = 'CLOSED',
}

export const EStageList = [
  EStage.ACTIVE,
  EStage.CLOSED,
] as const;

export type EStageType = typeof EStageList[number];

export const EStageLabels: Record<EStageType, string> = {
  [EStage.ACTIVE]: 'Active',
  [EStage.CLOSED]: 'Closed',
};
