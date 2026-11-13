import { ChangeDetectionStrategy, Component, ElementRef, afterNextRender, input, output, viewChild } from '@angular/core';

/**
 * Start screen. Shown before anything about today's match is loaded, so the team stays hidden and the
 * 30s clock can't start until the player taps Start.
 */
@Component({
  selector: 'app-intro',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="intro" aria-labelledby="intro-title">
      <div class="brand">
        <h1 id="intro-title" class="wordmark">TEAMSHEET</h1>
        @if (number()) {
          <span class="num">#{{ number() }}</span>
        }
      </div>
      <p class="tagline">The daily football lineup quiz</p>

      <svg class="pitch" viewBox="0 0 200 270" role="img" aria-label="Teamsheet: a football pitch with three players highlighted">
        <rect x="4" y="4" width="192" height="262" rx="14" fill="var(--pitch)" />
        <g fill="none" stroke="rgba(242,244,241,0.35)" stroke-width="1.5">
          <rect x="16" y="16" width="168" height="238" rx="2" />
          <line x1="16" y1="135" x2="184" y2="135" />
          <circle cx="100" cy="135" r="24" />
          <rect x="58" y="16" width="84" height="32" />
          <rect x="58" y="222" width="84" height="32" />
        </g>
        <g fill="rgba(14,26,20,0.15)" stroke="rgba(242,244,241,0.6)" stroke-width="1.6" stroke-dasharray="3 3">
          @for (p of empty; track $index) {
            <circle [attr.cx]="p[0]" [attr.cy]="p[1]" r="9" />
          }
        </g>
        <g fill="var(--accent)">
          @for (p of lit; track $index) {
            <circle class="lit" [attr.cx]="p[0]" [attr.cy]="p[1]" r="10" [style.animation-delay.ms]="$index * 180" />
          }
        </g>
      </svg>

      <ul class="rules">
        <li><span aria-hidden="true">⚽</span> Name 3 players from one famous match lineup</li>
        <li><span aria-hidden="true">⏱️</span> 30 seconds per guess. The clock starts when you tap Start</li>
        <li><span aria-hidden="true">💎</span> Rarer picks score more. Subs who came on +15</li>
      </ul>

      @if (streak() > 1) {
        <p class="streak">🔥 {{ streak() }}-day streak. Keep it going</p>
      }

      <button #startBtn type="button" class="btn-primary start" [disabled]="busy()" (click)="start.emit()">
        {{ busy() ? 'Loading…' : 'Start' }}
      </button>
    </section>
  `,
  styles: `
    :host { display: flex; flex: 1; }
    .intro {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      gap: 14px;
      padding: 4px 0 4px;
    }
    .brand { display: flex; align-items: baseline; gap: 10px; margin-top: 8px; }
    .wordmark {
      font-size: clamp(40px, 13vw, 56px);
      font-weight: 700;
      letter-spacing: -0.02em;
      line-height: 1;
    }
    .num { font-family: var(--font-head); font-size: 22px; font-weight: 600; color: var(--accent); }
    .tagline { margin: -6px 0 0; color: var(--muted); font-size: 16px; }
    .pitch { width: min(52vw, 210px); height: auto; filter: drop-shadow(0 10px 24px rgba(0,0,0,0.35)); }
    .lit { transform-box: fill-box; transform-origin: center; animation: pop 600ms cubic-bezier(.2,1.4,.4,1) both; }
    @keyframes pop { 0% { transform: scale(0); } 70% { transform: scale(1.2); } 100% { transform: scale(1); } }
    .rules {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 10px;
      text-align: left;
      width: 100%;
      max-width: 360px;
      font-size: 15px;
    }
    .rules li {
      display: flex;
      gap: 10px;
      align-items: flex-start;
      background: var(--panel);
      border-radius: 12px;
      padding: 10px 14px;
    }
    .streak { margin: 0; color: var(--accent); font-weight: 600; }
    .start { margin-top: auto; width: 100%; max-width: 360px; min-height: 56px; font-size: 19px; }
  `,
})
export class IntroComponent {
  readonly number = input<number | null>(null);
  readonly streak = input(0);
  readonly busy = input(false);
  readonly start = output<void>();

  /** Decorative only (a generic 4-3-3), not today's formation, so it gives nothing away. */
  protected readonly empty: [number, number][] = [
    [100, 240], [160, 200], [125, 206], [40, 200], [145, 150], [55, 150], [160, 92], [40, 92],
  ];
  protected readonly lit: [number, number][] = [[75, 206], [100, 160], [100, 70]]; // 8 + 3 = 11

  private readonly startBtn = viewChild.required<ElementRef<HTMLButtonElement>>('startBtn');

  constructor() {
    afterNextRender(() => this.startBtn().nativeElement.focus());
  }
}
