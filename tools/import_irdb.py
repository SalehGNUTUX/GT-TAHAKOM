#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
يستورد أجهزة من قاعدة probonopd/irdb (CSV) ويحوّلها إلى صيغة GT-TAHAKOM الموحّدة
في app/src/main/assets/irdb/. يحوّل بروتوكولات NEC/NECx إلى Pronto hex.

الاستخدام (يتطلّب إنترنت مرة واحدة للتنزيل):
  python3 tools/import_irdb.py "TV/Unionaire" "Samsung/TV/7,7" ...
أو بقائمة من ملف. ثم يُضاف الناتج إلى index.json.

المصدر: https://github.com/probonopd/irdb (مفتوح). الصيغة:
  functionname,protocol,device,subdevice,function
"""
import csv
import io
import json
import os
import re
import sys
import urllib.request

RAW = "https://raw.githubusercontent.com/probonopd/irdb/master/codes"
DST = "app/src/main/assets/irdb"

# تطبيع أسماء أزرار probonopd → ButtonId الدلالي عندنا
NAME_MAP = {
    "POWER": "POWER", "POWER ON": "POWER_ON", "POWER OFF": "POWER_OFF",
    "VOLUME +": "VOL_UP", "VOLUME -": "VOL_DOWN", "VOL +": "VOL_UP", "VOL -": "VOL_DOWN",
    "MUTE": "MUTE", "CHANNEL +": "CH_UP", "CHANNEL -": "CH_DOWN", "CH +": "CH_UP", "CH -": "CH_DOWN",
    "UP": "NAV_UP", "DOWN": "NAV_DOWN", "LEFT": "NAV_LEFT", "RIGHT": "NAV_RIGHT",
    "OK": "NAV_OK", "ENTER": "NAV_OK", "SELECT": "NAV_OK",
    "BACK": "BACK", "RETURN": "BACK", "EXIT": "EXIT", "HOME": "HOME", "MENU": "MENU",
    "INFO": "INFO", "DISPLAY": "DISP", "GUIDE": "GUIDE", "EPG": "GUIDE",
    "INPUT SOURCE": "SOURCE", "INPUT": "SOURCE", "SOURCE": "SOURCE", "TV/AV": "SOURCE",
    "0": "DIGIT_0", "1": "DIGIT_1", "2": "DIGIT_2", "3": "DIGIT_3", "4": "DIGIT_4",
    "5": "DIGIT_5", "6": "DIGIT_6", "7": "DIGIT_7", "8": "DIGIT_8", "9": "DIGIT_9",
    "PLAY": "PLAY", "PAUSE": "PAUSE", "STOP": "STOP", "FAST FORWARD": "FFWD",
    "REWIND": "RWD", "RECORD": "RECORD", "SLEEP": "SLEEP", "LIST": "LIST",
    "RED": "RED", "GREEN": "GREEN", "YELLOW": "YELLOW", "BLUE": "BLUE",
    "SUBTITLE": "CC", "TEXT": "TEXT", "FAVORITE": "FAV", "FAV": "FAV",
    # مرادفات LIRC الشائعة (بعد تجريد بادئة KEY_) — كثير من ملفات probonopd بهذه الأسماء
    "POWER": "POWER", "POWERON": "POWER_ON", "POWEROFF": "POWER_OFF", "OFF": "POWER",
    "POWERTOGGLE": "POWER", "POWER2": "POWER",
    "VOLUMEUP": "VOL_UP", "VOLUMEDOWN": "VOL_DOWN",
    "CHANNELUP": "CH_UP", "CHANNELDOWN": "CH_DOWN",
    "VOL_P": "VOL_UP", "VOL_M": "VOL_DOWN", "PROG_P": "CH_UP", "PROG_M": "CH_DOWN",
    "CH_P": "CH_UP", "CH_M": "CH_DOWN",
    "VOLUME_UP": "VOL_UP", "VOLUME_DOWN": "VOL_DOWN",
    "CHANNEL_UP": "CH_UP", "CHANNEL_DOWN": "CH_DOWN",
    "REWIND": "RWD", "FORWARD": "FFWD", "FASTFORWARD": "FFWD", "NEXT": "FFWD", "PREVIOUS": "RWD",
    "RED": "RED", "GREEN": "GREEN", "YELLOW": "YELLOW", "BLUE": "BLUE",
    "EPG": "GUIDE", "TVGUIDE": "GUIDE", "AUX": "SOURCE", "TV": "SOURCE", "VIDEO": "SOURCE",
    "PIP": "PIP", "TOOLS": "TOOLS", "SMART": "SMART", "APPS": "SMART",
}

NEC_FREQ = 38000  # Hz
RC_FREQ = 36000   # Hz — تردد بروتوكولات RC5/RC6 (فيليبس وكثير من الأوروبية/الصينية)


def _durations_to_pronto(freq, durations_us):
    """يحوّل قائمة مدد (ميكروثانية، تبدأ بـ ON وتتناوب) إلى كود Pronto hex.
    قيمة كلمة Pronto = عدد دورات الحامل = مدة×التردد/1e6 (نفس دلالة Pronto القياسية)."""
    carrier = round(1_000_000 / (freq * 0.241246))
    durs = list(durations_us)
    if len(durs) % 2 == 1:
        durs.append(round(50 * 889))  # فجوة فاصلة بين الإطارات
    words = [round(d * freq / 1_000_000) for d in durs]
    n = len(words) // 2
    head = [0x0000, carrier, 0x0000, n]
    return " ".join(f"{w:04x}" for w in head + words)


def _manchester_durations(bits, half_us, rc6=False, toggle_index=None):
    """يبني مدد إشارة من بتّات مانشستر.
    RC5: '1' = فضاء ثم نبضة ؛ '0' = نبضة ثم فضاء.
    RC6: العكس ('1' = نبضة ثم فضاء)؛ وبت التبديل (toggle) مزدوج العرض.
    يُرجع مدداً تبدأ بـ ON (تُسقَط أي بادئة OFF)."""
    levels = []  # 1=نبضة(on) 0=فضاء(off)، عند دقّة نصف-بت
    for i, b in enumerate(bits):
        wide = (toggle_index is not None and i == toggle_index)
        rep = 2 if wide else 1
        if rc6:
            first, second = (1, 0) if b else (0, 1)
        else:
            first, second = (0, 1) if b else (1, 0)
        levels += [first] * rep + [second] * rep
    # دمج المتتاليات (RLE)
    runs = []
    cur, cnt = levels[0], 1
    for l in levels[1:]:
        if l == cur:
            cnt += 1
        else:
            runs.append((cur, cnt)); cur, cnt = l, 1
    runs.append((cur, cnt))
    if runs and runs[0][0] == 0:
        runs = runs[1:]  # أسقط بادئة OFF (لا تُرسَل)
    return [c * half_us for (_lvl, c) in runs]


def rc5_to_pronto(device, function):
    """RC5: 14 بت = S1 S2 T A4..A0 C5..C0. نصف-البت 889μs، 36kHz."""
    # S2 = مكمّل البت السابع للأمر (لدعم أوامر 64-127 = RC5X)
    s2 = 0 if function > 63 else 1
    t = 0
    bits = [1, s2, t]
    for i in range(4, -1, -1):
        bits.append((device >> i) & 1)
    for i in range(5, -1, -1):
        bits.append((function >> i) & 1)
    durs = _manchester_durations(bits, 889, rc6=False)
    return _durations_to_pronto(RC_FREQ, durs)


def rc6_to_pronto(device, function):
    """RC6 mode 0: قائد (6t نبضة + 2t فضاء) + بت بدء(1) + 3 بت وضع(000)
    + بت تبديل مزدوج + 8 عنوان + 8 أمر. وحدة t=444μs، 36kHz، مانشستر معكوس."""
    t_us = 444
    # القائد: نبضة 6t ثم فضاء 2t (مدد صريحة قبل المانشستر)
    lead = [6 * t_us, 2 * t_us]
    bits = [1] + [0, 0, 0]  # بت بدء + وضع 0
    toggle_idx = len(bits)   # بت التبديل (مزدوج العرض)
    bits += [0]              # toggle = 0
    for i in range(7, -1, -1):
        bits.append((device >> i) & 1)
    for i in range(7, -1, -1):
        bits.append((function >> i) & 1)
    man = _manchester_durations(bits, t_us, rc6=True, toggle_index=toggle_idx - 0)
    # القائد نبضة، ثم المانشستر يبدأ ببت البدء '1' = نبضة → ادمج أول نبضة مع القائد؟
    # نُبقيها منفصلة: lead = [on6t, off2t] ثم man يبدأ بـ on. ندمج off2t مع بداية man؟
    # man يبدأ بـ on (بعد إسقاط أي off). نضع lead ثم man مباشرة (lead ينتهي بـ off، man يبدأ بـ on) ✓
    return _durations_to_pronto(RC_FREQ, lead + man)


def encode(protocol, device, subdevice, function):
    """يحوّل (بروتوكول، عنوان، فرعي، أمر) إلى Pronto حسب البروتوكول المدعوم."""
    p = protocol.upper()
    return when_protocol(p, device, subdevice, function)


def when_protocol(p, device, subdevice, function):
    if p.startswith("NEC"):
        return nec_to_pronto(device, subdevice, function)
    if p == "RC5" or p == "RC5X":
        return rc5_to_pronto(device, function)
    if p == "RC6":
        return rc6_to_pronto(device, function)
    return None


def nec_to_pronto(device, subdevice, function):
    """يولّد كود Pronto hex لإطار NEC من (device, subdevice, function)."""
    # NEC: حامل 38kHz. وحدة Pronto: 1/(freq*0.241246e-6).
    carrier = round(1_000_000 / (NEC_FREQ * 0.241246))  # = word1
    # NEC يرسل: device, ~device (أو subdevice), function, ~function (8 بت لكل منها، LSB أولاً)
    if subdevice < 0:
        sub = (~device) & 0xFF
    else:
        sub = subdevice & 0xFF
    bytes_ = [device & 0xFF, sub, function & 0xFF, (~function) & 0xFF]

    # وحدة NEC = 562.5us ≈ 560us. بوحدات Pronto: round(562.5/(carrier*0.241246)).
    unit = round(562.5 / (carrier * 0.241246))
    pairs = []
    # رأس: 9000us on (16 وحدة) + 4500us off (8 وحدات)
    pairs += [16 * unit, 8 * unit]
    for b in bytes_:
        for i in range(8):
            bit = (b >> i) & 1
            if bit:
                pairs += [unit, 3 * unit]  # 1 = 560 on, 1690 off
            else:
                pairs += [unit, unit]      # 0 = 560 on, 560 off
    # نبضة نهاية
    pairs += [unit, 39 * unit]

    n = len(pairs) // 2
    words = [0x0000, carrier, 0x0000, n] + pairs
    return " ".join(f"{w:04x}" for w in words)


def fetch_csv(path):
    url = f"{RAW}/{path}.csv"
    with urllib.request.urlopen(url, timeout=20) as r:
        return r.read().decode("utf-8", errors="ignore")


def convert(csv_text):
    buttons = []
    reader = csv.DictReader(io.StringIO(csv_text))
    for row in reader:
        proto = (row.get("protocol") or "").strip()
        try:
            dev = int(row["device"]); sub = int(row["subdevice"]); fn = int(row["function"])
        except (ValueError, KeyError):
            continue
        code = encode(proto, dev, sub, fn)
        if code is None:
            continue  # بروتوكول غير مدعوم (Panasonic/SIRC… لاحقاً)
        name = (row.get("functionname") or "").strip().upper()
        bid = _normalize_button(name)
        freq = RC_FREQ if proto.upper().startswith("RC") else NEC_FREQ
        buttons.append({"id": bid, "label": name, "code": code, "freq": freq})
    return buttons


def _normalize_button(name):
    """يطابق اسم زر probonopd مع ButtonId. يتسامح مع بادئة LIRC ‏KEY_‎ والفواصل."""
    if name in NAME_MAP:
        return NAME_MAP[name]
    core = name
    for pre in ("KEY_", "KEY ", "BTN_"):
        if core.startswith(pre):
            core = core[len(pre):]
            break
    core = core.strip()
    if core in NAME_MAP:
        return NAME_MAP[core]
    if core.isdigit() and len(core) == 1:
        return f"DIGIT_{core}"
    # تطبيع الفواصل: مسافات/شرطات → شرطة سفلية ثم محاولة أخيرة
    alt = re.sub(r"[\s\-]+", "_", core)
    return NAME_MAP.get(alt, "UNKNOWN")


def slugify(name):
    return re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-") or "device"


def main(specs):
    """specs: قائمة 'Brand/TV/dev,sub' (مسار probonopd). الفئة تُستنتج من الجزء الثاني."""
    index_path = os.path.join(DST, "index.json")
    index = json.load(open(index_path, encoding="utf-8"))["devices"]
    added = 0
    for spec in specs:
        parts = spec.split("/")
        brand = parts[0]
        category = "TV" if len(parts) < 2 else parts[1].replace(" ", "_")
        try:
            buttons = convert(fetch_csv(spec))
        except Exception as e:
            print(f"skip {spec}: {e}", file=sys.stderr)
            continue
        if not buttons:
            print(f"skip {spec}: no NEC buttons", file=sys.stderr)
            continue
        cat_dir = "TV" if "TV" in category else category
        os.makedirs(os.path.join(DST, cat_dir), exist_ok=True)
        slug = slugify(brand)
        rel = f"{cat_dir}/{slug}.json"
        device = {"category": cat_dir, "brand": brand, "model": brand,
                  "freq": NEC_FREQ, "buttons": buttons}
        json.dump(device, open(os.path.join(DST, rel), "w", encoding="utf-8"),
                  ensure_ascii=False, separators=(",", ":"))
        if not any(d["file"] == rel for d in index):
            index.append({"category": cat_dir, "brand": brand, "model": brand,
                          "file": rel, "freq": NEC_FREQ, "buttons": len(buttons)})
        added += 1
        print(f"added {brand} ({len(buttons)} buttons) ← {spec}")
    json.dump({"version": 1, "devices": index},
              open(index_path, "w", encoding="utf-8"),
              ensure_ascii=False, separators=(",", ":"))
    print(f"done: {added} devices, index now {len(index)}")


if __name__ == "__main__":
    main(sys.argv[1:])
