import type { CSSProperties, ReactNode } from 'react';
import { IconInbox } from '@tabler/icons-react';
import { statusMeta } from './status';
import './ui.css';

interface PanelProps {
  title?: ReactNode;
  subtitle?: ReactNode;
  action?: ReactNode;
  /** Remove body padding (tables that should reach the card edges). */
  flush?: boolean;
  footer?: ReactNode;
  children?: ReactNode;
  className?: string;
  style?: CSSProperties;
  [key: `data-${string}`]: string | undefined;
}

export function Panel({ title, subtitle, action, flush, footer, children, className, style, ...rest }: PanelProps) {
  return (
    <section className={`sg-panel${className ? ` ${className}` : ''}`} style={style} {...rest}>
      {(title || action) && (
        <header className="sg-panel__head">
          <div>
            {title && <h3 className="sg-panel__title">{title}</h3>}
            {subtitle && <p className="sg-panel__sub">{subtitle}</p>}
          </div>
          {action}
        </header>
      )}
      {children !== undefined && (
        <div className={flush ? 'sg-panel__body sg-panel__body--flush' : 'sg-panel__body'}>{children}</div>
      )}
      {footer && <footer className="sg-panel__foot">{footer}</footer>}
    </section>
  );
}

export function StatusPill({ status, pulse }: { status: string; pulse?: boolean }) {
  const m = statusMeta(status);
  const style = { '--pb': m.soft, '--pf': m.text, '--pd': m.color } as CSSProperties;
  return (
    <span className={`sg-pill${pulse ? ' sg-pill--pulse' : ''}`} style={style}>
      {m.label}
    </span>
  );
}

export function MiniBar({ value, color }: { value: number; color?: string }) {
  const style = color ? ({ '--kc': color } as CSSProperties) : undefined;
  return (
    <span className="sg-bar" style={style} aria-hidden>
      <i style={{ width: `${Math.max(0, Math.min(100, value))}%` }} />
    </span>
  );
}

export function EmptyState({ children, icon }: { children: ReactNode; icon?: ReactNode }) {
  return (
    <div className="sg-empty">
      <span className="sg-empty__icon">{icon ?? <IconInbox size={18} stroke={1.6} />}</span>
      <span>{children}</span>
    </div>
  );
}

export function SkeletonRows({ rows = 4 }: { rows?: number }) {
  return (
    <div style={{ padding: '8px 20px 20px', display: 'flex', flexDirection: 'column', gap: 14 }}>
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="sg-skel" style={{ height: 18, opacity: 1 - i * 0.12 }} />
      ))}
    </div>
  );
}
