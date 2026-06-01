#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
يحوّل قاعدة بيانات IRRemote (assets/db: صيغتا .button و remote.json)
إلى صيغة موحّدة لـ GT-TAHAKOM في app/src/main/assets/irdb/:
  - irdb/index.json           : فهرس [category, brand, model, file, freq]
  - irdb/<category>/<slug>.json : جهاز واحد بأزراره الدلالية + أكواد Pronto

يُشغّل مرة واحدة (أو عند تحديث القاعدة). أوفلاين بالكامل، بلا تبعيات خارجية.
"""
import json
import os
import re
import sys

SRC = "_study/IRRemote-libre/app/src/main/assets/db"
DST = "app/src/main/assets/irdb"

# المعرّفات الدلالية مطابقة لـ Button.java في IRRemote (الرقم → الاسم)
ID_NAMES = {
    0: "UNKNOWN", 1: "POWER", 2: "POWER_ON", 3: "POWER_OFF", 4: "VOL_UP",
    5: "VOL_DOWN", 6: "CH_UP", 7: "CH_DOWN", 8: "NAV_UP", 9: "NAV_DOWN",
    10: "NAV_LEFT", 11: "NAV_RIGHT", 12: "NAV_OK", 13: "BACK", 14: "MUTE",
    15: "MENU", 16: "DIGIT_0", 17: "DIGIT_1", 18: "DIGIT_2", 19: "DIGIT_3",
    20: "DIGIT_4", 21: "DIGIT_5", 22: "DIGIT_6", 23: "DIGIT_7", 24: "DIGIT_8",
    25: "DIGIT_9", 26: "SOURCE", 27: "GUIDE", 28: "SMART", 29: "LAST",
    30: "CLEAR", 31: "EXIT", 32: "CC", 33: "INFO", 34: "SLEEP", 35: "PLAY",
    36: "PAUSE", 37: "STOP", 38: "FFWD", 39: "RWD", 40: "NEXT", 41: "PREV",
    42: "RECORD", 43: "DISP", 61: "HOME",
}


def slugify(name):
    s = re.sub(r"[^A-Za-z0-9]+", "-", name).strip("-").lower()
    return s or "device"


def pronto_freq(code):
    """التردد بالهرتز من رمز Pronto: 1_000_000 / (word1 * 0.241246)."""
    parts = code.split()
    if len(parts) < 2 or parts[0] != "0000":
        return 0
    try:
        word1 = int(parts[1], 16)
        return round(1_000_000 / (word1 * 0.241246)) if word1 else 0
    except ValueError:
        return 0


def convert_button_dir(path):
    """مجلد فيه ملفات b_N.button → قائمة أزرار دلالية."""
    buttons = []
    for fn in sorted(os.listdir(path), key=lambda x: int(re.sub(r"\D", "", x) or 0)):
        if not fn.endswith(".button"):
            continue
        m = re.match(r"b_(\d+)\.button", fn)
        if not m:
            continue
        bid = int(m.group(1))
        code = open(os.path.join(path, fn), encoding="utf-8", errors="ignore").read().strip()
        if not code:
            continue
        buttons.append({
            "id": ID_NAMES.get(bid, "UNKNOWN"),
            "code": code,
            "freq": pronto_freq(code),
        })
    return buttons


def convert_remote_json(path):
    """مجلد فيه remote.json → قائمة أزرار (أوامر خاصة، id دلالي إن وُجد)."""
    data = json.load(open(os.path.join(path, "remote.json"), encoding="utf-8"))
    buttons = []
    for b in data.get("buttons", []):
        code = b.get("code", "").strip()
        if not code:
            continue
        buttons.append({
            "id": ID_NAMES.get(b.get("id", 0), "UNKNOWN"),
            "label": b.get("text", ""),
            "code": code,
            "freq": pronto_freq(code),
        })
    return buttons


def main():
    if not os.path.isdir(SRC):
        print(f"source not found: {SRC}", file=sys.stderr)
        sys.exit(1)
    index = []
    for category in sorted(os.listdir(SRC)):
        cat_path = os.path.join(SRC, category)
        if not os.path.isdir(cat_path):
            continue
        out_cat = os.path.join(DST, category)
        os.makedirs(out_cat, exist_ok=True)
        for brand in sorted(os.listdir(cat_path)):
            dev_path = os.path.join(cat_path, brand)
            if not os.path.isdir(dev_path):
                continue
            if os.path.exists(os.path.join(dev_path, "remote.json")):
                buttons = convert_remote_json(dev_path)
            else:
                buttons = convert_button_dir(dev_path)
            if not buttons:
                continue
            freq = next((b["freq"] for b in buttons if b["freq"]), 0)
            slug = slugify(brand)
            device = {
                "category": category,
                "brand": brand,
                "model": brand,
                "freq": freq,
                "buttons": buttons,
            }
            rel = f"{category}/{slug}.json"
            json.dump(device, open(os.path.join(DST, rel), "w", encoding="utf-8"),
                      ensure_ascii=False, separators=(",", ":"))
            index.append({
                "category": category,
                "brand": brand,
                "model": brand,
                "file": rel,
                "freq": freq,
                "buttons": len(buttons),
            })
    os.makedirs(DST, exist_ok=True)
    json.dump({"version": 1, "devices": index},
              open(os.path.join(DST, "index.json"), "w", encoding="utf-8"),
              ensure_ascii=False, separators=(",", ":"))
    print(f"converted {len(index)} devices into {DST}")
    cats = {}
    for d in index:
        cats[d["category"]] = cats.get(d["category"], 0) + 1
    for c, n in sorted(cats.items()):
        print(f"  {c}: {n}")


if __name__ == "__main__":
    main()
