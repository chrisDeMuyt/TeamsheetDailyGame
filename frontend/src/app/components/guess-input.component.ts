import {
  ChangeDetectionStrategy, Component, ElementRef, computed, input, model, output, viewChild,
} from '@angular/core';
import { PlayerOption } from '../api/models';
import { Feedback } from '../state/game-store';
import { formatClock } from '../state/view';

/**
 * Name box + countdown bar. Enter or Submit sends the text; if it isn't an exact full name the parent shows
 * one "Did you mean" button underneath, and tapping that is the guess.
 */
@Component({
  selector: 'app-guess-input',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form class="guess" (submit)="onSubmit($event)" novalidate>
      <div class="row">
        <span class="count">Guess {{ guessNumber() }} of {{ total() }}</span>
        <span class="clock" [class.low]="low()" aria-hidden="true">{{ clock() }}</span>
      </div>
      <div class="bar" role="progressbar" aria-label="Time left for this guess"
           aria-valuemin="0" [attr.aria-valuemax]="totalSeconds()"
           [attr.aria-valuenow]="seconds()" [attr.aria-valuetext]="seconds() + ' seconds left'">
        <div class="fill" [class.low]="low()" [style.width.%]="percent()"></div>
      </div>
      <span class="sr-only" aria-live="polite">{{ warning() }}</span>

      <div class="field">
        <label class="sr-only" for="guess-text">Player name, guess {{ guessNumber() }} of {{ total() }}</label>
        <input #box id="guess-text" type="text" maxlength="100"
               autocomplete="off" autocorrect="off" autocapitalize="words" spellcheck="false" enterkeyhint="send"
               placeholder="Type a player's name"
               [value]="text()" (input)="text.set(box.value); edited.emit()"
               [attr.aria-describedby]="feedback() ? 'guess-feedback' : null" />
        <button type="submit" class="btn-primary" [disabled]="busy() || !text().trim()">Submit</button>
      </div>

      @if (suggestion(); as s) {
        <div class="suggest">
          <span class="suggest-label">Did you mean</span>
          <button #suggestBtn type="button" class="suggest-btn" [disabled]="busy()"
                  (click)="suggestionPicked.emit()">{{ s.name }}?</button>
        </div>
      }

      <p id="guess-feedback" class="feedback" role="status" [class]="'feedback ' + (feedback()?.kind ?? '')">
        {{ feedback()?.text }}
      </p>
    </form>
  `,
  styles: `
    :host { display: block; }
    .guess { display: flex; flex-direction: column; gap: 8px; }
    .row { display: flex; justify-content: space-between; align-items: baseline; }
    .count { font-weight: 600; font-size: 17px; }
    .clock { font-family: var(--font-head); font-weight: 700; font-size: 17px; color: var(--accent); font-variant-numeric: tabular-nums; }
    .clock.low { color: var(--miss); }
    .bar { height: 5px; border-radius: 3px; background: rgba(169, 184, 174, 0.25); overflow: hidden; }
    .fill { height: 100%; background: var(--accent); transition: width 200ms linear; }
    .fill.low { background: var(--miss); }
    .field { display: flex; gap: 12px; margin-top: 8px; }
    input {
      flex: 1;
      min-width: 0;
      min-height: 52px;
      padding: 0 16px;
      background: var(--input-bg);
      border: 1.5px solid var(--border);
      border-radius: 12px;
      font-size: 17px; /* >= 16px so iOS doesn't zoom */
    }
    input::placeholder { color: var(--muted); opacity: 0.85; }
    input:focus-visible { outline: none; border-color: var(--accent); box-shadow: 0 0 0 3px rgba(245, 197, 66, 0.25); }
    button { min-height: 52px; font-size: 17px; padding: 0 22px; }
    .suggest {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;
      background: var(--panel);
      border-radius: 12px;
      padding: 8px 10px 8px 14px;
      animation: rise 160ms ease-out;
    }
    .suggest-label { color: var(--muted); font-size: 15px; }
    .suggest-btn {
      flex: 1;
      min-height: var(--touch);
      background: transparent;
      color: var(--accent);
      border: 1.5px solid var(--accent);
      font-weight: 700;
      font-size: 16px;
      padding: 0 14px;
      text-align: left;
    }
    @keyframes rise { from { opacity: 0; transform: translateY(4px); } }
    .feedback { margin: 0; min-height: 1.4em; font-size: 14px; color: var(--muted); }
    .feedback.notfound, .feedback.suggest { color: var(--text); }
    .feedback.hit { color: var(--accent); }
    .feedback.miss, .feedback.timeout, .feedback.error { color: var(--miss); }
    .feedback.ambiguous, .feedback.duplicate { color: var(--text); }
  `,
})
export class GuessInputComponent {
  readonly guessNumber = input.required<number>();
  readonly total = input(3);
  readonly remainingMs = input.required<number>();
  readonly totalMs = input(30_000);
  readonly busy = input(false);
  readonly feedback = input<Feedback | null>(null);
  readonly suggestion = input<PlayerOption | null>(null);
  readonly text = model('');
  readonly submitted = output<string>();
  readonly suggestionPicked = output<void>();
  /** The user changed the text (an old "Did you mean" no longer applies). */
  readonly edited = output<void>();

  private readonly box = viewChild.required<ElementRef<HTMLInputElement>>('box');

  protected readonly seconds = computed(() => Math.max(0, Math.ceil(this.remainingMs() / 1000)));
  protected readonly totalSeconds = computed(() => Math.round(this.totalMs() / 1000));
  protected readonly clock = computed(() => formatClock(this.remainingMs()));
  protected readonly percent = computed(() => Math.max(0, Math.min(100, (this.remainingMs() / this.totalMs()) * 100)));
  protected readonly low = computed(() => this.seconds() <= 5);
  /** Announced once at 10s and at 5s, not every second. */
  protected readonly warning = computed(() => {
    const s = this.seconds();
    return s <= 5 && s > 0 ? '5 seconds left' : s <= 10 && s > 5 ? '10 seconds left' : '';
  });

  protected onSubmit(e: Event): void {
    e.preventDefault();
    const t = this.text().trim();
    if (!t || this.busy()) return;
    this.submitted.emit(t);
  }

  private readonly suggestBtn = viewChild<ElementRef<HTMLButtonElement>>('suggestBtn');

  focus(): void {
    this.box().nativeElement.focus();
  }

  /** Move to the "Did you mean" button once it renders (also drops the phone keyboard so it's visible). */
  focusSuggestion(): void {
    setTimeout(() => this.suggestBtn()?.nativeElement.focus());
  }

  select(): void {
    this.box().nativeElement.select();
  }
}
