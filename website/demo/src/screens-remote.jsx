// screens-remote.jsx — gestural remote: touchpad (+ pointer mode), primary
// controls (incl. Menu), and a "More controls" sheet (numbers, functions, colors)
const { useState: useStateR, useRef: useRefR, useEffect: useEffectR } = React;

function RemoteScreen({ device, onBack, onShowTransport, onOpenMore, send, beamKey, pointer, onPointer }) {
  const { tk, tr: T, lang, rtl } = useUI();
  const c = tk.colors;
  const dev = device || DEVICES[0];
  const tcol = transportColorOf(c, dev.transport);

  const rowBtn = (icon, label, onClick, opts = {}) => (
    <button onClick={() => { send(label); onClick && onClick(); }} aria-label={label}
      style={{
        background: opts.bg || c.surface, border: `${tk.hair}px solid ${opts.bg ? 'transparent' : c.line}`,
        color: opts.color || c.text, borderRadius: tk.radius.pill, cursor: 'pointer',
        width: opts.w || 52, height: opts.w || 52, display: 'flex', alignItems: 'center', justifyContent: 'center',
        boxShadow: tk.elev, flexShrink: 0, transition: 'transform .14s',
      }}
      onMouseDown={e => (e.currentTarget.style.transform = 'scale(.9)')}
      onMouseUp={e => (e.currentTarget.style.transform = 'scale(1)')}
      onMouseLeave={e => (e.currentTarget.style.transform = 'scale(1)')}>
      <Icon name={icon} size={opts.ic || 23} stroke={opts.color || c.text} strokeWidth={1.8} />
    </button>
  );

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', minHeight: 0 }}>
      {/* header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '6px 16px 4px' }}>
        <IconBtn name={rtl ? 'forwardNav' : 'back'} variant="ghost" onClick={onBack} />
        <div style={{ flex: 1, minWidth: 0, textAlign: 'center' }}>
          <div style={{ fontSize: 16.5, fontWeight: 700, color: c.text, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
            {lang === 'ar' ? dev.nameAr : dev.nameEn}
          </div>
          <div style={{ fontSize: 12.5, color: c.textFaint, marginTop: 1 }}>{dev.brand} · {lang === 'ar' ? dev.roomAr : dev.roomEn}</div>
        </div>
        <IconBtn name="source" variant="ghost" size={40} icon={21} onClick={() => send(T('rmSource'))} />
        <IconBtn name="more" variant="ghost" size={40} icon={21} onClick={onOpenMore} label={T('rmMore')} />
      </div>

      {/* active transport pill */}
      <div style={{ padding: '6px 16px 2px' }}>
        <button onClick={onShowTransport} style={{
          width: '100%', display: 'flex', alignItems: 'center', gap: 12, cursor: 'pointer',
          background: transportSoftOf(c, dev.transport), border: 'none', borderRadius: tk.radius.md,
          padding: '11px 14px', fontFamily: fontStack, textAlign: 'start',
        }}>
          <span style={{ position: 'relative', display: 'flex', alignItems: 'center', justifyContent: 'center', width: 34, height: 34 }}>
            <span key={beamKey} className="om-beam" style={{ position: 'absolute', inset: 0, borderRadius: '50%', border: `2px solid ${tcol}` }} />
            <Icon name={TRANSPORT_META[dev.transport].icon} size={22} stroke={tcol} strokeWidth={2} />
          </span>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 11, fontWeight: 600, color: tcol, opacity: .8, letterSpacing: '.3px' }}>{T('rmActivePath')}</div>
            <div style={{ fontSize: 14, fontWeight: 700, color: tcol }}>{T(TRANSPORT_META[dev.transport].key)}</div>
          </div>
          <Chip color={tcol} bg={tk.dark ? 'rgba(255,255,255,.06)' : 'rgba(255,255,255,.6)'} style={{ border: 'none' }}>
            {T('trWhy')}
          </Chip>
        </button>
      </div>

      {/* top control cluster (incl. Menu) */}
      <div style={{ display: 'flex', justifyContent: 'center', gap: 9, padding: '12px 16px 4px' }}>
        {rowBtn('power', T('rmPower'), null, { bg: c.accent, color: c.accentText, w: 50, ic: 22 })}
        {rowBtn('homeBtn', T('rmHome'), null, { w: 50, ic: 22 })}
        {rowBtn('menu', T('rmMenu'), null, { w: 50, ic: 22 })}
        {rowBtn(rtl ? 'forwardNav' : 'back', T('rmBack'), null, { w: 50, ic: 22 })}
        {rowBtn('mic', T('rmVoice'), null, { bg: transportSoftOf(c, dev.transport), color: tcol, w: 50, ic: 22 })}
      </div>

      {/* pointer-mode banner */}
      {pointer && (
        <div style={{ padding: '8px 16px 0' }}>
          <div style={{
            display: 'flex', alignItems: 'center', gap: 9, padding: '8px 12px',
            borderRadius: tk.radius.pill, background: c.accentSoft,
          }}>
            <Icon name="pointer" size={16} stroke={c.accent} strokeWidth={1.9} />
            <span style={{ flex: 1, fontSize: 12.5, fontWeight: 700, color: c.accent }}>{T('morePointerOn')}</span>
            <button onClick={() => onPointer(false)} style={{
              background: 'none', border: 'none', cursor: 'pointer', color: c.accent,
              fontSize: 12.5, fontWeight: 700, fontFamily: fontStack, textDecoration: 'underline',
            }}>{T('moreExit')}</button>
          </div>
        </div>
      )}

      {/* touchpad */}
      <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: 0, padding: '6px 16px' }}>
        <TouchPad pointerMode={pointer}
          onDir={(d) => send({ up: '▲', down: '▼', left: '◀', right: '▶' }[d])}
          onOk={() => send('OK')} onClick={() => send(T('rmClick'))} />
      </div>

      {/* rockers */}
      <div style={{ display: 'flex', gap: 12, padding: '4px 16px' }}>
        <Rocker icon="volUp" iconDown="volDown" label={T('rmVolume')} onUp={() => send(T('rmVolume') + ' +')} onDown={() => send(T('rmVolume') + ' −')}
          mid={<IconBtn name="mute" variant="ghost" size={38} icon={20} onClick={() => send(T('rmMute'))} />} />
        <Rocker icon="caretUp" iconDown="caretDown" label={T('rmChannel')} onUp={() => send(T('rmChannel') + ' +')} onDown={() => send(T('rmChannel') + ' −')} />
      </div>

      {/* media */}
      <div style={{ display: 'flex', justifyContent: 'center', gap: 16, padding: '10px 16px 16px' }}>
        {rowBtn(rtl ? 'forward' : 'rewind', '⏪', null, { w: 50, ic: 22 })}
        {rowBtn('play', '⏯', null, { bg: c.surface2, w: 50, ic: 22 })}
        {rowBtn(rtl ? 'rewind' : 'forward', '⏩', null, { w: 50, ic: 22 })}
      </div>
    </div>
  );
}

// ── more controls content ────────────────────────────
function MoreControls({ dev, pointer, onPointer, onSend }) {
  const { tk, tr: T } = useUI();
  const c = tk.colors;
  const tcol = transportColorOf(c, dev.transport);

  const sub = (label) => (
    <div style={{ fontSize: 12.5, fontWeight: 700, color: c.textFaint, letterSpacing: '.3px', margin: '4px 2px 10px' }}>{label}</div>
  );

  const funcs = [
    { icon: 'guide', key: 'fnGuide' },
    { icon: 'info2', key: 'fnInfo' },
    { icon: 'subtitles', key: 'fnSubs' },
    { icon: 'tv', key: 'fnChannels' },
    { icon: 'keyboard', key: 'fnKeyboard' },
    { icon: 'source', key: 'rmSource' },
  ];
  const colors = [
    { name: 'red', col: 'oklch(0.60 0.20 25)' },
    { name: 'green', col: 'oklch(0.62 0.16 150)' },
    { name: 'yellow', col: 'oklch(0.80 0.14 95)' },
    { name: 'blue', col: 'oklch(0.58 0.14 250)' },
  ];
  const nums = ['1', '2', '3', '4', '5', '6', '7', '8', '9'];

  return (
    <div>
      {/* pointer mode */}
      <div style={{
        display: 'flex', alignItems: 'center', gap: 12, padding: '13px 14px', marginBottom: 18,
        borderRadius: tk.radius.md, background: pointer ? c.accentSoft : c.bg2,
        border: `1.5px solid ${pointer ? c.accent : 'transparent'}`,
      }}>
        <span style={{
          width: 40, height: 40, borderRadius: tk.radius.sm, flexShrink: 0,
          background: c.surface, display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}><Icon name="pointer" size={21} stroke={pointer ? c.accent : c.textDim} strokeWidth={1.8} /></span>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 14.5, fontWeight: 700, color: c.text }}>{T('morePointer')}</div>
          <div style={{ fontSize: 12, color: c.textFaint, marginTop: 2, lineHeight: 1.45 }}>{T('morePointerHint')}</div>
        </div>
        <button onClick={() => onPointer(!pointer)} style={{
          width: 46, height: 28, borderRadius: 14, border: 'none', cursor: 'pointer', flexShrink: 0,
          background: pointer ? c.accent : c.lineStrong, position: 'relative', transition: 'background .2s',
        }}>
          <span style={{ position: 'absolute', top: 3, insetInlineStart: pointer ? 21 : 3, width: 22, height: 22, borderRadius: '50%', background: '#fff', transition: 'inset-inline-start .2s', boxShadow: '0 1px 3px rgba(0,0,0,.3)' }} />
        </button>
      </div>

      {/* number pad */}
      {sub(T('moreNumbers'))}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 8, marginBottom: 18 }}>
        {nums.map(n => (
          <button key={n} onClick={() => onSend(n)} style={{
            fontFamily: fontStack, fontSize: 20, fontWeight: 600, color: c.text, cursor: 'pointer',
            padding: '13px 0', borderRadius: tk.radius.md, background: c.surface,
            border: `${tk.hair}px solid ${c.line}`, boxShadow: tk.elev,
          }}>{n}</button>
        ))}
        <button onClick={() => onSend(T('fnDelete'))} style={{
          display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer',
          padding: '13px 0', borderRadius: tk.radius.md, background: c.bg2, border: 'none',
        }}><Icon name="backspace" size={22} stroke={c.textDim} strokeWidth={1.7} /></button>
        <button onClick={() => onSend('0')} style={{
          fontFamily: fontStack, fontSize: 20, fontWeight: 600, color: c.text, cursor: 'pointer',
          padding: '13px 0', borderRadius: tk.radius.md, background: c.surface,
          border: `${tk.hair}px solid ${c.line}`, boxShadow: tk.elev,
        }}>0</button>
        <button onClick={() => onSend('—')} style={{
          fontFamily: fontStack, fontSize: 20, fontWeight: 700, color: c.textDim, cursor: 'pointer',
          padding: '13px 0', borderRadius: tk.radius.md, background: c.bg2, border: 'none',
        }}>—</button>
      </div>

      {/* functions */}
      {sub(T('moreFunctions'))}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 8, marginBottom: 18 }}>
        {funcs.map(f => (
          <button key={f.key} onClick={() => onSend(T(f.key))} style={{
            fontFamily: fontStack, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 7,
            padding: '14px 4px', borderRadius: tk.radius.md, background: c.surface, cursor: 'pointer',
            border: `${tk.hair}px solid ${c.line}`, boxShadow: tk.elev,
          }}>
            <Icon name={f.icon} size={22} stroke={c.text} strokeWidth={1.7} />
            <span style={{ fontSize: 11.5, fontWeight: 600, color: c.textDim, textAlign: 'center', lineHeight: 1.2 }}>{T(f.key)}</span>
          </button>
        ))}
      </div>

      {/* color buttons */}
      {sub(T('moreColors'))}
      <div style={{ display: 'flex', gap: 10, marginBottom: 6 }}>
        {colors.map(cl => (
          <button key={cl.name} onClick={() => onSend('●')} aria-label={cl.name} style={{
            flex: 1, height: 44, borderRadius: tk.radius.md, cursor: 'pointer', border: 'none',
            background: cl.col, boxShadow: `0 4px 14px -6px ${cl.col}`,
          }} />
        ))}
      </div>
    </div>
  );
}

// ── gestural touchpad (with pointer / trackpad mode) ─
function TouchPad({ pointerMode, onDir, onOk, onClick }) {
  const { tk, tr: T } = useUI();
  const c = tk.colors;
  const ref = useRefR(null);
  const start = useRefR(null);
  const moved = useRefR(false);
  const [ripple, setRipple] = useStateR(null);
  const [flash, setFlash] = useStateR(null);
  const [cursor, setCursor] = useStateR({ x: 145, y: 145 });

  function down(e) {
    start.current = { x: e.clientX, y: e.clientY, cx: cursor.x, cy: cursor.y };
    moved.current = false;
    e.currentTarget.setPointerCapture(e.pointerId);
  }
  function move(e) {
    if (!start.current) return;
    const dx = e.clientX - start.current.x, dy = e.clientY - start.current.y;
    if (Math.hypot(dx, dy) > 4) moved.current = true;
    if (pointerMode) {
      setCursor({
        x: Math.max(20, Math.min(270, start.current.cx + dx)),
        y: Math.max(20, Math.min(270, start.current.cy + dy)),
      });
    }
  }
  function up(e) {
    if (!start.current) return;
    const r = ref.current.getBoundingClientRect();
    if (pointerMode) {
      if (!moved.current) { setRipple({ x: cursor.x, y: cursor.y, key: Date.now() }); onClick(); }
    } else {
      const dx = e.clientX - start.current.x, dy = e.clientY - start.current.y;
      const dist = Math.hypot(dx, dy);
      const lx = e.clientX - r.left, ly = e.clientY - r.top;
      setRipple({ x: lx, y: ly, key: Date.now() });
      if (dist < 22) onOk();
      else {
        const dir = Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? 'right' : 'left') : (dy > 0 ? 'down' : 'up');
        setFlash(dir); setTimeout(() => setFlash(null), 240); onDir(dir);
      }
    }
    start.current = null;
  }

  const edge = (dir, icon, pos) => (
    <span style={{ position: 'absolute', ...pos, opacity: pointerMode ? 0 : (flash === dir ? 1 : 0.5), transition: 'opacity .15s' }}>
      <Icon name={icon} size={22} stroke={flash === dir ? c.accent : c.textFaint} strokeWidth={2} />
    </span>
  );

  return (
    <div ref={ref} onPointerDown={down} onPointerMove={move} onPointerUp={up}
      style={{
        width: 290, height: 290, borderRadius: tk.soft ? 40 : '46%',
        background: `radial-gradient(circle at 50% 42%, ${c.surface2}, ${c.surface})`,
        border: `${tk.hair}px solid ${c.line}`, position: 'relative',
        boxShadow: `${tk.elev}, inset 0 1px 1px rgba(255,255,255,.04)`,
        touchAction: 'none', cursor: pointerMode ? 'crosshair' : 'grab', userSelect: 'none',
        display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden',
      }}>
      {edge('up', 'caretUp', { top: 14, left: '50%', transform: 'translateX(-50%)' })}
      {edge('down', 'caretDown', { bottom: 14, left: '50%', transform: 'translateX(-50%)' })}
      {edge('left', 'caretLeft', { left: 14, top: '50%', transform: 'translateY(-50%)' })}
      {edge('right', 'caretRight', { right: 14, top: '50%', transform: 'translateY(-50%)' })}

      {/* center OK (hidden in pointer mode) */}
      <div style={{
        width: 92, height: 92, borderRadius: '50%', background: c.bg,
        border: `1.5px solid ${c.line}`, display: 'flex', alignItems: 'center', justifyContent: 'center',
        pointerEvents: 'none', boxShadow: tk.elev, opacity: pointerMode ? 0 : 1, transition: 'opacity .2s',
      }}>
        <span style={{ fontSize: 16, fontWeight: 800, letterSpacing: '1px', color: c.text }}>OK</span>
      </div>

      {/* pointer cursor */}
      {pointerMode && (
        <span style={{ position: 'absolute', left: cursor.x, top: cursor.y, transform: 'translate(-2px,-2px)', pointerEvents: 'none', filter: 'drop-shadow(0 2px 3px rgba(0,0,0,.35))' }}>
          <Icon name="pointer" size={30} stroke={c.bg} fill={c.accent} strokeWidth={1.4} />
        </span>
      )}
      {pointerMode && (
        <span style={{ position: 'absolute', bottom: 16, left: 0, right: 0, textAlign: 'center', fontSize: 11.5, fontWeight: 600, color: c.textFaint, pointerEvents: 'none' }}>{T('morePointer')}</span>
      )}

      {ripple && (
        <span key={ripple.key} className="om-ripple" style={{
          position: 'absolute', left: ripple.x, top: ripple.y, width: 16, height: 16,
          marginLeft: -8, marginTop: -8, borderRadius: '50%', background: c.accent, pointerEvents: 'none',
        }} />
      )}
    </div>
  );
}

function Rocker({ icon, iconDown, label, onUp, onDown, mid }) {
  const { tk } = useUI();
  const c = tk.colors;
  const btn = (ic, fn) => (
    <button onClick={fn} style={{
      flex: 1, background: 'none', border: 'none', cursor: 'pointer', color: c.text,
      display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '12px 0',
      transition: 'background .15s',
    }}
      onMouseDown={e => (e.currentTarget.style.background = c.bg2)}
      onMouseUp={e => (e.currentTarget.style.background = 'transparent')}
      onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}>
      <Icon name={ic} size={22} stroke={c.text} strokeWidth={1.9} />
    </button>
  );
  return (
    <div style={{
      flex: 1, background: c.surface, border: `${tk.hair}px solid ${c.line}`,
      borderRadius: tk.radius.pill, boxShadow: tk.elev, display: 'flex', alignItems: 'center',
      overflow: 'hidden',
    }}>
      {btn(icon, onUp)}
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', minWidth: 44 }}>
        {mid || <span style={{ fontSize: 11, fontWeight: 700, color: c.textFaint, letterSpacing: '.3px' }}>{label}</span>}
      </div>
      {btn(iconDown, onDown)}
    </div>
  );
}

Object.assign(window, { RemoteScreen, MoreControls, TouchPad, Rocker });
