// AUTO-GENERATED from OpenAPI definitions

export enum EUserRole {
  ADMIN = 'ADMIN',
  USER = 'USER',
}

export const EUserRoleList = [
  EUserRole.ADMIN,
  EUserRole.USER,
] as const;

export type EUserRoleType = typeof EUserRoleList[number];

export const EUserRoleLabels: Record<EUserRoleType, string> = {
  [EUserRole.ADMIN]: 'Admin',
  [EUserRole.USER]: 'User',
};
