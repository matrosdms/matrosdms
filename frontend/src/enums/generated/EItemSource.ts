// AUTO-GENERATED from OpenAPI definitions

export enum EItemSource {
  UPLOAD = 'UPLOAD',
  EMAIL = 'EMAIL',
  SCAN = 'SCAN',
  API = 'API',
  UNKNOWN = 'UNKNOWN',
}

export const EItemSourceList = [
  EItemSource.UPLOAD,
  EItemSource.EMAIL,
  EItemSource.SCAN,
  EItemSource.API,
  EItemSource.UNKNOWN,
] as const;

export type EItemSourceType = typeof EItemSourceList[number];

export const EItemSourceLabels: Record<EItemSourceType, string> = {
  [EItemSource.UPLOAD]: 'Upload',
  [EItemSource.EMAIL]: 'Email',
  [EItemSource.SCAN]: 'Scan',
  [EItemSource.API]: 'Api',
  [EItemSource.UNKNOWN]: 'Unknown',
};
