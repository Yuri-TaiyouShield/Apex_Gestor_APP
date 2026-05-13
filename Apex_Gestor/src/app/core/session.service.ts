import { Injectable, computed, signal } from '@angular/core';

import { DEFAULT_PERSONA } from './app-variant';
import { Persona } from './models';

const STORAGE_KEY = 'apex.persona';

@Injectable({ providedIn: 'root' })
export class SessionService {
  readonly persona = signal<Persona>(this.readPersona());
  readonly roleLabel = computed(() => {
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

  private readPersona(): Persona {
    const value = localStorage.getItem(STORAGE_KEY);
    if (value === 'cliente' || value === 'vendedor' || value === 'gerente' || value === 'admin') {
      return value;
    }
    return DEFAULT_PERSONA;
  }
}
