// icons.jsx — refined line-icon set (stroke = currentColor)
// One <Icon name=… size=… stroke=…/> component. Paths are minimal & geometric
// to suit the calm/luxurious aesthetic. Exports Icon to window.

const ICON_PATHS = {
  // navigation / chrome
  back:      <path d="M15 5l-7 7 7 7" />,
  forwardNav:<path d="M9 5l7 7-7 7" />,
  chevron:   <path d="M9 6l6 6-6 6" />,
  close:     <path d="M6 6l12 12M18 6L6 18" />,
  plus:      <path d="M12 5v14M5 12h14" />,
  check:     <path d="M5 12.5l4.5 4.5L19 7" />,
  search:    <g><circle cx="11" cy="11" r="6.5" /><path d="M20 20l-4-4" /></g>,
  more:      <g><circle cx="12" cy="5" r="1.4" /><circle cx="12" cy="12" r="1.4" /><circle cx="12" cy="19" r="1.4" /></g>,
  gear:      <g><circle cx="12" cy="12" r="3.2" /><path d="M12 2.5v3M12 18.5v3M21.5 12h-3M5.5 12h-3M18.7 5.3l-2.1 2.1M7.4 16.6l-2.1 2.1M18.7 18.7l-2.1-2.1M7.4 7.4L5.3 5.3" /></g>,
  info:      <g><circle cx="12" cy="12" r="9" /><path d="M12 11v5M12 7.6v.1" /></g>,
  globe:     <g><circle cx="12" cy="12" r="9" /><path d="M3 12h18M12 3c2.6 2.4 2.6 15.6 0 18M12 3c-2.6 2.4-2.6 15.6 0 18" /></g>,
  sun:       <g><circle cx="12" cy="12" r="4" /><path d="M12 2.5v2.5M12 19v2.5M2.5 12H5M19 12h2.5M5.6 5.6l1.8 1.8M16.6 16.6l1.8 1.8M18.4 5.6l-1.8 1.8M7.4 16.6l-1.8 1.8" /></g>,
  moon:      <path d="M20 14.5A8 8 0 119.5 4a6.5 6.5 0 0010.5 10.5z" />,

  // remote glyphs
  power:     <g><path d="M12 3.5v8" /><path d="M6.6 7A8 8 0 1017.4 7" /></g>,
  homeBtn:   <path d="M4 11.5L12 5l8 6.5M6 10v9h12v-9" />,
  caretUp:   <path d="M7 14l5-5 5 5" />,
  caretDown: <path d="M7 10l5 5 5-5" />,
  caretLeft: <path d="M14 7l-5 5 5 5" />,
  caretRight:<path d="M10 7l5 5-5 5" />,
  volUp:     <g><path d="M4 9.5v5h3l4.5 3.5v-12L7 9.5H4z" /><path d="M16 8.5a5 5 0 010 7M18.5 6a8.5 8.5 0 010 12" /></g>,
  volDown:   <g><path d="M4 9.5v5h3l4.5 3.5v-12L7 9.5H4z" /><path d="M16 9.5a4 4 0 010 5" /></g>,
  mute:      <g><path d="M4 9.5v5h3l4.5 3.5v-12L7 9.5H4z" /><path d="M15.5 9.5l5 5M20.5 9.5l-5 5" /></g>,
  play:      <path d="M7 5l12 7-12 7z" />,
  pause:     <g><path d="M9 5v14M15 5v14" /></g>,
  rewind:    <g><path d="M11 6l-7 6 7 6zM20 6l-7 6 7 6z" /></g>,
  forward:   <g><path d="M13 6l7 6-7 6zM4 6l7 6-7 6z" /></g>,
  mic:       <g><rect x="9" y="3" width="6" height="11" rx="3" /><path d="M5.5 11.5a6.5 6.5 0 0013 0M12 18v3" /></g>,
  source:    <g><rect x="3" y="5" width="18" height="13" rx="2" /><path d="M8 21h8" /></g>,
  channelUp: <path d="M7 14l5-5 5 5" />,
  menu:      <path d="M4 7h16M4 12h16M4 17h16" />,
  pointer:   <path d="M6 3l13 7.5-5.6 1.4-1.4 5.6z" />,
  keyboard:  <g><rect x="3" y="6" width="18" height="12" rx="2" /><path d="M7 10v.01M11 10v.01M15 10v.01M8 14h8" /></g>,
  guide:     <g><rect x="3" y="4" width="18" height="16" rx="2" /><path d="M3 9h18M9 9v11" /></g>,
  subtitles: <g><rect x="3" y="5" width="18" height="14" rx="2" /><path d="M7 11h4M7 14.5h7M14 11h3" /></g>,
  backspace: <g><path d="M8 5h12v14H8l-5-7z" /><path d="M15 10l-4 4M11 10l4 4" /></g>,
  info2:     <g><rect x="3" y="4" width="18" height="16" rx="2" /><path d="M12 15v-4M12 8.5v.1" /></g>,

  // device / transport
  tv:        <g><rect x="3" y="5" width="18" height="12" rx="2" /><path d="M8 21h8M12 17v4" /></g>,
  ac:        <g><rect x="3" y="6" width="18" height="7" rx="2" /><path d="M6 17v.5M10 18v.5M14 17v.5M18 18v.5" /></g>,
  receiver:  <g><rect x="3" y="8" width="18" height="8" rx="1.5" /><circle cx="7" cy="12" r="1" /><path d="M11 12h6" /></g>,
  speaker:   <g><rect x="6" y="3" width="12" height="18" rx="2" /><circle cx="12" cy="14" r="3.4" /><circle cx="12" cy="6.5" r="1" /></g>,
  wifi:      <g><path d="M2.5 9a14 14 0 0119 0M5.5 12.5a9.5 9.5 0 0113 0M8.5 16a5 5 0 017 0" /><circle cx="12" cy="19.3" r="1" /></g>,
  ir:        <g><path d="M12 12v.01" /><path d="M8.5 8.5a5 5 0 000 7M15.5 8.5a5 5 0 010 7M6 6a9 9 0 000 12M18 6a9 9 0 010 12" /></g>,
  bridge:    <g><circle cx="12" cy="12" r="2.2" /><path d="M7 12a5 5 0 015-5M17 12a5 5 0 01-5 5M4 12a8 8 0 018-8M20 12a8 8 0 01-8 8" /></g>,
  link:      <g><path d="M9 12h6" /><path d="M10 8H8a4 4 0 000 8h2M14 8h2a4 4 0 010 8h-2" /></g>,
  signal:    <path d="M4 18v-2M9 18v-5M14 18v-8M19 18V8" />,
  dot:       <circle cx="12" cy="12" r="4" />,
  scan:      <g><path d="M4 8V5.5A1.5 1.5 0 015.5 4H8M16 4h2.5A1.5 1.5 0 0120 5.5V8M20 16v2.5a1.5 1.5 0 01-1.5 1.5H16M8 20H5.5A1.5 1.5 0 014 18.5V16" /><path d="M4 12h16" /></g>,
  shield:    <path d="M12 3l7 3v5c0 5-3 7.5-7 9-4-1.5-7-4-7-9V6z" />,
  swap:      <g><path d="M7 7h11l-3-3M17 17H6l3 3" /></g>,
};

function Icon({ name, size = 24, stroke = 'currentColor', strokeWidth = 1.7, fill = 'none', style }) {
  const inner = ICON_PATHS[name] || ICON_PATHS.dot;
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill={fill}
         stroke={stroke} strokeWidth={strokeWidth} strokeLinecap="round"
         strokeLinejoin="round" style={style} aria-hidden="true">
      {inner}
    </svg>
  );
}

Object.assign(window, { Icon, ICON_PATHS });
