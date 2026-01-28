// AUTO-GENERATED from OpenAPI definitions

export enum EPipelineStatus {
  PROCESSING = 'PROCESSING',
  READY = 'READY',
  ERROR = 'ERROR',
  DUPLICATE = 'DUPLICATE',
}

export const EPipelineStatusList = [
  EPipelineStatus.PROCESSING,
  EPipelineStatus.READY,
  EPipelineStatus.ERROR,
  EPipelineStatus.DUPLICATE,
] as const;

export type EPipelineStatusType = typeof EPipelineStatusList[number];

export const EPipelineStatusLabels: Record<EPipelineStatusType, string> = {
  [EPipelineStatus.PROCESSING]: 'Processing',
  [EPipelineStatus.READY]: 'Ready',
  [EPipelineStatus.ERROR]: 'Error',
  [EPipelineStatus.DUPLICATE]: 'Duplicate',
};
