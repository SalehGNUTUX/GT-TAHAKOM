# موقع المشروع — البناء والتحديث

الموقع مستضاف على **GitHub Pages**: https://salehgnutux.github.io/GT-TAHAKOM/
يُقدَّم من جذر فرع `main` (إعداد Pages: branch=main, path=/). ملف `.nojekyll` يمنع
معالجة Jekyll فيُقدَّم HTML كما هو.

## البنية
- `index.html` (الجذر): صفحة الهبوط — بطل + مزايا + تحميل + النموذج الحي. مستقلّة
  بذاتها (CSS/JS مضمّن)، ثنائية اللغة (ع/EN) ووضعان (فاتح/داكن) بنفس ألوان التطبيق
  (سمة serene)، تُحفظ التفضيلات في localStorage.
- `website/demo/`: النموذج الحي التفاعلي (React عبر Babel من GT-TAHAKOM-DESIGN)،
  مضمّن في الصفحة عبر `<iframe>`. قابل للنقر والتنقّل، وفيه مبدّل لغة/سمة داخلي.
- `website/assets/`: أيقونة التطبيق.

## التحديث مع كل إصدار (مهم)
عند إصدار نسخة جديدة، حدّث في `index.html`:
1. **شارة الإصدار**: `<span class="badge" id="verBadge">v0.9.2</span>` → الإصدار الجديد.
2. **زر التحميل** يشير دائماً لأحدث إصدار تلقائياً (`releases/latest`) فلا يحتاج تعديلاً.
3. أضِف أي ميزة جديدة إلى مصفوفة `I18N.ar.feats` و`I18N.en.feats`.
4. إن تغيّر التصميم في `GT-TAHAKOM-DESIGN/`، أعد نسخه إلى `website/demo/`:
   ```bash
   cp -r GT-TAHAKOM-DESIGN/{src,frames,assets,uploads} website/demo/
   cp GT-TAHAKOM-DESIGN/TAHAKOM.html website/demo/index.html
   ```
5. ادفع إلى `main` — تتحدّث Pages تلقائياً خلال دقيقة.

## التحقّق
```bash
curl -s -o /dev/null -w "%{http_code}" https://salehgnutux.github.io/GT-TAHAKOM/
gh api repos/SalehGNUTUX/GT-TAHAKOM/pages/builds/latest --jq .status   # = built
```
