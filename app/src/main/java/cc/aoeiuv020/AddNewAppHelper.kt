package cc.aoeiuv020

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import cf.playhi.freezeyou.R
import cf.playhi.freezeyou.utils.AlertDialogUtils
import cf.playhi.freezeyou.utils.ApplicationLabelUtils
import cf.playhi.freezeyou.utils.OneKeyListUtils
import cf.playhi.freezeyou.utils.ToastUtils

/**
 * Created by AoEiuV020 on 2021.12.23-01:03:20.
 */
@SuppressLint("StaticFieldLeak")
object AddNewAppHelper {
    const val name = "AddNewAppHelper"
    const val key = "exists"
    private lateinit var ctx: Context
    val sharedPreferences: SharedPreferences
        get() = ctx.getSharedPreferences(name, Context.MODE_PRIVATE)

    fun checkNew(activity: Activity) {
        this.ctx = activity.applicationContext
        Thread {
            val existsList = load()
            val allList =
                ctx.packageManager.getInstalledPackages(0)
                    .map {
                        val name = ApplicationLabelUtils.getApplicationLabel(
                            ctx,
                            ctx.packageManager,
                            it.applicationInfo,
                            it.packageName
                        )
                        AppInfo(name, it.packageName)
                    }

            val newList: List<AppInfo> = allList.filter {
                !OneKeyListUtils.existsInOneKeyList(
                    ctx,
                    ctx.getString(R.string.sAutoFreezeApplicationList),
                    it.applicationId
                ) && !existsList.contains(it)
            }
            if (newList.isEmpty()) {
                if (allList.size != existsList.size) {
                    save(allList)
                }
                return@Thread
            }
            val nameList = newList.map { it.name }.toTypedArray()
            activity.runOnUiThread {
                val selectedIndexSet = mutableSetOf<Int>()
                AlertDialog.Builder(activity)
                    .setIcon(R.mipmap.ic_launcher_new_round)
                    .setTitle("新应用添加到一键冻结")
                    .setMultiChoiceItems(nameList, null) { _, position, selected ->
                        if (selected) {
                            selectedIndexSet.add(position)
                        } else {
                            selectedIndexSet.remove(position)
                        }
                    }.setNegativeButton("下次再说") { _, _ ->
                    }.setNeutralButton("全部确定") { _, _ ->
                        save(allList)
                        add(activity, newList)
                    }.setPositiveButton("确定") { _, _ ->
                        save(allList)
                        if (selectedIndexSet.isEmpty()) {
                            return@setPositiveButton
                        }
                        add(activity, selectedIndexSet.map { newList[it] })
                    }.show()
            }
        }.start()
    }

    private fun add(
        activity: Activity,
        selectedList: List<AppInfo>
    ) {
        val failedNameList = mutableListOf<String>()
        selectedList.onEach {
            if (!OneKeyListUtils.addToOneKeyList(
                    ctx,
                    ctx.getString(R.string.sAutoFreezeApplicationList),
                    it.applicationId
                )
            ) {
                failedNameList.add(it.name)
            }
        }
        if (failedNameList.isEmpty()) {
            ToastUtils.showShortToast(ctx, "添加新应用成功")
        } else {
            AlertDialogUtils.buildAlertDialog(
                activity, null,
                failedNameList.joinToString(", "), "添加新应用失败"
            ).show()
        }
    }

    private fun save(appInfoList: List<AppInfo>) {
        val value = appInfoList.joinToString("\n") { it.applicationId + '/' + it.name }
        sharedPreferences.edit().putString(key, value).apply()
    }

    private fun load(): List<AppInfo> {
        val value = sharedPreferences.getString(key, null) ?: return emptyList()
        return value.split('\n').map {
            val (applicationId, name) = it.split('/')
            AppInfo(name, applicationId)
        }
    }

    private data class AppInfo(
        val name: String,
        val applicationId: String,
    ) {
        override fun equals(other: Any?): Boolean {
            return applicationId == (other as? AppInfo)?.applicationId
        }

        override fun hashCode(): Int {
            return applicationId.hashCode()
        }
    }
}
