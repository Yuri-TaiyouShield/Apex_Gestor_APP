import { Injectable, computed, signal } from '@angular/core';

import { DEFAULT_PERSONA } from './app-variant';
import { ApexRole, Persona } from './models';

const STORAGE_KEY = 'apex.persona';
const ROLES_STORAGE_KEY = 'apex.roles';

@Injectable({ providedIn: 'root' })
export class SessionService {
  readonly persona = signal<Persona>(this.readPersona());
  readonly roles = signal<ApexRole[]>(this.readRoles());
  readonly roleLabel = computed(() => {
    const primaryRole = this.roles()[0];
    const roleLabels: Partial<Record<ApexRole, string>> = {
      ROLE_SYSADMIN: 'Sysadmin',
      ROLE_DONO_GERENTE: 'Dono/Gerente',
      ROLE_FINANCEIRO: 'Financeiro',
      ROLE_VENDEDOR: 'Vendedor',
      ROLE_CAIXA: 'Caixa',
      ROLE_DESPACHANTE: 'Despachante',
      ROLE_CLIENTE_B2C: 'Cliente'
    };
    if (primaryRole && roleLabels[primaryRole]) {
      return roleLabels[primaryRole];
    }
    const labels: Record<Persona, string> = {
      cliente: 'Cliente',
      vendedor: 'Vendedor',
      gerente: 'Gerente',
      admin: 'Administrador'
    };
    return labels[this.persona()];
  });

  setPersona(persona: Persona): void {
    localStorage.setItem(STORAGE_KEY, persona);
    this.persona.set(persona);
  }

  setRoles(roles: string[]): void {
    const normalized = roles.map((role) => this.normalizeRole(role));
    localStorage.setItem(ROLES_STORAGE_KEY, JSON.stringify(normalized));
    this.roles.set(normalized);
  }

  clearAuthContext(): void {
    localStorage.removeItem(ROLES_STORAGE_KEY);
    this.roles.set([]);
  }

  hasAnyRole(roles: readonly ApexRole[] | undefined): boolean {
    if (!roles || roles.length === 0) {
      return true;
    }
    const current = new Set(this.roles());
    return roles.some((role) => current.has(role));
  }

  normalizeRole(role: string): ApexRole {
    const normalized = role.startsWith('ROLE_') ? role : `ROLE_${role}`;
    return normalized as ApexRole;
  }

  private readPersona(): Persona {
    const value = localStorage.getItem(STORAGE_KEY);
    if (value === 'cliente' || value === 'vendedor' || value === 'gerente' || value === 'admin') {
      return value;
    }
    return DEFAULT_PERSONA;
  }

  private readRoles(): ApexRole[] {
    const value = localStorage.getItem(ROLES_STORAGE_KEY);
    if (!value) {
      return [];
    }
    try {
      const roles = JSON.parse(value) as string[];
      return roles.map((role) => this.normalizeRole(role));
    } catch {
      return [];
    }
  }
}
