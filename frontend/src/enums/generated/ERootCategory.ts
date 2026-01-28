// AUTO-GENERATED from OpenAPI definitions

export enum ERootCategory {
  WHO = 'WHO',
  WHAT = 'WHAT',
  WHERE = 'WHERE',
  KIND = 'KIND',
}

export const ERootCategoryList = [
  ERootCategory.WHO,
  ERootCategory.WHAT,
  ERootCategory.WHERE,
  ERootCategory.KIND,
] as const;

export type ERootCategoryType = typeof ERootCategoryList[number];

export const ERootCategoryLabels: Record<ERootCategoryType, string> = {
  [ERootCategory.WHO]: 'Who',
  [ERootCategory.WHAT]: 'What',
  [ERootCategory.WHERE]: 'Where',
  [ERootCategory.KIND]: 'Kind',
};
