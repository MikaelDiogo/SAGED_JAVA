import type { CSSProperties, ElementType, ReactNode } from 'react';
import { MiniBar } from '../ui';
import '../ui/ui.css';

interface StatCardProps {
  label: string;
  value: string | number;
  /** Hex color (semantic status color). Falls back to brand green. */
  color?: string;
  icon: ElementType;
  /** Small caption under the number. */
  hint?: ReactNode;
  /** 0-100: renders a thin share bar next to the caption. */
  share?: number;
}

/** Tint a hex color to a soft background (8% alpha). */
function soft(hex: string) {
  return `${hex}14`;
}

export function StatCard({ label, value, color = '#1d720a', icon: Icon, hint, share }: StatCardProps) {
  const style = { '--kc': color, '--kc-soft': soft(color) } as CSSProperties;
  return (
    <div className="sg-panel sg-kpi" style={style}>
      <div className="sg-kpi__top">
        <span className="sg-kpi__label">
          <span className="sg-kpi__dot" />
          {label}
        </span>
        <span className="sg-kpi__icon">
          <Icon size={16} stroke={1.8} />
        </span>
      </div>
      <div className="sg-kpi__value">{value}</div>
      {(hint || share !== undefined) && (
        <div className="sg-kpi__foot">
          {share !== undefined && <MiniBar value={share} />}
          {hint && <span style={{ whiteSpace: 'nowrap' }}>{hint}</span>}
        </div>
      )}
    </div>
  );
}
