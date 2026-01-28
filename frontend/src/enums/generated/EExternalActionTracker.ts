// AUTO-GENERATED from OpenAPI definitions

export enum EExternalActionTracker {
  NONE = 'NONE',
  GOOGLE_TASKS = 'GOOGLE_TASKS',
  MICROSOFT_TODO = 'MICROSOFT_TODO',
  JIRA = 'JIRA',
}

export const EExternalActionTrackerList = [
  EExternalActionTracker.NONE,
  EExternalActionTracker.GOOGLE_TASKS,
  EExternalActionTracker.MICROSOFT_TODO,
  EExternalActionTracker.JIRA,
] as const;

export type EExternalActionTrackerType = typeof EExternalActionTrackerList[number];

export const EExternalActionTrackerLabels: Record<EExternalActionTrackerType, string> = {
  [EExternalActionTracker.NONE]: 'None',
  [EExternalActionTracker.GOOGLE_TASKS]: 'Google Tasks',
  [EExternalActionTracker.MICROSOFT_TODO]: 'Microsoft Todo',
  [EExternalActionTracker.JIRA]: 'Jira',
};
