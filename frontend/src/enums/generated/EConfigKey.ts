// AUTO-GENERATED from OpenAPI definitions

export enum EConfigKey {
  PREFERED_LANGUAGE = 'PREFERED_LANGUAGE',
}

export const EConfigKeyList = [
  EConfigKey.PREFERED_LANGUAGE,
] as const;

export type EConfigKeyType = typeof EConfigKeyList[number];

export const EConfigKeyLabels: Record<EConfigKeyType, string> = {
  [EConfigKey.PREFERED_LANGUAGE]: 'Prefered Language',
};
