// ملف البناء الجذري لمشروع GT-TAHAKOM
// الإضافات تُعرّف هنا بدون تطبيق، وتُطبّق في وحدة :app
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}
