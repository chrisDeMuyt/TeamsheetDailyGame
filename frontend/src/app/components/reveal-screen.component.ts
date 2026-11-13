import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { Pick, Reveal } from '../api/models';
import { SUB_BONUS, Spot, buildShareText, pickLabel, pickOutcome } from '../state/view';
import { PitchComponent } from './pitch.component';
import { ShareCardComponent } from './share-card.component';
import { SubsStripComponent } from './subs-strip.component';

/** Full squad with every name and pick %, your picks lit, then the pick list and share actions. */
@Component({
  selector: 'app-reveal-screen',
  imports: [PitchComponent, SubsStripComponent, ShareCardComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 class="sr-only">Full lineup with pick percentages</h2>
    <div class="board">
      <app-pitch [spots]="starters()" label="Starting eleven, revealed" />
      <app-subs-strip [spots]="subs()" [showBonus]="false" />
    </div>

    <h2 class="sr-only">Your picks</h2>
    <ol class="picks">
      @for (p of reveal().picks; track p.guessIndex) {
        <li class="row" [class.bad]="p.result !== 'hit'">
          <span class="who">{{ p.guessIndex }} · {{ label(p) }}@if (p.sub) {<span class="muted"> (sub)</span>}</span>
          <strong class="what">{{ outcomeText(p) }}</strong>
        </li>
      }
    </ol>

    <p class="basis">
      @if (reveal().pctSource === 'seed') {
        Pick % are early estimates until enough people have played.
      } @else {
        Pick % = share of today's players who named them.
      }
    </p>

    <app-share-card class="share" [text]="shareText()" [streak]="streak()" [lastPlayed]="lastPlayed()" />
  `,
  styles: `
    :host { display: flex; flex-direction: column; gap: 14px; flex: 1; }
    .board {
      display: grid;
      grid-template-columns: minmax(0, 1fr) clamp(76px, 24%, 128px);
      gap: 12px;
    }
    /* Full names need the whole width on small phones: subs move under the pitch. */
    @media (max-width: 419px) {
      .board { grid-template-columns: minmax(0, 1fr); }
    }
    .picks { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
    .row {
      background: var(--panel);
      border-radius: 10px;
      padding: 12px 14px;
      display: flex;
      justify-content: space-between;
      gap: 12px;
      font-size: 17px;
    }
    .muted { color: var(--muted); }
    .what { color: var(--accent); font-weight: 600; text-align: right; white-space: nowrap; }
    .bad .what { color: var(--miss); }
    .basis { margin: 0; font-size: 13px; color: var(--muted); }
    .share { margin-top: auto; padding-top: 12px; }
  `,
})
export class RevealScreenComponent {
  readonly reveal = input.required<Reveal>();
  readonly starters = input.required<Spot[]>();
  readonly subs = input.required<Spot[]>();
  readonly streak = input(0);
  readonly lastPlayed = input<number | null>(null);

  protected readonly label = pickLabel;
  /** Link back to the game: whatever origin it's served from (https://teamsheet-daily.fly.dev in production). */
  private readonly link = typeof location !== 'undefined' ? location.origin : undefined;
  protected readonly shareText = computed(() => buildShareText(this.reveal(), this.streak(), this.link));

  /** "Cult Hero · 100", "Proper Fan 60 + 15" for subs, "Not in squad · 0". */
  protected outcomeText(p: Pick): string {
    if (p.result === 'hit' && p.sub) return `${p.tier} ${p.points - SUB_BONUS} + ${SUB_BONUS}`;
    return `${pickOutcome(p)} · ${p.points}`;
  }
}
