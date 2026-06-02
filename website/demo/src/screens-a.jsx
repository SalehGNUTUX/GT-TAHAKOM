// screens-a.jsx — Onboarding + Devices list
const { useState: useStateA, useEffect: useEffectA } = React;

// ── Onboarding ───────────────────────────────────────
function Onboarding({ onDone }) {
  const { tk, tr: T, lang, rtl } = useUI();
  const c = tk.colors;
  const [i, setI] = useStateA(0);
  const slides = [
    { key: '1', icon: 'tv',    visual: 'rings' },
    { key: '2', icon: 'scan',  visual: 'scan' },
    { key: '3', icon: 'link',  visual: 'paths' },
  ];
  const s = slides[i];
  const last = i === slides.length - 1;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', padding: '4px 26px 26px' }}>
      {/* top bar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', minHeight: 40 }}>
        <span style={{ display: 'flex', alignItems: 'center', gap: 9 }}>
          <Logo size={26} glow />
          <span style={{ fontSize: 15, fontWeight: 800, letterSpacing: '.5px', color: c.text }}>{T('appName')}</span>
        </span>
        {!last && <button onClick={onDone} style={{ background: 'none', border: 'none', color: c.textFaint, fontSize: 14, fontWeight: 600, cursor: 'pointer', fontFamily: fontStack }}>{T('obSkip')}</button>}
      </div>

      {/* visual */}
      <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: 0 }}>
        <OnboardVisual kind={s.visual} />
      </div>

      {/* copy */}
      <div style={{ minHeight: 178 }}>
        <h2 style={{
          margin: '0 0 12px', fontSize: 28, lineHeight: 1.22, fontWeight: 800,
          color: c.text, letterSpacing: lang === 'ar' ? 0 : '-.5px', textWrap: 'balance',
        }}>{T('ob' + (i + 1) + 'Title')}</h2>
        <p style={{ margin: 0, fontSize: 16, lineHeight: 1.65, color: c.textDim, textWrap: 'pretty', maxWidth: 320 }}>
          {T('ob' + (i + 1) + 'Body')}
        </p>
      </div>

      {/* footer */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 22 }}>
        <div style={{ display: 'flex', gap: 7 }}>
          {slides.map((_, k) => (
            <span key={k} onClick={() => setI(k)} style={{
              width: k === i ? 24 : 8, height: 8, borderRadius: 4, cursor: 'pointer',
              background: k === i ? c.accent : c.lineStrong, transition: 'width .25s, background .25s',
            }} />
          ))}
        </div>
        <Btn size="lg" onClick={() => last ? onDone() : setI(i + 1)}>
          {last ? T('obStart') : T('obNext')}
          <Icon name={rtl ? 'caretLeft' : 'caretRight'} size={18} stroke={c.accentText} strokeWidth={2.2} />
        </Btn>
      </div>
    </div>
  );
}

function OnboardVisual({ kind }) {
  const { tk } = useUI();
  const c = tk.colors;
  const box = { width: 250, height: 250, position: 'relative', display: 'flex', alignItems: 'center', justifyContent: 'center' };
  const center = (icon, col) => (
    <div style={{
      width: 96, height: 96, borderRadius: tk.soft ? 30 : '34%',
      background: c.surface, border: `${tk.hair}px solid ${c.line}`,
      boxShadow: tk.elev, display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 3,
    }}>
      <Icon name={icon} size={42} stroke={col || c.accent} strokeWidth={1.6} />
    </div>
  );

  if (kind === 'rings') {
    return (
      <div style={box}>
        {[150, 200, 248].map((d, k) => (
          <span key={k} className="om-ring" style={{
            position: 'absolute', width: d, height: d, borderRadius: '50%',
            border: `${tk.hair}px solid ${c.line}`, animationDelay: `${k * 0.6}s`,
          }} />
        ))}
        {center('tv')}
      </div>
    );
  }
  if (kind === 'scan') {
    return (
      <div style={box}>
        {[0, 1, 2].map(k => (
          <span key={k} className="om-pulse" style={{
            position: 'absolute', width: 110, height: 110, borderRadius: '50%',
            background: c.accentSoft, animationDelay: `${k * 0.9}s`,
          }} />
        ))}
        {center('scan')}
      </div>
    );
  }
  // paths: phone in center, three transports orbiting
  const orbs = [
    { tr: 'wifi', x: 0, y: -96 },
    { tr: 'ir', x: -88, y: 60 },
    { tr: 'bridge', x: 88, y: 60 },
  ];
  return (
    <div style={box}>
      <svg width="250" height="250" style={{ position: 'absolute', inset: 0 }}>
        {orbs.map((o, k) => (
          <line key={k} x1="125" y1="125" x2={125 + o.x} y2={125 + o.y}
            stroke={c.line} strokeWidth="1.5" strokeDasharray="3 5" className="om-dash" />
        ))}
      </svg>
      {center('homeBtn')}
      {orbs.map((o, k) => {
        const col = transportColorOf(c, o.tr);
        return (
          <div key={k} style={{
            position: 'absolute', left: '50%', top: '50%',
            transform: `translate(${o.x - 29}px, ${o.y - 29}px)`,
          }}>
            <div className="om-float" style={{ animationDelay: `${k * 0.5}s` }}>
              <div style={{
                width: 58, height: 58, borderRadius: '50%', background: transportSoftOf(c, o.tr),
                border: `1.5px solid ${col}`, display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>
                <Icon name={TRANSPORT_META[o.tr].icon} size={26} stroke={col} strokeWidth={1.9} />
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}

// ── Devices list ─────────────────────────────────────
function DevicesScreen({ onOpen, onAdd }) {
  const { tk, tr: T, lang, rtl } = useUI();
  const c = tk.colors;
  const [room, setRoom] = useStateA('all');
  const rooms = ['all', ...Array.from(new Set(DEVICES.map(d => (lang === 'ar' ? d.roomAr : d.roomEn))))];
  const list = room === 'all' ? DEVICES : DEVICES.filter(d => (lang === 'ar' ? d.roomAr : d.roomEn) === room);

  const statusOf = (d) => {
    if (d.status === 'connected') return { label: T('devConnected'), color: c.accent };
    if (d.status === 'ready') return { label: T('devReady'), color: transportColorOf(c, d.transport) };
    return { label: T('devOffline'), color: c.textFaint };
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', minHeight: 0 }}>
      {/* header */}
      <div style={{ padding: '8px 22px 4px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 7 }}>
              <Logo size={18} glow />
              <span style={{ fontSize: 13, fontWeight: 600, color: c.textFaint, letterSpacing: '.3px' }}>{T('appName')}</span>
            </div>
            <h1 style={{ margin: '4px 0 0', fontSize: 27, fontWeight: 800, color: c.text, letterSpacing: lang === 'ar' ? 0 : '-.6px' }}>{T('devTitle')}</h1>
          </div>
          <IconBtn name="search" variant="surface" />
        </div>
      </div>

      {/* auto hint */}
      <div style={{ padding: '12px 22px 4px' }}>
        <div style={{
          display: 'flex', alignItems: 'center', gap: 10, padding: '11px 14px',
          borderRadius: tk.radius.md, background: c.accentSoft,
        }}>
          <Icon name="shield" size={18} stroke={c.accent} strokeWidth={1.8} />
          <span style={{ fontSize: 12.8, fontWeight: 600, color: c.accent, lineHeight: 1.4 }}>{T('devAutoHint')}</span>
        </div>
      </div>

      {/* room filter */}
      <div style={{ display: 'flex', gap: 8, overflowX: 'auto', padding: '12px 22px 12px', flexShrink: 0, scrollbarWidth: 'none' }}>
        {rooms.map(r => {
          const on = room === r;
          return (
            <button key={r} onClick={() => setRoom(r)} style={{
              flexShrink: 0, fontFamily: fontStack, fontSize: 13.5, fontWeight: on ? 700 : 600,
              padding: '8px 16px', borderRadius: tk.radius.pill, cursor: 'pointer',
              border: `${tk.hair}px solid ${on ? 'transparent' : c.line}`,
              background: on ? c.text : 'transparent', color: on ? c.bg : c.textDim,
              transition: 'all .2s',
            }}>{r === 'all' ? T('devAll') : r}</button>
          );
        })}
      </div>

      {/* list */}
      <div style={{ flex: 1, overflowY: 'auto', padding: '0 22px 22px', display: 'flex', flexDirection: 'column', gap: 12 }}>
        {list.map(d => {
          const st = statusOf(d);
          const tcol = transportColorOf(c, d.transport);
          const off = d.status === 'offline';
          return (
            <Card key={d.id} onClick={() => onOpen(d)} pad={16}
              style={{ display: 'flex', alignItems: 'center', gap: 14, opacity: off ? 0.62 : 1 }}>
              <div style={{
                width: 52, height: 52, borderRadius: tk.radius.md, flexShrink: 0,
                background: transportSoftOf(c, d.transport),
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>
                <Icon name={d.icon} size={26} stroke={tcol} strokeWidth={1.7} />
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 16, fontWeight: 700, color: c.text, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  {lang === 'ar' ? d.nameAr : d.nameEn}
                </div>
                <div style={{ fontSize: 13, color: c.textFaint, marginTop: 2 }}>
                  {d.brand} · {lang === 'ar' ? d.platformAr : d.platformEn}
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 9 }}>
                  <TransportChip tr={d.transport} size="sm" />
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: 5, fontSize: 12, fontWeight: 600, color: st.color }}>
                    <Dot color={st.color} size={7} glow={d.status === 'connected'} />
                    {st.label}
                  </span>
                </div>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10 }}>
                {!off && <SignalDots level={d.signal} color={tcol} />}
                <Icon name={rtl ? 'caretLeft' : 'caretRight'} size={18} stroke={c.textFaint} />
              </div>
            </Card>
          );
        })}
        <button onClick={onAdd} style={{
          marginTop: 2, fontFamily: fontStack, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
          padding: '14px', borderRadius: tk.radius.lg, cursor: 'pointer',
          border: `1.5px dashed ${c.lineStrong}`, background: 'transparent', color: c.textDim,
          fontSize: 14.5, fontWeight: 600,
        }}>
          <Icon name="plus" size={18} stroke={c.textDim} /> {T('pairTitle')}
        </button>
      </div>
    </div>
  );
}

Object.assign(window, { Onboarding, DevicesScreen });
