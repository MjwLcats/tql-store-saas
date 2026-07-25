import { computed, type ComputedRef } from 'vue';
import { defineStore } from 'pinia';
import { fetchMenus, fetchProfile } from '@tql-store/api';
import type { ClientType, MenuItem, UserProfile } from '@tql-store/shared';

const tokenKey = (clientType: ClientType) => `tql-store:${clientType.toLowerCase()}:token`;

export const getToken = (clientType: ClientType) => localStorage.getItem(tokenKey(clientType));
export const setToken = (clientType: ClientType, token: string) => localStorage.setItem(tokenKey(clientType), token);
export const clearToken = (clientType: ClientType) => localStorage.removeItem(tokenKey(clientType));

export type PermissionRequirement = string | string[];
export type PermissionMatchMode = 'any' | 'all';

export function evaluatePermission(
  granted: Iterable<string>,
  required?: PermissionRequirement,
  mode: PermissionMatchMode = 'any'
) {
  if (!required || (Array.isArray(required) && required.length === 0)) return true;
  const grantedSet = granted instanceof Set ? granted : new Set(granted);
  const requirements = Array.isArray(required) ? required : [required];
  return mode === 'all'
    ? requirements.every(permission => grantedSet.has(permission))
    : requirements.some(permission => grantedSet.has(permission));
}

export const useAppStore = defineStore('tql-store-app', {
  state: () => ({
    collapsed: false,
    loading: false,
    profile: null as UserProfile | null,
    menus: [] as MenuItem[]
  }),
  actions: {
    toggleCollapsed() {
      this.collapsed = !this.collapsed;
    },
    async loadContext(force = false) {
      if (!force && this.profile && this.menus.length > 0) return;
      this.loading = true;
      try {
        const [profile, menus] = await Promise.all([fetchProfile(), fetchMenus()]);
        this.profile = profile;
        this.menus = menus;
      } finally {
        this.loading = false;
      }
    },
    reset() {
      this.profile = null;
      this.menus = [];
      this.collapsed = false;
    }
  }
});

export function effectiveMenus(menus: MenuItem[]) {
  const byId = new Map(menus.map(menu => [menu.id, menu]));
  const enabled = (menu: MenuItem) => {
    if (menu.status !== 1) return false;
    let parentId = menu.parentId;
    const visited = new Set<number>();
    while (parentId && !visited.has(parentId)) {
      visited.add(parentId);
      const parent = byId.get(parentId);
      if (!parent) break;
      if (parent.status !== 1) return false;
      parentId = parent.parentId;
    }
    return true;
  };
  return menus.filter(enabled);
}

export function usePermission(): {
  permissions: ComputedRef<Set<string>>;
  can: (required?: PermissionRequirement, mode?: PermissionMatchMode) => boolean;
} {
  const store = useAppStore();
  const permissions = computed(() => new Set(
    effectiveMenus(store.menus).map(menu => menu.permission).filter((value): value is string => Boolean(value))
  ));
  return {
    permissions,
    can: (required, mode = 'any') => evaluatePermission(permissions.value, required, mode)
  };
}
