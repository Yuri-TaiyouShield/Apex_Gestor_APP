import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { Injectable, PLATFORM_ID, inject, signal } from '@angular/core';

export type ApexDialogTone = 'primary' | 'warning' | 'danger';

export interface ApexConfirmOptions {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  tone?: ApexDialogTone;
}

export interface ApexPromptOptions {
  title: string;
  message?: string;
  label: string;
  placeholder?: string;
  initialValue?: string;
  required?: boolean;
  requiredMessage?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  tone?: ApexDialogTone;
}

export type ActiveDialogState =
  | {
      kind: 'confirm';
      options: Required<ApexConfirmOptions>;
      resolve: (value: boolean) => void;
    }
  | {
      kind: 'prompt';
      options: Required<ApexPromptOptions>;
      value: string;
      validationError: string | null;
      resolve: (value: string | null) => void;
    };

/**
 * Substituto acessivel e nao-bloqueante para window.confirm e window.prompt,
 * compativel com navegacao por teclado e Electron/Capacitor sandboxed.
 */
@Injectable({ providedIn: 'root' })
export class DialogService {
  private readonly document = inject(DOCUMENT);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  private triggerElement: HTMLElement | null = null;
  readonly active = signal<ActiveDialogState | null>(null);

  confirm(options: ApexConfirmOptions): Promise<boolean> {
    this.cancelPending();
    this.captureTriggerFocus();

    return new Promise<boolean>((resolve) => {
      this.active.set({
        kind: 'confirm',
        options: {
          title: options.title,
          message: options.message,
          confirmLabel: options.confirmLabel ?? 'Confirmar',
          cancelLabel: options.cancelLabel ?? 'Cancelar',
          tone: options.tone ?? 'primary'
        },
        resolve
      });
    });
  }

  prompt(options: ApexPromptOptions): Promise<string | null> {
    this.cancelPending();
    this.captureTriggerFocus();

    return new Promise<string | null>((resolve) => {
      this.active.set({
        kind: 'prompt',
        options: {
          title: options.title,
          message: options.message ?? '',
          label: options.label,
          placeholder: options.placeholder ?? '',
          initialValue: options.initialValue ?? '',
          required: options.required ?? true,
          requiredMessage: options.requiredMessage ?? 'Este campo é obrigatório.',
          confirmLabel: options.confirmLabel ?? 'Confirmar',
          cancelLabel: options.cancelLabel ?? 'Cancelar',
          tone: options.tone ?? 'primary'
        },
        value: options.initialValue ?? '',
        validationError: null,
        resolve
      });
    });
  }

  updatePromptValue(value: string): void {
    const current = this.active();
    if (current?.kind !== 'prompt') {
      return;
    }
    this.active.set({
      ...current,
      value,
      validationError: null
    });
  }

  accept(): void {
    const current = this.active();
    if (!current) {
      return;
    }
    if (current.kind === 'confirm') {
      this.closeAndRestoreFocus();
      current.resolve(true);
      return;
    }
    const trimmed = current.value.trim();
    if (current.options.required && !trimmed) {
      this.active.set({
        ...current,
        validationError: current.options.requiredMessage
      });
      return;
    }
    this.closeAndRestoreFocus();
    current.resolve(trimmed);
  }

  cancel(): void {
    const current = this.active();
    if (!current) {
      return;
    }
    this.closeAndRestoreFocus();
    if (current.kind === 'confirm') {
      current.resolve(false);
    } else {
      current.resolve(null);
    }
  }

  private cancelPending(): void {
    const current = this.active();
    if (!current) {
      return;
    }
    this.active.set(null);
    if (current.kind === 'confirm') {
      current.resolve(false);
    } else {
      current.resolve(null);
    }
  }

  private captureTriggerFocus(): void {
    if (!this.isBrowser) {
      this.triggerElement = null;
      return;
    }
    const activeEl = this.document?.activeElement;
    this.triggerElement = activeEl instanceof HTMLElement ? activeEl : null;
  }

  private closeAndRestoreFocus(): void {
    this.active.set(null);
    const target = this.triggerElement;
    this.triggerElement = null;
    if (this.isBrowser && target && typeof target.focus === 'function') {
      queueMicrotask(() => target.focus());
    }
  }
}
