import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Pick } from '../api/models';
import { pickLabel, pickOutcome } from '../state/view';

/** Chips for guesses already used: "1 · Traoré Cult Hero +100", "2 · Diao Not in squad 0". */
@Component({
  selector: 'app-guess-history',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (picks().length) {
      <ol class="history" aria-label="Your guesses so far">
        @for (p of picks(); track p.guessIndex) {
          <li class="chip" [class.bad]="p.result !== 'hit'">
            <span class="who">{{ p.guessIndex }} · {{ label(p) }}</span>
            <strong class="what">{{ outcome(p) }} {{ p.result === 'hit' ? '+' + p.points : '0' }}</strong>
          </li>
        }
      </ol>
    }
  `,
  styles: `
    :host { display: block; }
    .history {
      list-style: none;
      margin: 0;
      padding: 0;
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
      gap: 8px;
    }
    .chip {
      background: var(--panel);
      border-radius: 10px;
      padding: 8px 12px;
      font-size: 15px;
      display: flex;
      flex-wrap: wrap;
      column-gap: 6px;
    }
    .what { color: var(--accent); font-weight: 600; }
    .bad .what { color: var(--miss); }
  `,
})
export class GuessHistoryComponent {
  readonly picks = input.required<Pick[]>();
  protected readonly label = pickLabel;
  protected readonly outcome = pickOutcome;
}
