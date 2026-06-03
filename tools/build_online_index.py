#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
يولّد فهرس البحث الشبكي `app/src/main/assets/online_index.json` من كامل قاعدة
probonopd/irdb بتنزيل أرشيف المستودع **مرة واحدة** (لا 3000 طلب). الفهرس يُشحَن
مع التطبيق فيصير البحث بالعلامة أوفلاين، والإنترنت يُستخدم فقط لتنزيل أكواد الجهاز
المختار لاحقاً عبر raw.githubusercontent.com.

الاستخدام:  python3 tools/build_online_index.py

كل مدخل: {brand, category, type, path, protocol, functions, supported}
  - path: المسار النسبي تحت codes/ (لبناء رابط raw عند الجلب).
  - supported: هل بروتوكوله ضمن ما يدعمه محوّل الهاتف (NEC/RC5/RC6).
"""
import io
import json
import os
import tarfile
import urllib.request
from collections import Counter

TARBALL = "https://github.com/probonopd/irdb/archive/refs/heads/master.tar.gz"
OUT = "app/src/main/assets/online_index.json"

# بروتوكولات يدعمها محوّل الهاتف (IrCodeConverter.kt) — يجب أن يطابق when_protocol هناك.
def is_supported(proto):
    p = proto.upper()
    return (p.startswith("NEC") or p in ("RC5", "RC5X", "RC6")
            or p.startswith("SONY") or p == "SIRC" or p == "PANASONIC"
            or p in ("JVC", "MITSUBISHI", "DENON"))


def categorize(type_folder):
    """يستنتج فئة موحّدة (TV/Cable/Audio/Other) من اسم مجلّد نوع الجهاز."""
    t = type_folder.upper()
    if "TV" in t:
        return "TV"
    if any(k in t for k in ("SAT", "CABLE", "STB", "DVB", "TUNER", "DTV", "HDTV", "DECODER", "RECEIVER STB")):
        return "Cable"
    if any(k in t for k in ("AUDIO", "AMP", "CD", "DVD", "BLU", "SOUND", "SPEAKER", "STEREO", "RECEIVER", "AV ")):
        return "Audio"
    return "Other"


def dominant_protocol(csv_text):
    protos = []
    funcs = 0
    for line in csv_text.splitlines()[1:]:
        parts = line.split(",")
        if len(parts) < 5:
            continue
        protos.append(parts[1].strip())
        funcs += 1
    if not protos:
        return None, 0
    return Counter(protos).most_common(1)[0][0], funcs


def main():
    print("downloading irdb archive (one request)…")
    data = urllib.request.urlopen(TARBALL, timeout=120).read()
    print(f"  got {len(data) // 1024} KB")
    devices = []
    with tarfile.open(fileobj=io.BytesIO(data), mode="r:gz") as tar:
        for m in tar.getmembers():
            # المسار: irdb-master/codes/<Brand>/<Type>/<dev,sub>.csv
            parts = m.name.split("/")
            if not m.isfile() or not m.name.endswith(".csv"):
                continue
            try:
                ci = parts.index("codes")
            except ValueError:
                continue
            rest = parts[ci + 1:]
            if len(rest) < 3:
                continue  # نحتاج Brand/Type/file على الأقل
            brand = rest[0]
            type_folder = rest[-2]
            rel = "/".join(rest)  # تحت codes/
            try:
                text = tar.extractfile(m).read().decode("utf-8", errors="ignore")
            except Exception:
                continue
            proto, funcs = dominant_protocol(text)
            if not proto or funcs == 0:
                continue
            devices.append({
                "brand": brand,
                "category": categorize(type_folder),
                "type": type_folder,
                "path": rel,
                "protocol": proto,
                "functions": funcs,
                "supported": is_supported(proto),
            })
    devices.sort(key=lambda d: (d["brand"].lower(), d["category"], d["path"]))
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    json.dump({"version": 1, "devices": devices},
              open(OUT, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    sup = sum(1 for d in devices if d["supported"])
    brands = len({d["brand"] for d in devices})
    print(f"wrote {OUT}: {len(devices)} codesets · {sup} supported · {brands} brands")


if __name__ == "__main__":
    main()
