package com.gnutux.tahakom.core.model

/**
 * أزرار دلالية معروفة مشتركة بين كل أنواع الأجهزة ووسائل النقل.
 *
 * مقتبسة من ثوابت المعرّفات في `Button.java` بمشروع IRRemote، لكنها هنا
 * مجرّدة عن IR: كل [com.gnutux.tahakom.core.transport.Transport] يترجم هذا
 * المعرّف إلى تمثيله الخاص (كود IR، مفتاح Roku ECP، أمر WebSocket لـ Samsung...).
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
}
