import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Spot } from '../state/view';
import { SpotComponent } from './spot.component';

/** The starting XI on a pitch, positioned from the API's x/y. Position labels only until a slot is found. */
@Component({
  selector: 'app-pitch',
  imports: [SpotComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="pitch">
      <div class="markings" aria-hidden="true">
        <span class="halfway"></span>
        <span class="circle"></span>
        <span class="box"></span>
      </div>
      <div class="spots" role="list" [attr.aria-label]="label()">
        @for (s of spots(); track s.slot) {
          <app-spot [spot]="s" [placed]="true" [pop]="s.slot === poppedSlot()" />
        }
      </div>
    </div>
  `,
  styles: `
    :host { display: block; min-width: 0; }
    .pitch {
      position: relative;
      background: var(--pitch);
      border-radius: var(--radius);
      aspect-ratio: 0.66;
      padding: 12px;
    }
    .markings {
      position: absolute;
      inset: 12px;
      border: 1.5px solid var(--line);
      border-radius: 2px;
      overflow: hidden;
    }
    .halfway {
      position: absolute; left: 0; right: 0; top: 50%;
      border-top: 1.5px solid var(--line);
    }
    .circle {
      position: absolute; left: 50%; top: 50%;
      width: 30%; aspect-ratio: 1;
      transform: translate(-50%, -50%);
      border: 1.5px solid var(--line);
      border-radius: 50%;
    }
    .box {
      position: absolute; left: 22%; right: 22%; bottom: 0;
      height: 14%;
      border: 1.5px solid var(--line);
      border-bottom: none;
    }
    .spots { position: absolute; inset: 12px; }
  `,
})
export class PitchComponent {
  readonly spots = input.required<Spot[]>();
  readonly poppedSlot = input<number | null>(null);
  readonly label = input('Starting eleven');
}
