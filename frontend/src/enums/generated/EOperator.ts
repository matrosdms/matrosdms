// AUTO-GENERATED from OpenAPI definitions

export enum EOperator {
  EQ = 'EQ',
  CONTAINS = 'CONTAINS',
  GT = 'GT',
  LT = 'LT',
  GTE = 'GTE',
  LTE = 'LTE',
}

export const EOperatorList = [
  EOperator.EQ,
  EOperator.CONTAINS,
  EOperator.GT,
  EOperator.LT,
  EOperator.GTE,
  EOperator.LTE,
] as const;

export type EOperatorType = typeof EOperatorList[number];

export const EOperatorLabels: Record<EOperatorType, string> = {
  [EOperator.EQ]: 'Eq',
  [EOperator.CONTAINS]: 'Contains',
  [EOperator.GT]: 'Gt',
  [EOperator.LT]: 'Lt',
  [EOperator.GTE]: 'Gte',
  [EOperator.LTE]: 'Lte',
};
