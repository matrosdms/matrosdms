// AUTO-GENERATED from OpenAPI definitions

export enum EStage {
  ACTIVE = 'ACTIVE',
  ARCHIVED = 'ARCHIVED',
  DELETED = 'DELETED',
}

export const EStageList = [
  EStage.ACTIVE,
  EStage.ARCHIVED,
  EStage.DELETED,
] as const;

export type EStageType = typeof EStageList[number];

export const EStageLabels: Record<EStageType, string> = {
  [EStage.ACTIVE]: 'Active',
  [EStage.ARCHIVED]: 'Archived',
  [EStage.DELETED]: 'Deleted',
};
