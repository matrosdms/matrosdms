// AUTO-GENERATED from OpenAPI definitions

export enum EAiOutputFormat {
  MARKDOWN = 'MARKDOWN',
  JSON = 'JSON',
  TEXT = 'TEXT',
}

export const EAiOutputFormatList = [
  EAiOutputFormat.MARKDOWN,
  EAiOutputFormat.JSON,
  EAiOutputFormat.TEXT,
] as const;

export type EAiOutputFormatType = typeof EAiOutputFormatList[number];

export const EAiOutputFormatLabels: Record<EAiOutputFormatType, string> = {
  [EAiOutputFormat.MARKDOWN]: 'Markdown',
  [EAiOutputFormat.JSON]: 'Json',
  [EAiOutputFormat.TEXT]: 'Text',
};
