// AUTO-GENERATED from OpenAPI definitions

export enum EBroadcastSource {
  INBOX = 'INBOX',
  PIPELINE = 'PIPELINE',
  ACTION = 'ACTION',
}

export const EBroadcastSourceList = [
  EBroadcastSource.INBOX,
  EBroadcastSource.PIPELINE,
  EBroadcastSource.ACTION,
] as const;

export type EBroadcastSourceType = typeof EBroadcastSourceList[number];

export const EBroadcastSourceLabels: Record<EBroadcastSourceType, string> = {
  [EBroadcastSource.INBOX]: 'Inbox',
  [EBroadcastSource.PIPELINE]: 'Pipeline',
  [EBroadcastSource.ACTION]: 'Action',
};
