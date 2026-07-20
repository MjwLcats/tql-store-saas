import type { InjectionKey } from 'vue';
import type { AppConfig } from '@tql-store/shared';

export const APP_CONFIG_KEY: InjectionKey<AppConfig> = Symbol('TQL_APP_CONFIG');
