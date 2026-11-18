import { ChangeDetectionStrategy, Component, ElementRef, input, signal, viewChild } from '@angular/core';
import { PlayerStats } from '../api/models';

/** Native sheet only on touch devices that support it; desktop browsers get the clipboard (what people expect). */
export function useNativeShare(data: ShareData): boolean {
  if (typeof navigator === 'undefined' || typeof navigator.share !== 'function') return false;
  if (navigator.canShare && !navigator.canShare(data)) return false;
  return typeof matchMedia === 'function' && matchMedia('(pointer: coarse)').matches;
}

/**
 * "Share result" opens the phone's share sheet (or copies on desktop) with the emoji mini-pitch;
 * "Stats" opens the player's history: played, current and max streak, personal best.
 */
@Component({
  selector: 'app-share-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="actions">
      <button type="button" class="btn-primary share" (click)="share()">Share result</button>
      <button type="button" class="btn-ghost" (click)="openStats()" aria-haspopup="dialog">Stats</button>
    </div>
    <p class="status" role="status">{{ status() }}</p>
    @if (fallback()) {
      <label class="fallback">
        <span>Copy this:</span>
        <textarea #fallbackBox readonly rows="9">{{ text() }}</textarea>
      </label>
    }

    <dialog #dlg aria-labelledby="stats-title" (click)="backdropClose($event)">
      <h2 id="stats-title">Your stats</h2>
      <dl>
        <div><dt>Played</dt><dd>{{ stats()?.played ?? 1 }}</dd></div>
        <div><dt>Current streak</dt><dd>{{ stats()?.currentStreak ?? streak() }}</dd></div>
        <div><dt>Max streak</dt><dd>{{ stats()?.maxStreak ?? streak() }}</dd></div>
        <div><dt>Personal best</dt><dd>{{ stats()?.bestScore ?? '—' }}</dd></div>
      </dl>
      <button type="button" class="btn-primary" (click)="dlg.close()">Close</button>
    </dialog>
  `,
  styles: `
    :host { display: block; }
    .actions { display: flex; gap: 12px; }
    .actions button { min-height: 56px; font-size: 18px; }
    .share { flex: 1; }
    .status { margin: 8px 0 0; min-height: 1.4em; font-size: 14px; color: var(--accent); text-align: center; }
    .fallback { display: flex; flex-direction: column; gap: 6px; font-size: 14px; color: var(--muted); }
    textarea {
      width: 100%;
      background: var(--input-bg);
      color: var(--text);
      border: 1.5px solid var(--border);
      border-radius: 10px;
      padding: 10px;
      font: 15px/1.3 var(--font-body);
    }
    dialog {
      background: var(--panel);
      color: var(--text);
      border: 1px solid var(--border);
      border-radius: 18px;
      padding: 22px;
      width: min(92vw, 380px);
    }
    dialog::backdrop { background: rgba(0, 0, 0, 0.6); }
    h2 { font-size: 22px; margin-bottom: 14px; }
    dl { margin: 0 0 18px; display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
    dl div { background: var(--bg); border-radius: 12px; padding: 12px; }
    dt { font-size: 13px; color: var(--muted); }
    dd { margin: 4px 0 0; font-family: var(--font-head); font-size: 22px; font-weight: 700; color: var(--accent); }
    dialog button { width: 100%; }
  `,
})
export class ShareCardComponent {
  readonly text = input.required<string>();
  readonly streak = input(0);
  readonly stats = input<PlayerStats | undefined>(undefined);

  protected readonly status = signal('');
  protected readonly fallback = signal(false);
  private readonly dlg = viewChild.required<ElementRef<HTMLDialogElement>>('dlg');
  private readonly fallbackBox = viewChild<ElementRef<HTMLTextAreaElement>>('fallbackBox');

  /**
   * "Share result": the phone's native share sheet (iOS Safari, Android) where available, like Wordle-style games;
   * copy-to-clipboard on desktop. Must run straight from the tap (no await before navigator.share).
   */
  async share(): Promise<void> {
    const data: ShareData = { text: this.text() };
    if (useNativeShare(data)) {
      try {
        await navigator.share(data);
        this.status.set('');
        this.fallback.set(false);
        return;
      } catch (e) {
        if (e instanceof DOMException && e.name === 'AbortError') return; // user closed the sheet
        // Anything else (blocked, unsupported target): fall back to copying.
      }
    }
    await this.copy();
  }

  async copy(): Promise<void> {
    try {
      await navigator.clipboard.writeText(this.text());
      this.status.set('Copied to clipboard. Paste it anywhere.');
      this.fallback.set(false);
    } catch {
      // Clipboard blocked (http on a LAN IP, old browser): show the text to copy by hand.
      this.fallback.set(true);
      this.status.set('Copy blocked by the browser. Select the text below.');
      setTimeout(() => this.fallbackBox()?.nativeElement.select());
    }
  }

  openStats(): void {
    this.dlg().nativeElement.showModal();
  }

  protected backdropClose(e: MouseEvent): void {
    if (e.target === this.dlg().nativeElement) this.dlg().nativeElement.close();
  }
}
