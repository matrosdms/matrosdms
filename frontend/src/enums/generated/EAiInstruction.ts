// AUTO-GENERATED from OpenAPI definitions

export enum EAiInstruction {
  SUMMARY = 'SUMMARY',
  TABLE_EXTRACTION = 'TABLE_EXTRACTION',
  ACTION_ITEMS = 'ACTION_ITEMS',
  PROOFREAD = 'PROOFREAD',
  KEY_FACTS = 'KEY_FACTS',
}

export const EAiInstructionList = [
  EAiInstruction.SUMMARY,
  EAiInstruction.TABLE_EXTRACTION,
  EAiInstruction.ACTION_ITEMS,
  EAiInstruction.PROOFREAD,
  EAiInstruction.KEY_FACTS,
] as const;

export type EAiInstructionType = typeof EAiInstructionList[number];

export const EAiInstructionLabels: Record<EAiInstructionType, string> = {
  [EAiInstruction.SUMMARY]: 'Summary',
  [EAiInstruction.TABLE_EXTRACTION]: 'Table Extraction',
  [EAiInstruction.ACTION_ITEMS]: 'Action Items',
  [EAiInstruction.PROOFREAD]: 'Proofread',
  [EAiInstruction.KEY_FACTS]: 'Key Facts',
};
