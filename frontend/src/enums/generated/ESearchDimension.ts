// AUTO-GENERATED from OpenAPI definitions

export enum ESearchDimension {
  WHO = 'WHO',
  WHAT = 'WHAT',
  WHERE = 'WHERE',
  KIND = 'KIND',
  CONTEXT = 'CONTEXT',
  STORE = 'STORE',
  ISSUE_DATE = 'ISSUE_DATE',
  CREATED = 'CREATED',
  FULLTEXT = 'FULLTEXT',
  ATTRIBUTE = 'ATTRIBUTE',
  SOURCE = 'SOURCE',
  HAS_TEXT = 'HAS_TEXT',
}

export const ESearchDimensionList = [
  ESearchDimension.WHO,
  ESearchDimension.WHAT,
  ESearchDimension.WHERE,
  ESearchDimension.KIND,
  ESearchDimension.CONTEXT,
  ESearchDimension.STORE,
  ESearchDimension.ISSUE_DATE,
  ESearchDimension.CREATED,
  ESearchDimension.FULLTEXT,
  ESearchDimension.ATTRIBUTE,
  ESearchDimension.SOURCE,
  ESearchDimension.HAS_TEXT,
] as const;

export type ESearchDimensionType = typeof ESearchDimensionList[number];

export const ESearchDimensionLabels: Record<ESearchDimensionType, string> = {
  [ESearchDimension.WHO]: 'Who',
  [ESearchDimension.WHAT]: 'What',
  [ESearchDimension.WHERE]: 'Where',
  [ESearchDimension.KIND]: 'Kind',
  [ESearchDimension.CONTEXT]: 'Context',
  [ESearchDimension.STORE]: 'Store',
  [ESearchDimension.ISSUE_DATE]: 'Issue Date',
  [ESearchDimension.CREATED]: 'Created',
  [ESearchDimension.FULLTEXT]: 'Fulltext',
  [ESearchDimension.ATTRIBUTE]: 'Attribute',
  [ESearchDimension.SOURCE]: 'Source',
  [ESearchDimension.HAS_TEXT]: 'Has Text',
};
