// app.jsx — controller: context, navigation, tweaks, viewport scaling
const { useState: useStateApp, useEffect: useEffectApp, useRef: useRefApp } = React;

const TWEAK_DEFAULTS = /*EDITMODE-BEGIN*/{
  "direction": "serene",
  "dark": false,
  "lang": "ar"
}/*EDITMODE-END*/;

function DirectionPicker({ value, dark, lang, onChange }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      {DIRECTIONS.map(d => {
        const tk = makeTokens(d.id, dark);
        const c = tk.colors;
        const on = value === d.id;
        return (
          <button key={d.id} onClick={() => onChange(d.id)} style={{
            display: 'flex', alignItems: 'center', gap: 11, padding: '10px 11px', cursor: 'pointer',
            borderRadius: 12, border: `1.5px solid ${on ? c.accent : 'rgba(125,125,135,.28)'}`,
            background: on ? 'rgba(125,125,135,.08)' : 'transparent', textAlign: 'start',
            fontFamily: "'IBM Plex Sans Arabic',system-ui", transition: 'all .15s',
          }}>
            {/* swatch */}
            <span style={{
              width: 42, height: 30, borderRadius: 7, flexShrink: 0, position: 'relative', overflow: 'hidden',
              background: c.bg, border: `1px solid ${c.line}`,
            }}>
              <span style={{ position: 'absolute', insetInlineStart: 5, top: 6, width: 16, height: 4, borderRadius: 2, background: c.text }} />
              <span style={{ position: 'absolute', insetInlineStart: 5, top: 13, width: 24, height: 4, borderRadius: 2, background: c.line }} />
              <span style={{ position: 'absolute', insetInlineEnd: 5, bottom: 5, width: 11, height: 11, borderRadius: '50%', background: c.accent }} />
            </span>
            <span style={{ flex: 1 }}>
              <span style={{ display: 'block', fontSize: 13.5, fontWeight: 700, color: '#e8e8ea' }}>{lang === 'ar' ? d.labelAr : d.labelEn}</span>
              <span style={{ display: 'block', fontSize: 11, color: 'rgba(200,200,210,.6)', marginTop: 1 }}>{lang === 'ar' ? d.descAr : d.descEn}</span>
            </span>
            {on && <span style={{ width: 8, height: 8, borderRadius: '50%', background: c.accent }} />}
          </button>
        );
      })}
    </div>
  );
}

function CommandToast({ toast }) {
  const { tk, tr: T } = useUI();
  const c = tk.colors;
  return (
    <div style={{
      position: 'absolute', top: 64, left: '50%', transform: `translateX(-50%) translateY(${toast ? 0 : -8}px)`,
      background: c.text, color: c.bg, padding: '8px 16px', borderRadius: 999,
      fontSize: 13.5, fontWeight: 700, opacity: toast ? 1 : 0, transition: 'opacity .2s, transform .2s',
      pointerEvents: 'none', zIndex: 60, display: 'flex', alignItems: 'center', gap: 7, whiteSpace: 'nowrap',
      boxShadow: '0 10px 30px -10px rgba(0,0,0,.5)',
    }}>
      <Icon name="check" size={15} stroke={c.bg} strokeWidth={2.5} />
      {T('rmSent')} · {String(toast || '')}
    </div>
  );
}

function App() {
  const [tw, setTweak] = useTweaks(TWEAK_DEFAULTS);
  const dir = tw.direction, dark = !!tw.dark, lang = tw.lang || 'ar';
  const rtl = lang === 'ar';
  const tk = makeTokens(dir, dark);
  const ctx = { tk, lang, rtl, tr: (k) => t(lang, k) };

  const [phase, setPhase] = useStateApp('onboarding');
  const [tab, setTab] = useStateApp('devices');
  const [device, setDevice] = useStateApp(DEVICES[0]);
  const [sheet, setSheet] = useStateApp(false);
  const [moreOpen, setMore] = useStateApp(false);
  const [pointer, setPointer] = useStateApp(false);
  const [toast, setToast] = useStateApp(null);
  const [beamKey, setBeam] = useStateApp(0);
  const toastT = useRefApp(null);

  function send(label) {
    setToast(label); setBeam(k => k + 1);
    clearTimeout(toastT.current);
    toastT.current = setTimeout(() => setToast(null), 1100);
  }

  // viewport scaling
  const [scale, setScale] = useStateApp(1);
  useEffectApp(() => {
    const fit = () => {
      const s = Math.min((window.innerWidth - 28) / 402, (window.innerHeight - 28) / 858, 1.08);
      setScale(s);
    };
    fit();
    window.addEventListener('resize', fit);
    return () => window.removeEventListener('resize', fit);
  }, []);

  function openDevice(d) { setSheet(false); setMore(false); setDevice(d); setTab('remote'); }
  function goTab(t) { setSheet(false); setMore(false); setTab(t); }

  let screen;
  if (phase === 'onboarding') {
    screen = <Onboarding onDone={() => setPhase('app')} />;
  } else if (tab === 'devices') {
    screen = <DevicesScreen onOpen={openDevice} onAdd={() => setTab('add')} />;
  } else if (tab === 'remote') {
    screen = <RemoteScreen device={device} onBack={() => goTab('devices')} onShowTransport={() => setSheet(true)}
      onOpenMore={() => setMore(true)} send={send} beamKey={beamKey} pointer={pointer} onPointer={setPointer} />;
  } else if (tab === 'add') {
    screen = <PairingScreen />;
  } else {
    screen = <SettingsScreen dark={dark} setDark={v => setTweak('dark', v)} lang={lang} setLang={v => setTweak('lang', v)} direction={dir} />;
  }

  const inApp = phase === 'app';

  return (
    <UICtx.Provider value={ctx}>
      <div style={{
        minHeight: '100vh', width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center',
        padding: 14, boxSizing: 'border-box',
      }}>
        <div style={{ transform: `scale(${scale})`, transformOrigin: 'center center', transition: 'transform .15s' }}>
          <PhoneFrame rtl={rtl}
            bottom={inApp ? <BottomNav tab={tab} onTab={goTab} /> : null}
            overlay={
              <React.Fragment>
                <Sheet open={sheet} onClose={() => setSheet(false)} title={ctx.tr('trTitle')}>
                  <TransportDetail device={device} onClose={() => setSheet(false)} />
                </Sheet>
                <Sheet open={moreOpen} onClose={() => setMore(false)} title={ctx.tr('moreTitle')}>
                  <MoreControls dev={device} pointer={pointer}
                    onPointer={(v) => { setPointer(v); if (v) setMore(false); }} onSend={send} />
                </Sheet>
                <CommandToast toast={toast} />
              </React.Fragment>
            }>
            <div key={phase + tab} className="om-screen" style={{ height: '100%', display: 'flex', flexDirection: 'column', minHeight: 0 }}>
              {screen}
            </div>
          </PhoneFrame>
        </div>
      </div>

      <TweaksPanel title="Tweaks">
        <TweakSection label={lang === 'ar' ? 'الاتجاه البصري' : 'Visual direction'} />
        <DirectionPicker value={dir} dark={dark} lang={lang} onChange={v => setTweak('direction', v)} />
        <TweakSection label={lang === 'ar' ? 'المظهر' : 'Appearance'} />
        <TweakToggle label={lang === 'ar' ? 'الوضع الداكن' : 'Dark mode'} value={dark} onChange={v => setTweak('dark', v)} />
        <TweakRadio label={lang === 'ar' ? 'اللغة' : 'Language'} value={lang}
          options={[{ value: 'ar', label: 'العربية' }, { value: 'en', label: 'English' }]}
          onChange={v => setTweak('lang', v)} />
        <TweakSection label={lang === 'ar' ? 'تنقّل سريع' : 'Quick jump'} />
        <TweakButton label={lang === 'ar' ? 'إعادة عرض الترحيب' : 'Replay onboarding'} onClick={() => { setPhase('onboarding'); }} />
      </TweaksPanel>
    </UICtx.Provider>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App />);
