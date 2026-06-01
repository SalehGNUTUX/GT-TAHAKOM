// ui.jsx — shared primitives + phone shell. Token- and RTL-aware.
// Exports: UICtx, useUI, Card, Btn, IconBtn, Chip, TransportChip, SignalDots,
//   Divider, Dot, StatusBar, BottomNav, Sheet, PhoneFrame, fontStack
const { createContext, useContext } = React;

const UICtx = createContext(null);
const useUI = () => useContext(UICtx);

const fontStack = "'IBM Plex Sans Arabic','IBM Plex Sans',system-ui,sans-serif";

// ── Card ─────────────────────────────────────────────
function Card({ children, style, pad, raised, onClick, interactive }) {
  const { tk } = useUI();
  const c = tk.colors;
  return (
    <div onClick={onClick}
      style={{
        background: raised ? c.surface2 : c.surface,
        border: `${tk.hair}px solid ${c.line}`,
        borderRadius: tk.radius.lg,
        padding: pad !== undefined ? pad : tk.cardPad,
        boxShadow: tk.elev,
        boxSizing: 'border-box',
        cursor: onClick || interactive ? 'pointer' : 'default',
        transition: 'transform .18s ease, box-shadow .2s ease, background .2s',
        ...style,
      }}>
      {children}
    </div>
  );
}

// ── Button ───────────────────────────────────────────
function Btn({ children, onClick, variant = 'solid', size = 'md', full, style, disabled, accent }) {
  const { tk } = useUI();
  const c = tk.colors;
  const ac = accent || c.accent;
  const pads = size === 'lg' ? '15px 22px' : size === 'sm' ? '8px 14px' : '12px 18px';
  const fs = size === 'lg' ? 16 : size === 'sm' ? 13.5 : 15;
  const base = {
    fontFamily: fontStack, fontSize: fs, fontWeight: 600,
    padding: pads, borderRadius: tk.radius.pill, border: 'none',
    cursor: disabled ? 'default' : 'pointer', width: full ? '100%' : 'auto',
    display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 8,
    opacity: disabled ? 0.45 : 1, transition: 'transform .15s ease, background .2s, opacity .2s',
    letterSpacing: '.2px', boxSizing: 'border-box',
  };
  const variants = {
    solid:   { background: ac, color: c.accentText },
    soft:    { background: c.accentSoft, color: ac },
    outline: { background: 'transparent', color: c.text, border: `1.5px solid ${c.lineStrong}` },
    ghost:   { background: 'transparent', color: c.textDim },
  };
  return (
    <button disabled={disabled} onClick={onClick}
      onMouseDown={e => !disabled && (e.currentTarget.style.transform = 'scale(0.97)')}
      onMouseUp={e => (e.currentTarget.style.transform = 'scale(1)')}
      onMouseLeave={e => (e.currentTarget.style.transform = 'scale(1)')}
      style={{ ...base, ...variants[variant], ...style }}>
      {children}
    </button>
  );
}

// ── round icon button ────────────────────────────────
function IconBtn({ name, onClick, size = 44, icon = 22, variant = 'surface', style, label, color }) {
  const { tk } = useUI();
  const c = tk.colors;
  const styles = {
    surface: { background: c.surface, border: `${tk.hair}px solid ${c.line}`, color: c.text },
    ghost:   { background: 'transparent', border: 'none', color: color || c.textDim },
    soft:    { background: c.accentSoft, border: 'none', color: c.accent },
  };
  return (
    <button onClick={onClick} aria-label={label}
      onMouseDown={e => (e.currentTarget.style.transform = 'scale(0.92)')}
      onMouseUp={e => (e.currentTarget.style.transform = 'scale(1)')}
      onMouseLeave={e => (e.currentTarget.style.transform = 'scale(1)')}
      style={{
        width: size, height: size, borderRadius: '50%', flexShrink: 0,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        cursor: 'pointer', transition: 'transform .14s ease, background .2s',
        boxShadow: tk.elev !== 'none' && variant === 'surface' ? tk.elev : 'none',
        ...styles[variant], ...style,
      }}>
      <Icon name={name} size={icon} stroke={color || (variant === 'soft' ? c.accent : 'currentColor')} />
    </button>
  );
}

// ── chip / badge ─────────────────────────────────────
function Chip({ children, color, bg, style }) {
  const { tk } = useUI();
  const c = tk.colors;
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: 6,
      fontSize: 12.5, fontWeight: 600, letterSpacing: '.2px',
      padding: '5px 11px', borderRadius: tk.radius.pill,
      color: color || c.textDim, background: bg || c.bg2,
      border: bg ? 'none' : `${tk.hair}px solid ${c.line}`,
      ...style,
    }}>{children}</span>
  );
}

function TransportChip({ tr, size = 'md' }) {
  const { tk, tr: T, lang } = useUI();
  const c = tk.colors;
  const meta = TRANSPORT_META[tr];
  const col = transportColorOf(c, tr);
  const soft = transportSoftOf(c, tr);
  const small = size === 'sm';
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: small ? 5 : 7,
      fontSize: small ? 11.5 : 12.5, fontWeight: 600,
      padding: small ? '4px 9px' : '6px 11px', borderRadius: tk.radius.pill,
      color: col, background: soft, whiteSpace: 'nowrap',
    }}>
      <Icon name={meta.icon} size={small ? 13 : 15} stroke={col} strokeWidth={1.9} />
      {T(meta.key)}
    </span>
  );
}

function SignalDots({ level = 3, color }) {
  const { tk } = useUI();
  const c = tk.colors;
  return (
    <span style={{ display: 'inline-flex', alignItems: 'flex-end', gap: 2.5, height: 14 }}>
      {[6, 9, 12].map((h, i) => (
        <span key={i} style={{
          width: 3, height: h, borderRadius: 2,
          background: i < level ? (color || c.accent) : c.line,
        }} />
      ))}
    </span>
  );
}

function Divider({ style }) {
  const { tk } = useUI();
  return <div style={{ height: tk.hair, background: tk.colors.line, ...style }} />;
}

function Dot({ color, size = 8, glow }) {
  return <span style={{
    width: size, height: size, borderRadius: '50%', background: color,
    boxShadow: glow ? `0 0 0 3px ${color}28` : 'none', flexShrink: 0,
  }} />;
}

// ── app icon badge ───────────────────────────────────
function Logo({ size = 28, radius, glow }) {
  const { tk } = useUI();
  return (
    <img src="assets/tahakom-icon.png" width={size} height={size} alt="TAHAKOM"
      style={{
        borderRadius: radius != null ? radius : Math.round(size * 0.26), display: 'block',
        objectFit: 'cover', flexShrink: 0,
        boxShadow: glow ? `0 6px 22px -6px ${tk.colors.wifi}66` : tk.elev,
      }} />
  );
}

// ── status bar ───────────────────────────────────────
function StatusBar() {
  const { tk } = useUI();
  const col = tk.colors.text;
  return (
    <div style={{
      height: 40, display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '0 22px', flexShrink: 0, color: col,
    }}>
      <span style={{ fontSize: 14.5, fontWeight: 600, letterSpacing: '.3px' }}>9:30</span>
      <div style={{ display: 'flex', alignItems: 'center', gap: 7 }}>
        <Icon name="wifi" size={15} stroke={col} strokeWidth={1.8} />
        <Icon name="signal" size={15} stroke={col} strokeWidth={2} />
        <span style={{
          width: 22, height: 11, borderRadius: 3, border: `1.4px solid ${col}`,
          position: 'relative', display: 'inline-block',
        }}>
          <span style={{ position: 'absolute', inset: 1.5, width: '72%', borderRadius: 1.5, background: col }} />
        </span>
      </div>
    </div>
  );
}

// ── bottom nav ───────────────────────────────────────
function BottomNav({ tab, onTab }) {
  const { tk, tr: T } = useUI();
  const c = tk.colors;
  const items = [
    { id: 'devices',  icon: 'tv',      label: T('navDevices') },
    { id: 'remote',   icon: 'power',   label: T('navRemote') },
    { id: 'add',      icon: 'plus',    label: T('navAdd') },
    { id: 'settings', icon: 'gear',    label: T('navSettings') },
  ];
  return (
    <div style={{
      flexShrink: 0, display: 'flex', alignItems: 'stretch',
      borderTop: `${tk.hair}px solid ${c.line}`, background: c.bg,
      padding: '6px 10px 4px', gap: 4,
    }}>
      {items.map(it => {
        const on = tab === it.id;
        return (
          <button key={it.id} onClick={() => onTab(it.id)}
            style={{
              flex: 1, background: 'none', border: 'none', cursor: 'pointer',
              display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4,
              padding: '6px 0', color: on ? c.accent : c.textFaint,
              transition: 'color .2s',
            }}>
            <span style={{
              padding: '3px 16px', borderRadius: tk.radius.pill,
              background: on ? c.accentSoft : 'transparent', transition: 'background .2s',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <Icon name={it.icon} size={21} stroke={on ? c.accent : c.textFaint}
                    strokeWidth={on ? 2 : 1.7} />
            </span>
            <span style={{ fontSize: 11, fontWeight: on ? 700 : 500 }}>{it.label}</span>
          </button>
        );
      })}
    </div>
  );
}

// ── bottom sheet overlay ─────────────────────────────
function Sheet({ open, onClose, children, title }) {
  const { tk } = useUI();
  const c = tk.colors;
  return (
    <div style={{
      position: 'absolute', inset: 0, zIndex: 40,
      pointerEvents: open ? 'auto' : 'none',
    }}>
      <div onClick={onClose} style={{
        position: 'absolute', inset: 0, background: 'rgba(0,0,0,.42)',
        opacity: open ? 1 : 0, transition: 'opacity .28s',
        backdropFilter: open ? 'blur(2px)' : 'none',
      }} />
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0,
        background: c.bg, borderTopLeftRadius: 28, borderTopRightRadius: 28,
        borderTop: `${tk.hair}px solid ${c.line}`,
        transform: open ? 'translateY(0)' : 'translateY(calc(100% + 120px))',
        transition: 'transform .34s cubic-bezier(.32,.72,0,1)',
        padding: '10px 20px calc(20px + env(safe-area-inset-bottom))',
        maxHeight: '82%', overflowY: 'auto', boxShadow: '0 -20px 50px rgba(0,0,0,.25)',
      }}>
        <div style={{ display: 'flex', justifyContent: 'center', paddingBottom: 6 }}>
          <div style={{ width: 38, height: 4, borderRadius: 2, background: c.lineStrong }} />
        </div>
        {title && <div style={{ fontSize: 19, fontWeight: 700, color: c.text, padding: '6px 2px 14px' }}>{title}</div>}
        {children}
      </div>
    </div>
  );
}

// ── phone shell ──────────────────────────────────────
function PhoneFrame({ children, bottom, overlay, rtl }) {
  const { tk } = useUI();
  const c = tk.colors;
  return (
    <div style={{
      width: 402, height: 858, borderRadius: 46, padding: 11,
      background: 'linear-gradient(160deg,#16171b,#0a0a0c)',
      boxShadow: '0 2px 2px rgba(255,255,255,.12) inset, 0 40px 90px -30px rgba(0,0,0,.6), 0 0 0 1px rgba(0,0,0,.5)',
      boxSizing: 'border-box', position: 'relative', flexShrink: 0,
    }}>
      <div dir={rtl ? 'rtl' : 'ltr'} style={{
        width: '100%', height: '100%', borderRadius: 36, overflow: 'hidden',
        background: c.bg, color: c.text, fontFamily: fontStack,
        display: 'flex', flexDirection: 'column', position: 'relative',
        transition: 'background .35s ease, color .35s ease',
      }}>
        {/* camera punch-hole */}
        <div style={{
          position: 'absolute', top: 13, left: '50%', transform: 'translateX(-50%)',
          width: 11, height: 11, borderRadius: '50%', background: '#000', zIndex: 50,
          boxShadow: '0 0 0 2.5px rgba(0,0,0,.4)',
        }} />
        <StatusBar />
        <div style={{ flex: 1, minHeight: 0, position: 'relative', display: 'flex', flexDirection: 'column' }}>
          {children}
        </div>
        {bottom}
        {overlay}
      </div>
    </div>
  );
}

Object.assign(window, {
  UICtx, useUI, fontStack,
  Card, Btn, IconBtn, Chip, TransportChip, SignalDots, Divider, Dot, Logo,
  StatusBar, BottomNav, Sheet, PhoneFrame,
});
