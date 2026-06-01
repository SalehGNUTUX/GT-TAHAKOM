package com.gnutux.tahakom.core.irdb

import com.gnutux.tahakom.core.model.ButtonId
import com.gnutux.tahakom.core.model.Command

/**
 * يحوّل زراً دلالياً ([ButtonId]) إلى [Command.Raw] يحمل كود Pronto الخاص بجهاز IR،
 * بالبحث في أزرار الجهاز المحمّل. يربط القاعدة المحلية بطبقة النقل.
 */
object IrCommandResolver {

    /** يجد كود Pronto للزر الدلالي في الجهاز، أو null إن لم يدعمه. */
    fun resolve(device: IrDevice, button: ButtonId): Command.Raw? {
        val code = device.buttons.firstOrNull { it.id == button.name }?.code ?: return null
        return Command.Raw(code)
    }

    /** المعرّفات الدلالية التي يدعمها هذا الجهاز فعلياً (لإظهار الأزرار المتاحة فقط). */
    fun supportedButtons(device: IrDevice): Set<String> =
        device.buttons.map { it.id }.toSet()
}
