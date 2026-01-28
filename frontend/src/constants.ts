export const DIMENSIONS = {
  WHO: 'who',
  WHAT: 'what',
  WHERE: 'where',
  KIND: 'kind'
} as const;

export const ROOT_IDS = {
  [DIMENSIONS.WHO]: 'ROOT_WHO',
  [DIMENSIONS.WHAT]: 'ROOT_WHAT',
  [DIMENSIONS.WHERE]: 'ROOT_WHERE',
  [DIMENSIONS.KIND]: 'ROOT_KIND'
} as const;