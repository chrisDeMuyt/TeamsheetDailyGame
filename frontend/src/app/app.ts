import { ChangeDetectionStrategy, Component, computed, inject, signal, viewChild } from '@angular/core';
import { GuessHistoryComponent } from './components/guess-history.component';
import { GuessInputComponent } from './components/guess-input.component';
import { IntroComponent } from './components/intro.component';
import { PitchComponent } from './components/pitch.component';
import { RevealScreenComponent } from './components/reveal-screen.component';
import { SubsStripComponent } from './components/subs-strip.component';
import { GUESSES, GUESS_MS, GameStore } from './state/game-store';
import { competitionLong, competitionShort, formatMatchDate } from './state/view';

@Component({
  selector: 'app-root',
  imports: [IntroComponent, PitchComponent, SubsStripComponent, GuessInputComponent, GuessHistoryComponent,
            RevealScreenComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly store = inject(GameStore);
  protected readonly GUESSES = GUESSES;
  protected readonly GUESS_MS = GUESS_MS;

  protected readonly draft = signal('');
  private readonly guessInput = viewChild(GuessInputComponent);

  protected readonly number = computed(() => this.store.reveal()?.puzzleNumber ?? this.store.puzzle()?.number);
  protected readonly competition = computed(() => competitionLong(this.store.puzzle()?.competition ?? ''));
  protected readonly matchDate = computed(() => {
    const p = this.store.puzzle();
    return p ? formatMatchDate(p.date) : '';
  });
  protected readonly revealLine = computed(() => {
    const r = this.store.reveal();
    return r ? `${r.team} · ${competitionShort(r.competition)} · ${formatMatchDate(r.date)}` : '';
  });

  constructor() {
    void this.store.init();
  }

  /** Enter / Submit: exact full name -> guessed; otherwise a "Did you mean" appears (no guess used). */
  protected async guess(text: string): Promise<void> {
    const result = await this.store.submitText(text);
    if (result) {
      this.draft.set('');
      this.guessInput()?.focus();
    } else if (this.store.suggestion()) {
      this.guessInput()?.focusSuggestion();
    } else {
      this.guessInput()?.select(); // not found: keep the text so they can fix it
    }
  }

  protected async pickSuggestion(): Promise<void> {
    const result = await this.store.pickSuggestion();
    if (result) this.draft.set('');
    this.guessInput()?.focus();
  }

  protected retry(): void {
    void this.store.init();
  }
}
