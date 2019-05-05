package cf.playhi.freezeyou;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import java.io.DataOutputStream;

import static cf.playhi.freezeyou.ToastUtils.showToast;

final class DevicePolicyManagerUtils {

    static DevicePolicyManager getDevicePolicyManager(Context context) {
        return (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    static void openDevicePolicyManager(Context context) {
        showToast(context, R.string.needActiveAccessibilityService);
        ComponentName componentName = new ComponentName(context, DeviceAdminReceiver.class);
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
        context.startActivity(intent);
    }

    /**
     * 优先 ROOT 模式锁屏，失败则尝试 免ROOT 模式锁屏
     *
     * @param context Context
     */
    static void doLockScreen(Context context) {
        //先走ROOT，有权限的话就可以不影响SmartLock之类的了
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());
            outputStream.writeBytes("input keyevent KEYCODE_POWER" + "\n");
            outputStream.writeBytes("exit\n");
            outputStream.flush();
            process.waitFor();
            ProcessUtils.destroyProcess(outputStream, process);
        } catch (Exception e) {
            e.printStackTrace();
        }

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm == null || pm.isScreenOn()) {
            DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName componentName = new ComponentName(context, DeviceAdminReceiver.class);
            if (devicePolicyManager != null) {
                if (devicePolicyManager.isAdminActive(componentName)) {
                    devicePolicyManager.lockNow();
                } else {
                    openDevicePolicyManager(context);
                }
            } else {
                showToast(context, R.string.devicePolicyManagerNotFound);
            }
        }
    }

}
