import type { components } from './schema';

/** Client-side extension of the generated Prediction type with UI-only properties */
export type ClientPrediction = components['schemas']['Prediction'];

export type InboxFile = Omit<components['schemas']['InboxFile'], 'prediction'> & {
  /** AI prediction extended with client-side properties */
  prediction?: ClientPrediction;
  // UI-only helper property injected during runtime
  displayName?: string;
};

export type ProgressMessage = components['schemas']['ProgressMessage'];
export type PipelineStatusMessage = components['schemas']['PipelineStatusMessage'];
export type BroadcastMessage = components['schemas']['BroadcastMessage'];

/** SSE payload (process ACTION): a task changed outside the UI, e.g. ticked off in Google Tasks. */
export type ActionChangedMessage = {
  actionUuid: string;
  name?: string;
  /** e.g. "Google Tasks" */
  source?: string;
  /** e.g. "Completed in Google Tasks" */
  summary?: string;
};