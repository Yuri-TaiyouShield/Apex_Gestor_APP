import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, ElementRef, HostListener, effect, inject, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DialogService } from '../../../core/ui/dialog.service';

@Component({
  selector: 'app-dialog-host',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (dialog.active(); as current) {
      <div class="dialog-backdrop">
        <button
          type="button"
          class="dialog-scrim"
          tabindex="-1"
          aria-label="Fechar diálogo"
          (click)="dialog.cancel()"
        ></button>
        <section
          #dialogCard
          class="dialog-card"
          [attr.role]="current.kind === 'confirm' ? 'alertdialog' : 'dialog'"
          aria-modal="true"
          aria-labelledby="apex-dialog-title"
          [attr.aria-describedby]="current.options.message ? 'apex-dialog-message' : null"
          tabindex="-1"
          (keydown)="onDialogKeydown($event)"
        >
          <header class="dialog-header">
            <h2 id="apex-dialog-title">{{ current.options.title }}</h2>
          </header>

          <div class="dialog-body">
            @if (current.options.message) {
              <p id="apex-dialog-message">{{ current.options.message }}</p>
            }

            @if (current.kind === 'prompt') {
              <label class="dialog-field">
                <span>{{ current.options.label }}</span>
                <input
                  #promptInput
                  type="text"
                  [ngModel]="current.value"
                  (ngModelChange)="dialog.updatePromptValue($event)"
                  [placeholder]="current.options.placeholder"
                  (keydown.enter)="dialog.accept()"
                />
              </label>
              @if (current.validationError) {
                <p class="dialog-error" role="alert">{{ current.validationError }}</p>
              }
            }
          </div>

          <footer class="dialog-footer">
            <button type="button" class="dialog-btn dialog-btn--secondary" (click)="dialog.cancel()">
              {{ current.options.cancelLabel }}
            </button>
            <button
              #confirmButton
              type="button"
              [class]="'dialog-btn dialog-btn--' + current.options.tone"
              (click)="dialog.accept()"
            >
              {{ current.options.confirmLabel }}
            </button>
          </footer>
        </section>
      </div>
    }
  `,
  styles: [`
    .dialog-backdrop {
      align-items: center;
      display: flex;
      inset: 0;
      justify-content: center;
      padding: 16px;
      position: fixed;
      z-index: var(--apex-z-modal);
    }

    .dialog-scrim {
      backdrop-filter: blur(4px);
      background: rgba(7, 11, 20, 0.62);
      border: 0;
      cursor: default;
      inset: 0;
      padding: 0;
      position: fixed;
    }

    .dialog-card {
      background: var(--apex-bg-surface);
      border: 1px solid var(--apex-border-strong);
      border-radius: var(--apex-radius-lg);
      box-shadow: var(--apex-shadow-lg);
      color: var(--apex-text-primary);
      display: grid;
      gap: 14px;
      max-width: 460px;
      padding: 20px;
      position: relative;
      width: 100%;
      z-index: 1;
    }

    .dialog-header h2 {
      color: var(--apex-text-primary);
      font-size: 1.08rem;
      font-weight: 750;
      margin: 0;
    }

    .dialog-body {
      display: grid;
      gap: 12px;
    }

    .dialog-body p {
      color: var(--apex-text-secondary);
      font-size: var(--apex-font-size-sm);
      line-height: 1.5;
      margin: 0;
    }

    .dialog-field {
      display: grid;
      gap: 6px;
    }

    .dialog-field span {
      color: var(--apex-text-secondary);
      font-size: var(--apex-font-size-xs);
      font-weight: 700;
    }

    .dialog-field input {
      background: var(--apex-bg-input);
      border: 1px solid var(--apex-border-default);
      border-radius: var(--apex-radius-md);
      color: var(--apex-text-primary);
      font: inherit;
      min-height: var(--apex-control-height);
      padding: 8px 12px;
      width: 100%;
    }

    .dialog-error {
      color: var(--apex-status-danger) !important;
      font-size: var(--apex-font-size-xs) !important;
      font-weight: 600;
    }

    .dialog-footer {
      display: flex;
      gap: 10px;
      justify-content: flex-end;
      padding-top: 4px;
    }

    .dialog-btn {
      align-items: center;
      border: 1px solid transparent;
      border-radius: var(--apex-radius-md);
      cursor: pointer;
      display: inline-flex;
      font: inherit;
      font-size: var(--apex-font-size-sm);
      font-weight: 700;
      justify-content: center;
      min-height: 38px;
      padding: 0 16px;
    }

    .dialog-btn--secondary {
      background: var(--apex-bg-surface-muted);
      border-color: var(--apex-border-default);
      color: var(--apex-text-primary);
    }

    .dialog-btn--primary {
      background: var(--apex-fill-primary);
      color: var(--apex-fill-primary-contrast);
    }

    .dialog-btn--warning {
      background: var(--apex-fill-warning);
      color: var(--apex-fill-warning-contrast);
    }

    .dialog-btn--danger {
      background: var(--apex-fill-danger);
      color: var(--apex-fill-danger-contrast);
    }
  `]
})
export class ApexDialogHostComponent {
  readonly dialog = inject(DialogService);
  private readonly dialogCard = viewChild<ElementRef<HTMLElement>>('dialogCard');
  private readonly promptInput = viewChild<ElementRef<HTMLInputElement>>('promptInput');
  private readonly confirmButton = viewChild<ElementRef<HTMLButtonElement>>('confirmButton');

  constructor() {
    effect(() => {
      const current = this.dialog.active();
      if (!current) {
        return;
      }
      queueMicrotask(() => {
        if (current.kind === 'prompt') {
          this.promptInput()?.nativeElement.focus();
        } else {
          this.confirmButton()?.nativeElement.focus();
        }
      });
    });
  }

  onDialogKeydown(event: KeyboardEvent): void {
    if (event.key !== 'Tab') {
      return;
    }
    const card = this.dialogCard()?.nativeElement;
    if (!card) {
      return;
    }
    const focusables = Array.from(
      card.querySelectorAll<HTMLElement>(
        'button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), a[href], [tabindex]:not([tabindex="-1"])'
      )
    );
    if (!focusables.length) {
      event.preventDefault();
      card.focus();
      return;
    }
    const first = focusables[0];
    const last = focusables[focusables.length - 1];
    const activeEl = card.ownerDocument?.activeElement;
    if (event.shiftKey && (activeEl === first || activeEl === card)) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && activeEl === last) {
      event.preventDefault();
      first.focus();
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.dialog.active()) {
      this.dialog.cancel();
    }
  }
}
