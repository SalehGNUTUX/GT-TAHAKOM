# قواعد ProGuard/R8 لـ GT-TAHAKOM
# تُضاف قواعد خاصة بوسائل النقل (OkHttp/WebSocket) عند الحاجة في المراحل القادمة.

# الاحتفاظ بنماذج البيانات المتسلسلة
-keep class com.gnutux.tahakom.core.model.** { *; }
