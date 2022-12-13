package cc.aoeiuv020

import android.widget.EditText
import cf.playhi.freezeyou.Main
import cf.playhi.freezeyou.R

/**
 * Created by AoEiuV020 on 2022.12.14-00:10:51.
 */
object BackHelper {
    /**
     * @return 返回true表示消费了事件，外面不再继续处理，
     */
    fun onBackPressed(main: Main): Boolean {
        val etSearch = main.findViewById<EditText>(R.id.search_editText)
        if (etSearch.text.isEmpty()) {
            return false
        }
        etSearch.setText("")
        return true
    }

}