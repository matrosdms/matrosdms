// AUTO-GENERATED from OpenAPI definitions

export enum EAttributeType {
  TEXT = 'TEXT',
  BOOLEAN = 'BOOLEAN',
  DATE = 'DATE',
  NUMBER = 'NUMBER',
  CURRENCY = 'CURRENCY',
  LINK = 'LINK',
}

export const EAttributeTypeList = [
  EAttributeType.TEXT,
  EAttributeType.BOOLEAN,
  EAttributeType.DATE,
  EAttributeType.NUMBER,
  EAttributeType.CURRENCY,
  EAttributeType.LINK,
] as const;

export type EAttributeTypeType = typeof EAttributeTypeList[number];

export const EAttributeTypeLabels: Record<EAttributeTypeType, string> = {
  [EAttributeType.TEXT]: 'Text',
  [EAttributeType.BOOLEAN]: 'Boolean',
  [EAttributeType.DATE]: 'Date',
  [EAttributeType.NUMBER]: 'Number',
  [EAttributeType.CURRENCY]: 'Currency',
  [EAttributeType.LINK]: 'Link',
};
