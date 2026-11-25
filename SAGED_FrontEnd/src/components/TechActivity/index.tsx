import type { CSSProperties } from 'react';
import { StatusPill } from '../ui';
import '../ui/ui.css';

interface TechActivityProps {
  techName: string;
  demand: string;
  status: string;
  reason?: string;
  time: string;
}

function initials(name: string) {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '?';
  return (parts[0][0] + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase();
}

export function TechActivity({ techName, demand, status, reason, time }: TechActivityProps) {
  const interrupted = status === 'INTERRUPTED';
  return (
    <div>
      <div className="sg-activity">
        <span className="sg-avatar" aria-hidden>{initials(techName)}</span>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 13.5, fontWeight: 600, color: 'var(--sg-text)' }}>{techName}</div>
          <div style={{ fontSize: 12.5, color: 'var(--sg-muted)', marginTop: 1 }}>{demand}</div>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 4 }}>
          {interrupted ? (
            <StatusPill status="INTERRUPTED" />
          ) : (
            <span
              className="sg-pill sg-pill--pulse"
              style={{ '--pb': '#e7f3fd', '--pf': '#1864ab', '--pd': '#1c7ed6' } as CSSProperties}
            >
              {status === 'ONLINE' ? 'Online' : status}
            </span>
          )}
          <span style={{ fontSize: 11.5, color: 'var(--sg-faint)' }}>{time}</span>
        </div>
      </div>

      {reason && (
        <div style={{ margin: '0 20px 14px', padding: '8px 12px', borderRadius: 8, background: '#fff5f5', border: '1px solid #ffd8d8' }}>
          <div style={{ fontSize: 11, fontWeight: 600, letterSpacing: '.06em', color: '#c92a2a' }}>JUSTIFICATIVA</div>
          <div style={{ fontSize: 12.5, color: 'var(--sg-text-2)', marginTop: 2 }}>{reason}</div>
        </div>
      )}
    </div>
  );
}
