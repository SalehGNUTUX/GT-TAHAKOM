package com.gnutux.tahakom.core.model

/**
 * أزرار دلالية معروفة مشتركة بين **كل أنواع الأجهزة** ووسائل النقل.
 *
 * مقتبسة وموسّعة من ثوابت `Button.java` بمشروع IRRemote. مجرّدة عن البروتوكول:
 * كل [com.gnutux.tahakom.core.transport.Transport] يترجم المعرّف لتمثيله الخاص.
 * النظام عام: الواجهة تعرض تلقائياً ما يدعمه كل جهاز من هذه القائمة (مدفوع بالبيانات).
 */
enum class ButtonId {
    UNKNOWN,

    // الطاقة
    POWER, POWER_ON, POWER_OFF,

    // الصوت
    VOL_UP, VOL_DOWN, MUTE,

    // القنوات
    CH_UP, CH_DOWN,

    // التنقّل
    NAV_UP, NAV_DOWN, NAV_LEFT, NAV_RIGHT, NAV_OK,
    BACK, HOME, MENU, EXIT, INFO, GUIDE,

    // الأرقام
    DIGIT_0, DIGIT_1, DIGIT_2, DIGIT_3, DIGIT_4,
    DIGIT_5, DIGIT_6, DIGIT_7, DIGIT_8, DIGIT_9,

    // المصادر والوسائط
    SOURCE, SMART,
    PLAY, PAUSE, STOP, FFWD, RWD, NEXT, PREV, RECORD,

    // وظائف إضافية (من IRRemote + ريموتات حديثة مثل LG/Samsung)
    CC,        // الترجمة/التسميات التوضيحية (Subtitle/Closed Caption)
    CLEAR,     // مسح
    LAST,      // القناة السابقة
    SLEEP,     // مؤقّت النوم
    DISP,      // العرض/المعلومات
    LIST,      // قائمة القنوات
    SETTINGS,  // الإعدادات
    TEXT,      // نصّ/تيليتكست
    AD,        // الوصف الصوتي / Quick Access
    TOPT,      // خيارات (T.OPT)
    FAV,       // المفضّلة

    // الأزرار الملوّنة (تيليتكست/تفاعلي)
    RED, GREEN, YELLOW, BLUE,
}
