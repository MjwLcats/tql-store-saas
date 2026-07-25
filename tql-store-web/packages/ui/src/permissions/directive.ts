import type { Directive, DirectiveBinding } from 'vue';
import { effectiveMenus, evaluatePermission, useAppStore, type PermissionMatchMode, type PermissionRequirement } from '@tql-store/auth';

export interface PermissionDirectiveValue {
  permission: PermissionRequirement;
  mode?: PermissionMatchMode;
}

function applyPermission(element: HTMLElement, binding: DirectiveBinding<PermissionRequirement | PermissionDirectiveValue>) {
  const store = useAppStore();
  const value = binding.value;
  const permission = typeof value === 'object' && !Array.isArray(value) ? value.permission : value;
  const mode = typeof value === 'object' && !Array.isArray(value) ? value.mode : 'any';
  const granted = effectiveMenus(store.menus)
    .map(menu => menu.permission).filter((item): item is string => Boolean(item));
  element.hidden = !evaluatePermission(granted, permission, mode);
}

export const permissionDirective: Directive<HTMLElement, PermissionRequirement | PermissionDirectiveValue> = {
  mounted: applyPermission,
  updated: applyPermission
};
