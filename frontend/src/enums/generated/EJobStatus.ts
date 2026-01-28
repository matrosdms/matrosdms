// AUTO-GENERATED from OpenAPI definitions

export enum EJobStatus {
  QUEUED = 'QUEUED',
  RUNNING = 'RUNNING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
}

export const EJobStatusList = [
  EJobStatus.QUEUED,
  EJobStatus.RUNNING,
  EJobStatus.COMPLETED,
  EJobStatus.FAILED,
] as const;

export type EJobStatusType = typeof EJobStatusList[number];

export const EJobStatusLabels: Record<EJobStatusType, string> = {
  [EJobStatus.QUEUED]: 'Queued',
  [EJobStatus.RUNNING]: 'Running',
  [EJobStatus.COMPLETED]: 'Completed',
  [EJobStatus.FAILED]: 'Failed',
};
