import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Spot } from '../state/view';
import { SpotComponent } from './spot.component';

/** Subs who came on, beside the pitch. */
@Component({
  selector: 'app-subs-strip',
  imports: [SpotComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="strip" aria-labelledby="subs-heading">
      <h2 id="subs-heading">Subs</h2>
      <div class="list" role="list" aria-label="Substitutes who came on">
        @for (s of spots(); track s.slot) {
          <app-spot [spot]="s" [pop]="s.slot === poppedSlot()" />
        }
      </div>
      @if (showBonus()) {
        <p class="note">Subs who came on <strong>+15</strong></p>
      }
    </section>
  `,
  styles: `
    /* A column beside the pitch; turns into a row when it gets the full width (reveal on narrow phones). */
    :host { display: block; min-width: 0; container-type: inline-size; }
    .strip {
      background: var(--panel);
      border-radius: var(--radius);
      padding: 14px 6px;
      height: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
    }
    h2 {
      font-family: var(--font-body);
      font-size: 12px;
      font-weight: 700;
      letter-spacing: 0.12em;
      text-transform: uppercase;
      color: var(--muted);
    }
    .list { display: flex; flex-direction: column; align-items: center; gap: 14px; width: 100%; }
    .note {
      margin: 4px 0 0;
      font-size: 12px;
      color: var(--muted);
      text-align: center;
      line-height: 1.35;
    }
    .note strong { display: block; color: var(--accent); }
    /* Must come after the base rules above. */
    @container (min-width: 220px) {
      .strip { padding: 12px; }
      .list { flex-direction: row; justify-content: space-evenly; align-items: flex-start; }
    }
  `,
})
export class SubsStripComponent {
  readonly spots = input.required<Spot[]>();
  readonly poppedSlot = input<number | null>(null);
  readonly showBonus = input(true);
}
