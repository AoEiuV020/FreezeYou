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
    const val key = "appInfoList"
    private lateinit var ctx: Context
    private var running: Boolean = false
    val sharedPreferences: SharedPreferences
        get() = ctx.getSharedPreferences(name, Context.MODE_PRIVATE)

    fun checkNew(activity: Activity) {
        this.ctx = activity.applicationContext
        if (running) {
            return
        }
        running = true
        val thread = Thread {
            try {
                realCheck(activity)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                running = false
            }
        }
        thread.start()
    }

    private fun realCheck(activity: Activity) {
        val existsMap = load().associateBy { it.applicationId }.toMutableMap()
        val allList =
            ctx.packageManager.getInstalledPackages(0)
                .map {
                    val name = ApplicationLabelUtils.getApplicationLabel(
                        ctx,
                        ctx.packageManager,
                        it.applicationInfo,
                        it.packageName
                    )
                    AppInfo(name, it.packageName, it.firstInstallTime)
                }
        if (allList.isEmpty()) {
            return
        }

        val newList: List<AppInfo> = allList.filter {
            val reinstalled = !OneKeyListUtils.existsInOneKeyList(
                ctx,
                ctx.getString(R.string.sAutoFreezeApplicationList),
                it.applicationId
            ) && (existsMap[it.applicationId]?.firstInstallTime ?: 0) < it.firstInstallTime
            existsMap[it.applicationId] = it
            reinstalled
        }
        if (newList.isEmpty()) {
            save(existsMap)
            return
        }
        val nameList = newList.map { it.name }.toTypedArray()
        activity.runOnUiThread {
            try {
                showDialog(activity, nameList, existsMap, newList)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showDialog(
        activity: Activity,
        nameList: Array<String>,
        existsMap: Map<String, AppInfo>,
        newList: List<AppInfo>
    ) {
        running = true
        val selectedIndexSet = mutableSetOf<Int>()
        val dialog = AlertDialog.Builder(activity)
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
                save(existsMap)
                add(activity, newList)
            }.setPositiveButton("确定") { _, _ ->
                save(existsMap)
                if (selectedIndexSet.isEmpty()) {
                    return@setPositiveButton
                }
                add(activity, selectedIndexSet.map { newList[it] })
            }.show()
        dialog.setOnDismissListener {
            running = false
        }
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

    private fun save(existsMap: Map<String, AppInfo>) {
        val value = existsMap.values.joinToString("\n") {
            it.firstInstallTime.toString() + '/' + it.applicationId + '/' + it.name
        }
        sharedPreferences.edit().putString(key, value).apply()
    }

    private fun load(): List<AppInfo> {
        val value = sharedPreferences.getString(key, null) ?: return emptyList()
        return value.split('\n').map {
            val (timeString, applicationId, name) = it.split('/')
            AppInfo(name, applicationId, timeString.toLong())
        }
    }

    private data class AppInfo(
        val name: String,
        val applicationId: String,
        val firstInstallTime: Long,
    ) {
        override fun equals(other: Any?): Boolean {
            return applicationId == (other as? AppInfo)?.applicationId
        }

        override fun hashCode(): Int {
            return applicationId.hashCode()
        }
    }
}
