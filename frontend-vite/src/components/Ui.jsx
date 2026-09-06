import { useRef, useState } from "react";
import { MoreHorizontal } from "lucide-react";

/* ------------------------------------------------------------------ */
/* Page — shell header every page renders inside                       */
/* ------------------------------------------------------------------ */

export function Page({ title, eyebrow, model, actions, children }) {
  const [scrolled, setScrolled] = useState(false);
  const scrollRef = useRef(null);

  const handleScroll = () => {
    const top = scrollRef.current?.scrollTop || 0;
    setScrolled((prev) => (top > 4) !== prev ? top > 4 : prev);
  };

  return (
    <div className="flex h-full min-w-0 flex-col">
      <header className={`topbar ${scrolled ? "topbar-elevated" : ""}`}>
        <div className="min-w-0">
          {eyebrow && <p className="eyebrow truncate">{eyebrow}</p>}
          <h1 className="mt-1 truncate font-display text-[18px] font-semibold leading-tight tracking-[-0.01em] text-text">
            {title}
          </h1>
        </div>

        <div className="flex shrink-0 items-center gap-2.5">
          {model && (
            <div className="status-pill status-online hidden md:inline-flex">
              <span className="relative flex h-1.5 w-1.5 shrink-0">
                <span className="absolute inline-flex h-full w-full animate-pulse-soft rounded-full bg-current opacity-60" />
                <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-current" />
              </span>
              <span className="max-w-[220px] truncate">{model}</span>
            </div>
          )}
          {actions && <div className="flex items-center gap-2">{actions}</div>}
        </div>
      </header>

      <div className="page-scroll" ref={scrollRef} onScroll={handleScroll}>
        <div className="page-wrap">{children}</div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------ */
/* Stat — headline metric card, used 3-up across every page            */
/* ------------------------------------------------------------------ */

const TONE_RULE = {
  accent: "bg-primary",
  teal: "bg-teal",
  blue: "bg-blue",
  warning: "bg-warning",
  danger: "bg-danger",
  neutral: "bg-line-bright",
};

export function Stat({ label, value, detail, icon: Icon, tone = "accent" }) {
  return (
    <div className="panel relative overflow-hidden p-4">
      <span className={`absolute inset-x-0 top-0 h-[2px] ${TONE_RULE[tone] || TONE_RULE.accent}`} />

      <div className="flex items-start gap-3.5">
        {Icon && (
          <div className={`icon-box icon-box-${tone}`}>
            <Icon size={17} />
          </div>
        )}

        <div className="min-w-0 flex-1">
          <p className="text-3xs font-semibold uppercase tracking-[.11em] text-faint">{label}</p>
          <p className="mt-1.5 truncate font-display text-[21px] font-semibold leading-none tracking-[-0.01em] text-text">
            {value}
          </p>
          {detail && <p className="mt-2 text-2xs leading-4 text-muted">{detail}</p>}
        </div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------ */
/* Badge                                                                */
/* ------------------------------------------------------------------ */

export function Badge({ tone = "neutral", dot, children }) {
  return (
    <span className={`badge badge-${tone}`}>
      {dot && (
        <span className="relative flex h-[5px] w-[5px] shrink-0">
          {(tone === "teal" || tone === "accent") && (
            <span className="absolute inline-flex h-full w-full animate-pulse-soft rounded-full bg-current opacity-60" />
          )}
          <span className="badge-dot relative" />
        </span>
      )}
      {children}
    </span>
  );
}

/* ------------------------------------------------------------------ */
/* MiniBar — inline progress/load indicator                            */
/* ------------------------------------------------------------------ */

export function MiniBar({ value = 0, tone = "accent" }) {
  const pct = Math.min(100, Math.max(0, value));
  return (
    <div className="h-[5px] w-full overflow-hidden rounded-full bg-panel-3">
      <div
        className={`mini-bar mini-bar-${tone} transition-[width] duration-500 ease-out`}
        style={{ width: `${pct}%` }}
      />
    </div>
  );
}

/* ------------------------------------------------------------------ */
/* TableActions — trailing row affordance in data tables                */
/* ------------------------------------------------------------------ */

export function TableActions() {
  return (
    <button className="icon-button" aria-label="Row actions" onClick={(e) => e.stopPropagation()}>
      <MoreHorizontal size={15} />
    </button>
  );
}