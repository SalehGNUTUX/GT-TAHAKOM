// i18n.jsx — AR/EN strings. t(lang, key). RTL when lang==='ar'.
const STR = {
  appName:        { ar: 'تَحَكُّمْ',            en: 'TAHAKOM' },
  appTag:         { ar: 'مركز تحكّم موحّد',     en: 'One control hub' },

  // nav
  navDevices:     { ar: 'الأجهزة',   en: 'Devices' },
  navRemote:      { ar: 'جهاز التحكّم', en: 'Remote' },
  navAdd:         { ar: 'إضافة',     en: 'Add' },
  navSettings:    { ar: 'الإعدادات', en: 'Settings' },

  // onboarding
  obSkip:         { ar: 'تخطّي',            en: 'Skip' },
  obNext:         { ar: 'التالي',           en: 'Next' },
  obStart:        { ar: 'لنبدأ',            en: 'Get started' },
  ob1Title:       { ar: 'جهاز تحكّم واحد لكل شيء', en: 'One remote for everything' },
  ob1Body:        { ar: 'تحكّم في تلفازك وأجهزتك من مكان واحد — مهما اختلفت علاماتها التجارية أو طريقة اتصالها.', en: 'Control your TV and devices from one place — whatever the brand or how they connect.' },
  ob2Title:       { ar: 'يختار الوسيلة تلقائياً', en: 'Picks the path automatically' },
  ob2Body:        { ar: 'يكتشف التطبيق هل يصل الجهاز عبر الشبكة أم الأشعة تحت الحمراء أم جسر — ويستخدمها دون إعداد.', en: 'It detects whether a device is reached over network, infrared, or a bridge — and uses it, no setup.' },
  ob3Title:       { ar: 'ثلاث وسائل، تجربة واحدة', en: 'Three paths, one feel' },
  ob3Body:        { ar: 'شبكة WiFi للتلفازات الذكية، أشعة تحت الحمراء للأجهزة القديمة، وجسر Broadlink لكل ما بينهما.', en: 'WiFi for smart TVs, infrared for legacy gear, and a Broadlink bridge for everything between.' },

  // devices
  devTitle:       { ar: 'أجهزتي',          en: 'My devices' },
  devConnected:   { ar: 'متصل',            en: 'Connected' },
  devReady:       { ar: 'جاهز',            en: 'Ready' },
  devOffline:     { ar: 'غير متصل',        en: 'Offline' },
  devSearching:   { ar: 'يبحث…',           en: 'Searching…' },
  devVia:         { ar: 'عبر',             en: 'via' },
  devOpenRemote:  { ar: 'فتح الريموت',     en: 'Open remote' },
  devAll:         { ar: 'الكل',            en: 'All' },
  devRooms:       { ar: 'الغرف',           en: 'Rooms' },
  devAutoHint:    { ar: 'وسيلة الاتصال تُختار تلقائياً', en: 'Connection path is chosen automatically' },

  // remote
  rmPower:        { ar: 'الطاقة',          en: 'Power' },
  rmTouchHint:    { ar: 'اسحب للتنقّل · انقر للاختيار', en: 'Swipe to navigate · tap to select' },
  rmVolume:       { ar: 'الصوت',           en: 'Volume' },
  rmChannel:      { ar: 'القناة',          en: 'Channel' },
  rmMute:         { ar: 'كتم',             en: 'Mute' },
  rmHome:         { ar: 'الرئيسية',        en: 'Home' },
  rmBack:         { ar: 'رجوع',            en: 'Back' },
  rmVoice:        { ar: 'بحث صوتي',        en: 'Voice' },
  rmSource:       { ar: 'المصدر',          en: 'Source' },
  rmMenu:         { ar: 'القائمة',         en: 'Menu' },
  rmMore:         { ar: 'المزيد',          en: 'More' },
  rmClick:        { ar: 'نقر',             en: 'Click' },
  rmApps:         { ar: 'التطبيقات',       en: 'Apps' },
  rmListening:    { ar: 'يستمع…',          en: 'Listening…' },
  rmSent:         { ar: 'أُرسل',           en: 'Sent' },
  rmActivePath:   { ar: 'وسيلة الإرسال النشطة', en: 'Active send path' },

  // more controls
  moreTitle:      { ar: 'أزرار إضافية',     en: 'More controls' },
  morePointer:    { ar: 'وضع المؤشّر',       en: 'Pointer mode' },
  morePointerHint:{ ar: 'حرّك مؤشّراً على الشاشة عبر مربّع اللمس — للأجهزة الذكية التي تدعم ذلك.', en: 'Move an on-screen cursor with the touchpad — for smart devices that support it.' },
  morePointerOn:  { ar: 'وضع المؤشّر مُفعّل', en: 'Pointer mode on' },
  moreExit:       { ar: 'إيقاف',            en: 'Exit' },
  moreNumbers:    { ar: 'لوحة الأرقام',     en: 'Number pad' },
  moreFunctions:  { ar: 'وظائف',            en: 'Functions' },
  moreColors:     { ar: 'الأزرار الملوّنة', en: 'Color buttons' },
  fnGuide:        { ar: 'دليل البرامج',     en: 'Guide' },
  fnInfo:         { ar: 'معلومات',          en: 'Info' },
  fnSubs:         { ar: 'الترجمة',          en: 'Subtitles' },
  fnChannels:     { ar: 'قائمة القنوات',    en: 'Channel list' },
  fnKeyboard:     { ar: 'لوحة المفاتيح',    en: 'Keyboard' },
  fnDelete:       { ar: 'حذف',              en: 'Delete' },

  // transport
  trTitle:        { ar: 'وسيلة الاتصال',   en: 'Connection path' },
  trAutoTitle:    { ar: 'الاكتشاف التلقائي', en: 'Auto-detected' },
  trAutoBody:     { ar: 'فحص التطبيق الجهاز واختار أفضل وسيلة متاحة. يمكنك تغييرها يدوياً.', en: 'We probed the device and chose the best available path. You can override it.' },
  trWifi:         { ar: 'شبكة WiFi',       en: 'WiFi network' },
  trWifiDesc:     { ar: 'تلفازات Android TV و Samsung و LG و Roku و Sony', en: 'Android TV, Samsung, LG, Roku, Sony' },
  trIr:           { ar: 'أشعة تحت الحمراء', en: 'Infrared (IR)' },
  trIrDesc:       { ar: 'الأجهزة القديمة عبر باعث الهاتف المدمج', en: 'Legacy gear via the phone\'s built-in emitter' },
  trBridge:       { ar: 'جسر WiFi-IR',     en: 'WiFi-IR bridge' },
  trBridgeDesc:   { ar: 'أي جهاز IR عبر وحدة Broadlink', en: 'Any IR device through a Broadlink unit' },
  trActive:       { ar: 'نشط الآن',        en: 'Active now' },
  trUnavailable:  { ar: 'غير متاح لهذا الجهاز', en: 'Not available for this device' },
  trWhy:          { ar: 'لماذا هذه الوسيلة؟', en: 'Why this path?' },
  trWhyBody:      { ar: 'هذا الجهاز يعلن عن نفسه على شبكتك المحلية، فالشبكة أسرع وأدق من الأشعة.', en: 'This device announces itself on your local network, so WiFi is faster and more precise than IR.' },
  trLatency:      { ar: 'زمن الاستجابة',   en: 'Latency' },
  trReliab:       { ar: 'الموثوقية',       en: 'Reliability' },
  trRange:        { ar: 'المدى',           en: 'Range' },

  // pairing
  pairTitle:      { ar: 'إضافة جهاز',      en: 'Add a device' },
  pairScanning:   { ar: 'يفحص محيطك…',     en: 'Scanning around you…' },
  pairScanSub:    { ar: 'الشبكة · الأشعة · الجسور', en: 'Network · infrared · bridges' },
  pairFound:      { ar: 'عُثر عليها',      en: 'Found nearby' },
  pairManual:     { ar: 'إضافة يدوية',     en: 'Add manually' },
  pairConnect:    { ar: 'اتصال',           en: 'Connect' },
  pairConnecting: { ar: 'يتصل…',           en: 'Connecting…' },
  pairPaired:     { ar: 'تمت الإضافة',     en: 'Paired' },
  pairAgain:      { ar: 'إعادة الفحص',     en: 'Scan again' },

  // settings
  setTitle:       { ar: 'الإعدادات',       en: 'Settings' },
  setAppearance:  { ar: 'المظهر',          en: 'Appearance' },
  setTheme:       { ar: 'السمة',           en: 'Theme' },
  setDark:        { ar: 'داكن',            en: 'Dark' },
  setLight:       { ar: 'فاتح',            en: 'Light' },
  setLang:        { ar: 'اللغة',           en: 'Language' },
  setTransports:  { ar: 'وسائل الإرسال',   en: 'Transports' },
  setBridgeIp:    { ar: 'عنوان جسر Broadlink', en: 'Broadlink bridge IP' },
  setIrEmitter:   { ar: 'باعث الأشعة المدمج', en: 'Built-in IR emitter' },
  setIrNone:      { ar: 'غير متوفّر في هذا الهاتف', en: 'Not present on this phone' },
  setNetwork:     { ar: 'اكتشاف الشبكة',   en: 'Network discovery' },
  setAbout:       { ar: 'حول التطبيق',     en: 'About' },
  setVersion:     { ar: 'الإصدار',         en: 'Version' },
  setLicense:     { ar: 'الترخيص',         en: 'License' },
  setLicenseBody: { ar: 'GPLv3 — مقتبس معمارياً من مشروع IRRemote.', en: 'GPLv3 — architecture adapted from the IRRemote project.' },
  setOn:          { ar: 'مُفعّل',           en: 'On' },
  setOff:         { ar: 'معطّل',           en: 'Off' },
};

function t(lang, key) {
  const e = STR[key];
  if (!e) return key;
  return e[lang] || e.en;
}

Object.assign(window, { STR, t });
