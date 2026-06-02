// data.jsx — device + transport + command model
// Devices carry a transport ('wifi'|'ir'|'bridge') chosen automatically,
// plus the alternative paths the auto-detector considered.

const DEVICES = [
  {
    id: 'living-sony',
    nameAr: 'تلفاز المعيشة', nameEn: 'Living room TV',
    brand: 'Sony Bravia', platformAr: 'Android TV', platformEn: 'Android TV',
    roomAr: 'المعيشة', roomEn: 'Living room',
    icon: 'tv', transport: 'wifi', status: 'connected', signal: 3,
    paths: { wifi: 'active', ir: 'na', bridge: 'available' },
  },
  {
    id: 'bedroom-samsung',
    nameAr: 'شاشة غرفة النوم', nameEn: 'Bedroom screen',
    brand: 'Samsung', platformAr: 'Tizen', platformEn: 'Tizen',
    roomAr: 'غرفة النوم', roomEn: 'Bedroom',
    icon: 'tv', transport: 'wifi', status: 'connected', signal: 2,
    paths: { wifi: 'active', ir: 'na', bridge: 'available' },
  },
  {
    id: 'salon-lg-ac',
    nameAr: 'مكيّف الصالة', nameEn: 'Salon A/C',
    brand: 'LG', platformAr: 'عاكس', platformEn: 'Inverter',
    roomAr: 'الصالة', roomEn: 'Salon',
    icon: 'ac', transport: 'bridge', status: 'ready', signal: 3,
    paths: { wifi: 'na', ir: 'available', bridge: 'active' },
  },
  {
    id: 'theater-roku',
    nameAr: 'المسرح المنزلي', nameEn: 'Home theater',
    brand: 'Roku', platformAr: 'Roku OS', platformEn: 'Roku OS',
    roomAr: 'المعيشة', roomEn: 'Living room',
    icon: 'speaker', transport: 'wifi', status: 'connected', signal: 3,
    paths: { wifi: 'active', ir: 'available', bridge: 'available' },
  },
  {
    id: 'old-receiver',
    nameAr: 'جهاز الاستقبال', nameEn: 'Set-top box',
    brand: 'Generic', platformAr: 'استقبال قديم', platformEn: 'Legacy IR',
    roomAr: 'المعيشة', roomEn: 'Living room',
    icon: 'receiver', transport: 'bridge', status: 'ready', signal: 2,
    paths: { wifi: 'na', ir: 'available', bridge: 'active' },
  },
  {
    id: 'kitchen-lg',
    nameAr: 'تلفاز المطبخ', nameEn: 'Kitchen TV',
    brand: 'LG', platformAr: 'webOS', platformEn: 'webOS',
    roomAr: 'المطبخ', roomEn: 'Kitchen',
    icon: 'tv', transport: 'wifi', status: 'offline', signal: 0,
    paths: { wifi: 'active', ir: 'na', bridge: 'available' },
  },
];

// devices the pairing scanner "discovers"
const DISCOVERABLE = [
  { id: 'd1', nameAr: 'تلفاز Sony – غرفة الضيوف', nameEn: 'Sony TV – Guest room', brand: 'Sony Bravia', transport: 'wifi', icon: 'tv', detailAr: 'اكتُشف على الشبكة', detailEn: 'Found on network' },
  { id: 'd2', nameAr: 'وحدة Broadlink RM4', nameEn: 'Broadlink RM4 hub', brand: 'Broadlink', transport: 'bridge', icon: 'bridge', detailAr: 'جسر أشعة في الصالة', detailEn: 'IR bridge in the salon' },
  { id: 'd3', nameAr: 'مكبّر صوت Sonos', nameEn: 'Sonos speaker', brand: 'Sonos', transport: 'wifi', icon: 'speaker', detailAr: 'اكتُشف على الشبكة', detailEn: 'Found on network' },
];

const TRANSPORTS = ['wifi', 'ir', 'bridge'];
const TRANSPORT_META = {
  wifi:   { icon: 'wifi',   key: 'trWifi',   descKey: 'trWifiDesc',   latency: 0.9, reliab: 0.95, range: 0.8 },
  ir:     { icon: 'ir',     key: 'trIr',     descKey: 'trIrDesc',     latency: 0.7, reliab: 0.6,  range: 0.4 },
  bridge: { icon: 'bridge', key: 'trBridge', descKey: 'trBridgeDesc', latency: 0.8, reliab: 0.85, range: 0.9 },
};

// remote command rows
function transportColorOf(colors, tr) {
  return tr === 'wifi' ? colors.wifi : tr === 'ir' ? colors.ir : colors.bridge;
}
function transportSoftOf(colors, tr) {
  return tr === 'wifi' ? colors.wifiSoft : tr === 'ir' ? colors.irSoft : colors.bridgeSoft;
}

Object.assign(window, {
  DEVICES, DISCOVERABLE, TRANSPORTS, TRANSPORT_META,
  transportColorOf, transportSoftOf,
});
