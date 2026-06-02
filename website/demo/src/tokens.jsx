// tokens.jsx — design token system
// 3 visual directions × {light, dark}. Each returns a flat token object.
// Colors are oklch for harmonious, low-chroma "calm/luxurious" feel.
// Exports: DIRECTIONS, makeTokens(direction, dark)  → window

const DIRECTIONS = [
  { id: 'serene',    labelAr: 'هدوء',   labelEn: 'Serene',    descAr: 'مادي مكرّر، أسطح ناعمة', descEn: 'Refined Material, soft surfaces' },
  { id: 'editorial', labelAr: 'تحرير',  labelEn: 'Editorial', descAr: 'مساحات واسعة، خطوط رفيعة', descEn: 'Generous space, hairlines' },
  { id: 'aurora',    labelAr: 'طبقات',  labelEn: 'Aurora',    descAr: 'عمق زجاجي متدرّج', descEn: 'Layered glass depth' },
];

// shared transport semantics — aligned to the GT-TAHAKOM brand icon
// WiFi = cyan/teal · IR = red · WiFi-IR bridge = amber/orange
function transportColors(dark) {
  return {
    wifi:   dark ? 'oklch(0.80 0.13 195)' : 'oklch(0.60 0.12 200)',
    ir:     dark ? 'oklch(0.70 0.18 28)'  : 'oklch(0.58 0.19 27)',
    bridge: dark ? 'oklch(0.79 0.14 68)'  : 'oklch(0.64 0.14 64)',
    wifiSoft:   dark ? 'oklch(0.80 0.13 195 / 0.16)' : 'oklch(0.60 0.12 200 / 0.12)',
    irSoft:     dark ? 'oklch(0.70 0.18 28 / 0.16)'  : 'oklch(0.58 0.19 27 / 0.10)',
    bridgeSoft: dark ? 'oklch(0.79 0.14 68 / 0.16)'  : 'oklch(0.64 0.14 64 / 0.12)',
  };
}

function makeTokens(direction = 'serene', dark = false) {
  const T = transportColors(dark);
  let c;

  if (direction === 'editorial') {
    // warm paper / near-black ink, bronze accent, hairline-driven
    c = dark ? {
      bg:'oklch(0.165 0.006 70)', bg2:'oklch(0.185 0.006 70)',
      surface:'oklch(0.205 0.007 70)', surface2:'oklch(0.235 0.008 70)',
      text:'oklch(0.95 0.006 80)', textDim:'oklch(0.74 0.008 75)', textFaint:'oklch(0.56 0.008 75)',
      line:'oklch(0.32 0.008 75)', lineStrong:'oklch(0.42 0.01 75)',
      accent:'oklch(0.81 0.11 82)', accentText:'oklch(0.19 0.03 75)', accentSoft:'oklch(0.81 0.11 82 / 0.16)',
    } : {
      bg:'oklch(0.975 0.006 82)', bg2:'oklch(0.965 0.007 82)',
      surface:'oklch(0.998 0.003 82)', surface2:'oklch(0.985 0.005 82)',
      text:'oklch(0.23 0.012 65)', textDim:'oklch(0.46 0.012 68)', textFaint:'oklch(0.62 0.01 70)',
      line:'oklch(0.89 0.008 75)', lineStrong:'oklch(0.80 0.01 75)',
      accent:'oklch(0.60 0.10 80)', accentText:'oklch(0.99 0.005 90)', accentSoft:'oklch(0.60 0.10 80 / 0.10)',
    };
    return base(direction, dark, c, T, { sm:8, md:12, lg:16, pill:999, gut:22, cardPad:20, elev:'none', hair:1, soft:false, display:760 });
  }

  if (direction === 'aurora') {
    // cool indigo, violet accent, layered soft elevation + glass
    c = dark ? {
      bg:'oklch(0.165 0.022 278)', bg2:'oklch(0.20 0.026 278)',
      surface:'oklch(0.235 0.028 278)', surface2:'oklch(0.275 0.03 278)',
      text:'oklch(0.96 0.01 280)', textDim:'oklch(0.76 0.018 280)', textFaint:'oklch(0.58 0.02 280)',
      line:'oklch(0.36 0.03 280)', lineStrong:'oklch(0.46 0.035 280)',
      accent:'oklch(0.81 0.13 192)', accentText:'oklch(0.15 0.03 200)', accentSoft:'oklch(0.81 0.13 192 / 0.18)',
    } : {
      bg:'oklch(0.97 0.012 280)', bg2:'oklch(0.955 0.016 280)',
      surface:'oklch(0.995 0.006 280)', surface2:'oklch(0.985 0.01 280)',
      text:'oklch(0.26 0.03 282)', textDim:'oklch(0.47 0.03 282)', textFaint:'oklch(0.63 0.025 282)',
      line:'oklch(0.90 0.014 282)', lineStrong:'oklch(0.82 0.02 282)',
      accent:'oklch(0.60 0.12 198)', accentText:'oklch(0.99 0.005 200)', accentSoft:'oklch(0.60 0.12 198 / 0.10)',
    };
    return base(direction, dark, c, T, {
      sm:14, md:22, lg:30, pill:999, gut:18, cardPad:18,
      elev: dark ? '0 10px 30px -12px oklch(0.10 0.04 280 / 0.7)' : '0 14px 34px -16px oklch(0.55 0.12 285 / 0.30)',
      hair:1, soft:true, display:680,
    });
  }

  // serene (default) — calm teal-blue, tonal rounded surfaces
  c = dark ? {
    bg:'oklch(0.185 0.012 248)', bg2:'oklch(0.215 0.014 248)',
    surface:'oklch(0.245 0.016 248)', surface2:'oklch(0.285 0.018 248)',
    text:'oklch(0.96 0.008 240)', textDim:'oklch(0.76 0.012 240)', textFaint:'oklch(0.58 0.013 240)',
    line:'oklch(0.35 0.016 245)', lineStrong:'oklch(0.45 0.018 245)',
    accent:'oklch(0.80 0.11 195)', accentText:'oklch(0.18 0.03 200)', accentSoft:'oklch(0.80 0.11 195 / 0.16)',
  } : {
    bg:'oklch(0.975 0.006 235)', bg2:'oklch(0.96 0.008 235)',
    surface:'oklch(0.998 0.003 235)', surface2:'oklch(0.985 0.006 235)',
    text:'oklch(0.25 0.02 245)', textDim:'oklch(0.47 0.02 245)', textFaint:'oklch(0.63 0.018 245)',
    line:'oklch(0.91 0.008 240)', lineStrong:'oklch(0.83 0.012 240)',
    accent:'oklch(0.58 0.10 202)', accentText:'oklch(0.99 0.004 200)', accentSoft:'oklch(0.58 0.10 202 / 0.10)',
  };
  return base(direction, dark, c, T, {
    sm:12, md:18, lg:26, pill:999, gut:18, cardPad:18,
    elev: dark ? '0 8px 24px -14px oklch(0.10 0.02 245 / 0.8)' : '0 8px 22px -16px oklch(0.45 0.05 245 / 0.25)',
    hair:1, soft:false, display:640,
  });
}

function base(direction, dark, c, T, shape) {
  return {
    direction, dark,
    colors: { ...c, ...T },
    radius: { sm: shape.sm, md: shape.md, lg: shape.lg, pill: shape.pill },
    space: shape.gut,
    cardPad: shape.cardPad,
    elev: shape.elev,
    hair: shape.hair,
    soft: shape.soft,
    displayWeight: shape.display,
    // status-bar + nav use text color
  };
}

Object.assign(window, { DIRECTIONS, makeTokens, transportColors });
