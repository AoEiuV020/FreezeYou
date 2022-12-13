package cc.aoeiuv020

import android.content.Intent
import cf.playhi.freezeyou.Main
import cf.playhi.freezeyou.OneKeyFreeze

/**
 * Created by AoEiuV020 on 2022.12.14-00:26:45.
 */
object FreezeHelper {
    /**
     * @return 返回true表示消费了事件，外面不再继续处理，
     */
    fun onFloatButtonClick(main: Main): Boolean {
        main.startActivity(
            Intent(main, OneKeyFreeze::class.java).putExtra(
                "autoCheckAndLockScreen",
                false
            )
        )

        return true;
    }
}