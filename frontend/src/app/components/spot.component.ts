import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { Spot } from '../state/view';

/** One circle + label. Positioned absolutely on the pitch, or stacked in the subs strip. */
@Component({
  selector: 'app-spot',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[class]': '"spot " + spot().state',
    '[class.placed]': 'placed()',
    '[class.pop]': 'pop()',
    '[style.left.%]': 'placed() ? left() : null',
    '[style.top.%]': 'placed() ? top() : null',
    role: 'listitem',
  },
  template: `
    <span class="disc" aria-hidden="true">{{ spot().state === 'hit' ? spot().points : '' }}</span>
    <span class="label" aria-hidden="true">{{ spot().label }}@if (spot().sub) {<span class="arrow"> ↑</span>}</span>
    @if (spot().detail) {
      <span class="detail" aria-hidden="true">{{ spot().detail }}</span>
    }
    <span class="sr-only">{{ srText() }}</span>
  `,
  styles: `
    :host {
      --disc: clamp(32px, 10vw, 44px);
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 3px;
      text-align: center;
      font-size: 12px;
      line-height: 1.15;
      font-weight: 700;
      min-width: 0;
    }
    :host(.placed) {
      position: absolute;
      transform: translate(-50%, calc(var(--disc) / -2));
      width: 25%;
    }
    .disc {
      width: var(--disc);
      height: var(--disc);
      border-radius: 50%;
      display: grid;
      place-items: center;
      font-family: var(--font-head);
      font-size: 15px;
      font-weight: 700;
      flex: none;
    }
    :host(.empty) .disc {
      border: 2px dashed rgba(242, 244, 241, 0.6);
      background: rgba(14, 26, 20, 0.12);
    }
    :host(.hit) .disc, :host(.picked) .disc {
      background: var(--accent);
      color: var(--bg);
    }
    :host(.picked) .disc { box-shadow: 0 0 0 4px rgba(245, 197, 66, 0.3); }
    :host(.revealed) .disc { background: var(--text); }

    .label { white-space: nowrap; }
    :host(:not(.empty)) .label {
      background: var(--chip);
      border-radius: 4px;
      padding: 1px 5px;
    }
    :host(.hit) .label, :host(.picked) .label { color: var(--accent); }
    .arrow { font-weight: 700; }
    .detail { font-weight: 500; color: var(--text); }
    :host(.picked) .detail { color: var(--accent); font-weight: 700; }

    :host(.pop) .disc { animation: pop 520ms cubic-bezier(.2, 1.4, .4, 1); }
    :host(.pop) .label { animation: fade 400ms ease-out; }
    @keyframes pop {
      0% { transform: scale(0.3); }
      60% { transform: scale(1.18); }
      100% { transform: scale(1); }
    }
    @keyframes fade { from { opacity: 0; transform: translateY(4px); } }
  `,
})
export class SpotComponent {
  readonly spot = input.required<Spot>();
  readonly placed = input(false);
  readonly pop = input(false);

  /** x 0 = left touchline; y 0 = own goal line (GK at the bottom, like the mockup). */
  protected readonly left = computed(() => this.spot().x ?? 50);
  protected readonly top = computed(() => 9 + (85 - (this.spot().y ?? 50)) * 0.98);

  protected readonly srText = computed(() => {
    const s = this.spot();
    const where = s.sub ? `Substitute, ${s.pos}` : s.pos;
    switch (s.state) {
      case 'empty': return `${where}: not found yet`;
      case 'hit': return `${where}: ${s.label}, ${s.points} points`;
      case 'picked': return `${where}: ${s.label}, picked by ${s.detail} of players. Your pick`;
      default: return `${where}: ${s.label}, picked by ${s.detail} of players`;
    }
  });
}
