// screens-c.jsx — Pairing, Transport detail sheet, Settings
const { useState: useStateC, useEffect: useEffectC, useRef: useRefC } = React;

// ── Pairing / discovery ──────────────────────────────
function PairingScreen({ onDone }) {
  const { tk, tr: T, lang } = useUI();
  const c = tk.colors;
  const [scanning, setScanning] = useStateC(true);
  const [found, setFound] = useStateC([]);
  const [state, setState] = useStateC({}); // id -> 'idle'|'connecting'|'paired'
  const timers = useRefC([]);

  function runScan() {
    setFound([]); setState({}); setScanning(true);
    timers.current.forEach(clearTimeout);
    timers.current = DISCOVERABLE.map((d, i) =>
      setTimeout(() => setFound(f => [...f, d]), 700 + i * 750));
    timers.current.push(setTimeout(() => setScanning(false), 700 + DISCOVERABLE.length * 750 + 400));
  }
  useEffectC(() => { runScan(); return () => timers.current.forEach(clearTimeout); }, []);

  function connect(d) {
    setState(s => ({ ...s, [d.id]: 'connecting' }));
    setTimeout(() => setState(s => ({ ...s, [d.id]: 'paired' })), 1400);
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', minHeight: 0 }}>
      <div style={{ padding: '8px 22px 4px' }}>
        <h1 style={{ margin: 0, fontSize: 27, fontWeight: 800, color: c.text, letterSpacing: lang === 'ar' ? 0 : '-.6px' }}>{T('pairTitle')}</h1>
      </div>

      {/* radar */}
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '20px 0 8px', flexShrink: 0 }}>
        <div style={{ width: 132, height: 132, position: 'relative', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          {scanning && [0, 1, 2].map(k => (
            <span key={k} className="om-pulse" style={{
              position: 'absolute', width: 132, height: 132, borderRadius: '50%',
              background: c.accentSoft, animationDelay: `${k * 0.9}s`,
            }} />
          ))}
          <div style={{
            width: 78, height: 78, borderRadius: '50%', background: c.surface,
            border: `${tk.hair}px solid ${c.line}`, boxShadow: tk.elev,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <Icon name={scanning ? 'scan' : 'check'} size={34} stroke={c.accent} strokeWidth={1.7} />
          </div>
        </div>
        <div style={{ fontSize: 16, fontWeight: 700, color: c.text, marginTop: 16 }}>
          {scanning ? T('pairScanning') : T('pairFound')}
        </div>
        <div style={{ fontSize: 12.5, color: c.textFaint, marginTop: 3 }}>{T('pairScanSub')}</div>
      </div>

      {/* found list */}
      <div style={{ flex: 1, overflowY: 'auto', padding: '14px 22px 16px', display: 'flex', flexDirection: 'column', gap: 11 }}>
        {found.map(d => {
          const tcol = transportColorOf(c, d.transport);
          const st = state[d.id] || 'idle';
          return (
            <Card key={d.id} pad={14} className="om-fade-up"
              style={{ display: 'flex', alignItems: 'center', gap: 13 }}>
              <div style={{
                width: 46, height: 46, borderRadius: tk.radius.md, flexShrink: 0,
                background: transportSoftOf(c, d.transport), display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>
                <Icon name={d.icon} size={23} stroke={tcol} strokeWidth={1.7} />
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 15, fontWeight: 700, color: c.text, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  {lang === 'ar' ? d.nameAr : d.nameEn}
                </div>
                <div style={{ fontSize: 12, color: c.textFaint, marginTop: 2, display: 'flex', alignItems: 'center', gap: 6 }}>
                  <Icon name={TRANSPORT_META[d.transport].icon} size={13} stroke={tcol} strokeWidth={2} />
                  {lang === 'ar' ? d.detailAr : d.detailEn}
                </div>
              </div>
              {st === 'paired'
                ? <Chip color={c.accent} bg={c.accentSoft} style={{ border: 'none' }}><Icon name="check" size={14} stroke={c.accent} strokeWidth={2.5} />{T('pairPaired')}</Chip>
                : <Btn size="sm" variant={st === 'connecting' ? 'soft' : 'solid'} disabled={st === 'connecting'} onClick={() => connect(d)}>
                    {st === 'connecting' ? T('pairConnecting') : T('pairConnect')}
                  </Btn>}
            </Card>
          );
        })}
        {!scanning && (
          <button onClick={runScan} style={{
            marginTop: 4, fontFamily: fontStack, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
            padding: '13px', borderRadius: tk.radius.lg, cursor: 'pointer',
            border: `1.5px solid ${c.line}`, background: 'transparent', color: c.textDim, fontSize: 14, fontWeight: 600,
          }}>
            <Icon name="scan" size={17} stroke={c.textDim} /> {T('pairAgain')}
          </button>
        )}
      </div>
    </div>
  );
}

// ── transport detail (sheet content) ─────────────────
function TransportDetail({ device, onClose }) {
  const { tk, tr: T, lang } = useUI();
  const c = tk.colors;
  const dev = device || DEVICES[0];
  const [sel, setSel] = useStateC(dev.transport);

  return (
    <div>
      {/* auto banner */}
      <div style={{
        display: 'flex', gap: 12, padding: '13px 14px', borderRadius: tk.radius.md,
        background: c.accentSoft, marginBottom: 16,
      }}>
        <Icon name="shield" size={20} stroke={c.accent} strokeWidth={1.8} style={{ flexShrink: 0, marginTop: 1 }} />
        <div>
          <div style={{ fontSize: 13.5, fontWeight: 700, color: c.accent }}>{T('trAutoTitle')}</div>
          <div style={{ fontSize: 12.8, color: c.textDim, lineHeight: 1.5, marginTop: 3 }}>{T('trAutoBody')}</div>
        </div>
      </div>

      {/* options */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {TRANSPORTS.map(tr => {
          const meta = TRANSPORT_META[tr];
          const tcol = transportColorOf(c, tr);
          const path = dev.paths[tr];
          const na = path === 'na';
          const active = sel === tr;
          return (
            <button key={tr} disabled={na} onClick={() => setSel(tr)}
              style={{
                textAlign: 'start', fontFamily: fontStack, cursor: na ? 'default' : 'pointer',
                display: 'flex', alignItems: 'center', gap: 13, padding: '13px 14px',
                borderRadius: tk.radius.md, background: active ? transportSoftOf(c, tr) : c.surface,
                border: `1.5px solid ${active ? tcol : c.line}`, opacity: na ? 0.45 : 1,
                transition: 'all .18s',
              }}>
              <div style={{
                width: 42, height: 42, borderRadius: tk.radius.sm, flexShrink: 0,
                background: transportSoftOf(c, tr), display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>
                <Icon name={meta.icon} size={22} stroke={tcol} strokeWidth={1.9} />
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 15, fontWeight: 700, color: c.text }}>{T(meta.key)}</div>
                <div style={{ fontSize: 12, color: c.textFaint, marginTop: 2, lineHeight: 1.4 }}>
                  {na ? T('trUnavailable') : T(meta.descKey)}
                </div>
              </div>
              {path === 'active' && <Dot color={tcol} size={9} glow />}
              {active && <Icon name="check" size={20} stroke={tcol} strokeWidth={2.4} />}
            </button>
          );
        })}
      </div>

      {/* metrics for selected */}
      <div style={{ marginTop: 16, padding: '14px 16px', borderRadius: tk.radius.md, background: c.bg2 }}>
        <div style={{ fontSize: 13, fontWeight: 700, color: c.text, marginBottom: 12 }}>{T(TRANSPORT_META[sel].key)}</div>
        {[['trLatency', 'latency'], ['trReliab', 'reliab'], ['trRange', 'range']].map(([k, key]) => (
          <div key={key} style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 9 }}>
            <span style={{ fontSize: 12.5, color: c.textDim, width: 78, flexShrink: 0 }}>{T(k)}</span>
            <span style={{ flex: 1, height: 6, borderRadius: 3, background: c.line, overflow: 'hidden' }}>
              <span style={{ display: 'block', height: '100%', width: `${TRANSPORT_META[sel][key] * 100}%`, background: transportColorOf(c, sel), borderRadius: 3 }} />
            </span>
          </div>
        ))}
      </div>

      {/* why */}
      <div style={{ marginTop: 14, display: 'flex', gap: 10 }}>
        <Icon name="info" size={18} stroke={c.textFaint} style={{ flexShrink: 0, marginTop: 1 }} />
        <div style={{ fontSize: 12.8, color: c.textDim, lineHeight: 1.55 }}>{T('trWhyBody')}</div>
      </div>

      <Btn full size="lg" style={{ marginTop: 18 }} onClick={onClose}>{T('trActive')}</Btn>
    </div>
  );
}

// ── Settings ─────────────────────────────────────────
function SettingsScreen({ dark, setDark, lang, setLang, direction }) {
  const { tk, tr: T } = useUI();
  const c = tk.colors;

  const Row = ({ icon, label, sub, right, onClick, last }) => (
    <div onClick={onClick} style={{
      display: 'flex', alignItems: 'center', gap: 13, padding: '13px 2px', cursor: onClick ? 'pointer' : 'default',
      borderBottom: last ? 'none' : `${tk.hair}px solid ${c.line}`,
    }}>
      {icon && <span style={{
        width: 38, height: 38, borderRadius: tk.radius.sm, background: c.bg2, flexShrink: 0,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}><Icon name={icon} size={20} stroke={c.textDim} strokeWidth={1.8} /></span>}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 15, fontWeight: 600, color: c.text }}>{label}</div>
        {sub && <div style={{ fontSize: 12.5, color: c.textFaint, marginTop: 2 }}>{sub}</div>}
      </div>
      {right}
    </div>
  );

  const Seg = ({ options, value, onChange }) => (
    <div style={{ display: 'flex', background: c.bg2, borderRadius: tk.radius.pill, padding: 3, gap: 2 }}>
      {options.map(o => {
        const on = value === o.v;
        return (
          <button key={o.v} onClick={() => onChange(o.v)} style={{
            fontFamily: fontStack, fontSize: 13, fontWeight: on ? 700 : 600, cursor: 'pointer',
            padding: '7px 14px', borderRadius: tk.radius.pill, border: 'none',
            background: on ? c.surface : 'transparent', color: on ? c.text : c.textFaint,
            boxShadow: on ? tk.elev : 'none', transition: 'all .2s', display: 'flex', alignItems: 'center', gap: 6,
          }}>{o.icon && <Icon name={o.icon} size={15} stroke={on ? c.accent : c.textFaint} strokeWidth={2} />}{o.label}</button>
        );
      })}
    </div>
  );

  const Toggle = ({ on, onClick }) => (
    <button onClick={onClick} style={{
      width: 46, height: 28, borderRadius: 14, border: 'none', cursor: 'pointer', flexShrink: 0,
      background: on ? c.accent : c.lineStrong, position: 'relative', transition: 'background .2s',
    }}>
      <span style={{
        position: 'absolute', top: 3, insetInlineStart: on ? 21 : 3, width: 22, height: 22, borderRadius: '50%',
        background: '#fff', transition: 'inset-inline-start .2s', boxShadow: '0 1px 3px rgba(0,0,0,.3)',
      }} />
    </button>
  );

  const [bridgeOn, setBridgeOn] = useStateC(true);
  const [netOn, setNetOn] = useStateC(true);

  const section = (title, children) => (
    <div style={{ marginBottom: 22 }}>
      <div style={{ fontSize: 12.5, fontWeight: 700, color: c.textFaint, letterSpacing: '.4px', textTransform: 'uppercase', padding: '0 2px 6px' }}>{title}</div>
      <Card pad="2px 16px">{children}</Card>
    </div>
  );

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', minHeight: 0 }}>
      <div style={{ padding: '8px 22px 10px' }}>
        <h1 style={{ margin: 0, fontSize: 27, fontWeight: 800, color: c.text, letterSpacing: lang === 'ar' ? 0 : '-.6px' }}>{T('setTitle')}</h1>
      </div>
      <div style={{ flex: 1, overflowY: 'auto', padding: '6px 22px 22px' }}>
        <Card pad={16} style={{ display: 'flex', alignItems: 'center', gap: 14, marginBottom: 22 }}>
          <Logo size={52} glow />
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 17, fontWeight: 800, color: c.text }}>GT-TAHAKOM</div>
            <div style={{ fontSize: 13, color: c.textFaint, marginTop: 2 }}>{T('appName')} · {T('appTag')}</div>
          </div>
          <Chip color={c.accent} bg={c.accentSoft} style={{ border: 'none' }}>v1.0</Chip>
        </Card>
        {section(T('setAppearance'), <>
          <Row icon={dark ? 'moon' : 'sun'} label={T('setTheme')} right={
            <Seg value={dark ? 'd' : 'l'} onChange={v => setDark(v === 'd')}
              options={[{ v: 'l', label: T('setLight'), icon: 'sun' }, { v: 'd', label: T('setDark'), icon: 'moon' }]} />
          } />
          <Row icon="globe" label={T('setLang')} last right={
            <Seg value={lang} onChange={setLang}
              options={[{ v: 'ar', label: 'عربي' }, { v: 'en', label: 'EN' }]} />
          } />
        </>)}

        {section(T('setTransports'), <>
          <Row icon="bridge" label={T('setBridgeIp')} sub="192.168.1.42 · Broadlink RM4" right={<Toggle on={bridgeOn} onClick={() => setBridgeOn(v => !v)} />} />
          <Row icon="ir" label={T('setIrEmitter')} sub={T('setIrNone')} right={<Chip>{T('setOff')}</Chip>} />
          <Row icon="wifi" label={T('setNetwork')} last right={<Toggle on={netOn} onClick={() => setNetOn(v => !v)} />} />
        </>)}

        {section(T('setAbout'), <>
          <Row icon="info" label={T('setVersion')} right={<span style={{ fontSize: 14, fontWeight: 600, color: c.textFaint }}>1.0 · م0</span>} />
          <Row icon="shield" label={T('setLicense')} sub={T('setLicenseBody')} last right={<Chip color={c.accent} bg={c.accentSoft} style={{ border: 'none' }}>GPLv3</Chip>} />
        </>)}

        <div style={{ textAlign: 'center', fontSize: 12, color: c.textFaint, marginTop: 6 }}>
          {T('appName')} — {T('appTag')}
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { PairingScreen, TransportDetail, SettingsScreen });
