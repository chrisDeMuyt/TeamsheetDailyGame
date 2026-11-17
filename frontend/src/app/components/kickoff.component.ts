import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { Puzzle } from '../api/models';
import { competitionLong, formatMatchDate, scoreline } from '../state/view';

/**
 * The 5s transition after Start: the team, then the match and date, then the scoreline, each fading in.
 * The guess clock only starts once it ends (GameStore creates the play afterwards).
 */
@Component({
  selector: 'app-kickoff',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="kickoff" role="status" aria-live="polite" [style.--total]="durationMs() + 'ms'">
      <p class="step team">{{ puzzle().team }}</p>
      <p class="step match">{{ match() }} · <span class="nowrap">{{ date() }}</span></p>
      <p class="step score">
        {{ score().main }}@if (score().note) {<span class="note"> ({{ score().note }})</span>}
      </p>
      <div class="bar" aria-hidden="true"><span></span></div>
      <p class="step ready">Get ready…</p>
    </section>
  `,
  styles: `
    :host { display: flex; flex: 1; }
    .kickoff {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      text-align: center;
      gap: 14px;
      padding: 24px 0;
    }
    .step { margin: 0; opacity: 0; animation: rise 450ms ease-out forwards; }
    .team {
      font-family: var(--font-head);
      font-size: clamp(36px, 12vw, 52px);
      font-weight: 700;
      line-height: 1.05;
      letter-spacing: -0.02em;
      color: var(--accent);
    }
    .match { font-size: 18px; color: var(--muted); animation-delay: calc(var(--total) * 0.3); }
    .score { font-size: clamp(20px, 6vw, 24px); font-weight: 700; animation-delay: calc(var(--total) * 0.6); }
    .note { font-size: 15px; font-weight: 500; color: var(--muted); white-space: nowrap; }
    .ready { font-size: 14px; color: var(--muted); animation-delay: calc(var(--total) * 0.8); }
    .nowrap { white-space: nowrap; }
    .bar {
      width: min(70%, 260px);
      height: 4px;
      margin-top: 18px;
      border-radius: 2px;
      background: var(--panel);
      overflow: hidden;
    }
    .bar span {
      display: block;
      height: 100%;
      background: var(--accent);
      transform-origin: left;
      animation: fill var(--total) linear forwards;
    }
    @keyframes rise { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: none; } }
    @keyframes fill { from { transform: scaleX(0); } to { transform: scaleX(1); } }
    @media (prefers-reduced-motion: reduce) {
      .step { animation-duration: 1ms; }
    }
  `,
})
export class KickoffComponent {
  readonly puzzle = input.required<Puzzle>();
  readonly durationMs = input(5000);

  protected readonly match = computed(() => competitionLong(this.puzzle().competition));
  protected readonly date = computed(() => formatMatchDate(this.puzzle().date));
  /** "Liverpool 3–3 AC Milan" + note "aet, 3–2 pens" (shown smaller). */
  protected readonly score = computed(() => {
    const p = this.puzzle();
    const s = scoreline(p.team, p.opponent, p.result);
    const i = s.indexOf(' (');
    return i < 0 ? { main: s, note: '' } : { main: s.slice(0, i), note: s.slice(i + 2, -1) };
  });
}
