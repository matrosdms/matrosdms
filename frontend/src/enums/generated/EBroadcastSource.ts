// AUTO-GENERATED from OpenAPI definitions

export enum EBroadcastSource {
  INBOX = 'INBOX',
  PIPELINE = 'PIPELINE',
}

export const EBroadcastSourceList = [
  EBroadcastSource.INBOX,
  EBroadcastSource.PIPELINE,
] as const;

export type EBroadcastSourceType = typeof EBroadcastSourceList[number];

export const EBroadcastSourceLabels: Record<EBroadcastSourceType, string> = {
  [EBroadcastSource.INBOX]: 'Inbox',
  [EBroadcastSource.PIPELINE]: 'Pipeline',
};
