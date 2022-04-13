package cf.playhi.freezeyou.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cf.playhi.freezeyou.R;
import cf.playhi.freezeyou.adapter.ReplaceableSimpleAdapter;
import cf.playhi.freezeyou.app.FreezeYouBaseActivity;

import static cf.playhi.freezeyou.utils.ApplicationIconUtils.getApplicationIcon;
import static cf.playhi.freezeyou.utils.ApplicationLabelUtils.getApplicationLabel;
import static cf.playhi.freezeyou.utils.FUFUtils.realGetFrozenStatus;
import static cf.playhi.freezeyou.utils.MoreUtils.processListFilter;
import static cf.playhi.freezeyou.utils.OneKeyListUtils.existsInOneKeyList;
import static cf.playhi.freezeyou.utils.ThemeUtils.getThemeDot;
import static cf.playhi.freezeyou.utils.ThemeUtils.getThemeSecondDot;
import static cf.playhi.freezeyou.utils.ThemeUtils.processActionBar;
import static cf.playhi.freezeyou.utils.ThemeUtils.processSetTheme;
import static cf.playhi.freezeyou.utils.ToastUtils.showToast;

public class FUFLauncherShortcutCreator extends FreezeYouBaseActivity {

    private int customThemeDisabledDot = R.drawable.shapedotblue;
    private int customThemeEnabledDot = R.drawable.shapedotblack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        processSetTheme(this);
        super.onCreate(savedInstanceState);
        processActionBar(getSupportActionBar());

        final Intent intent = getIntent();
        String slf_n = intent.getStringExtra("slf_n");
        final boolean returnPkgName = intent.getBooleanExtra("returnPkgName", false);
        final boolean isSlfMode = slf_n != null;

        try {
            customThemeDisabledDot = getThemeDot(this);
            customThemeEnabledDot = getThemeSecondDot(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (isSlfMode || returnPkgName) {
            setContentView(R.layout.fuflsc_select_application);

            if (isSlfMode)
                setTitle(R.string.add);
            else
                setTitle(R.string.plsSelect);

            new Thread(() -> {

                final ListView app_listView = findViewById(R.id.fuflsc_app_list);
                final ProgressBar progressBar = findViewById(R.id.fuflsc_progressBar);
                final LinearLayout linearLayout = findViewById(R.id.fuflsc_linearLayout);
                final ArrayList<Map<String, Object>> AppList = new ArrayList<>();
                final EditText search_editText = findViewById(R.id.fuflsc_search_editText);
                runOnUiThread(() -> {
                    linearLayout.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.VISIBLE);
                    app_listView.setVisibility(View.GONE);
                });
                app_listView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
                final Context applicationContext = getApplicationContext();
                PackageManager packageManager = applicationContext.getPackageManager();
                List<ApplicationInfo> applicationInfo = packageManager.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
                int size = applicationInfo == null ? 0 : applicationInfo.size();
                for (int i = 0; i < size; i++) {
                    ApplicationInfo applicationInfo1 = applicationInfo.get(i);
                    Map<String, Object> keyValuePair = processAppStatus(
                            getApplicationLabel(applicationContext, packageManager, applicationInfo1, applicationInfo1.packageName),
                            applicationInfo1.packageName,
                            applicationInfo1,
                            packageManager
                    );
                    if (keyValuePair != null) {
                        AppList.add(keyValuePair);
                    }
                }

                if (!AppList.isEmpty()) {
                    Collections.sort(AppList, (stringObjectMap, t1) ->
                            ((String) stringObjectMap.get("PackageName"))
                                    .compareTo((String) t1.get("PackageName")));
                }

                final ReplaceableSimpleAdapter adapter =
                        new ReplaceableSimpleAdapter(
                                FUFLauncherShortcutCreator.this,
                                (ArrayList<Map<String, Object>>) AppList.clone(),
                                R.layout.app_list_1,
                                new String[]{"Img", "Name", "PackageName", "isFrozen"},
                                new int[]{R.id.img, R.id.name, R.id.pkgName, R.id.isFrozen});//isFrozen、isAutoList传图像资源id

                adapter.setViewBinder(new ReplaceableSimpleAdapter.ViewBinder() {
                    public boolean setViewValue(View view, Object data, String textRepresentation) {
                        if (view instanceof ImageView && data instanceof Drawable) {
                            ((ImageView) view).setImageDrawable((Drawable) data);
                            return true;
                        } else
                            return false;
                    }
                });

                search_editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        if (TextUtils.isEmpty(charSequence)) {
                            adapter.replaceAllInFormerArrayList(AppList);
                        } else {
                            adapter.replaceAllInFormerArrayList(processListFilter(charSequence, AppList));
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    linearLayout.setVisibility(View.GONE);
                    app_listView.setAdapter(adapter);
                    app_listView.setTextFilterEnabled(true);
                    app_listView.setVisibility(View.VISIBLE);
                });

                app_listView.setOnItemClickListener((adapterView, view, i, l) -> {
                    HashMap<String, Object> map = (HashMap<String, Object>) app_listView.getItemAtPosition(i);
                    final String name = (String) map.get("Name");
                    final String pkgName = (String) map.get("PackageName");
                    if (isSlfMode) {
                        SharedPreferences sp = getSharedPreferences(getIntent().getStringExtra("slf_n"), MODE_PRIVATE);
                        if (!existsInOneKeyList(sp.getString("pkgS", ""), pkgName)) {
                            sp.edit().putString("pkgS", sp.getString("pkgS", "") + pkgName + ",").apply();
                            showToast(FUFLauncherShortcutCreator.this, R.string.added);
                        } else {
                            showToast(FUFLauncherShortcutCreator.this, R.string.alreadyExist);
                        }
                        setResult(RESULT_OK);
                    } else {// if (returnPkgName)s
                        setResult(RESULT_OK, new Intent()
                                .putExtra("pkgName", pkgName)
                                .putExtra("name", name)
                                .putExtra("id", "FreezeYou! " + pkgName));
                        finish();
                    }
                });
            }).start();

        } else {
            finish();
        }

    }

    private Map<String, Object> processAppStatus(String name, String packageName, ApplicationInfo applicationInfo, PackageManager packageManager) {
        if (!("android".equals(packageName) || "cc.aoeiuv020.freezeyou".equals(packageName))) {
            Map<String, Object> keyValuePair = new HashMap<>();
            keyValuePair.put("Img", getApplicationIcon(FUFLauncherShortcutCreator.this, packageName, applicationInfo, true));
            keyValuePair.put("Name", name);
            processFrozenStatus(keyValuePair, packageName, packageManager);
            keyValuePair.put("PackageName", packageName);
            return keyValuePair;
        }
        return null;
    }

    private void processFrozenStatus(Map<String, Object> keyValuePair, String packageName, PackageManager packageManager) {
        keyValuePair.put("isFrozen", getFrozenStatus(packageName, packageManager));
    }

    /**
     * @param packageName 应用包名
     * @return 资源 Id
     */
    private int getFrozenStatus(String packageName, PackageManager packageManager) {
        return realGetFrozenStatus(FUFLauncherShortcutCreator.this, packageName, packageManager)
                ? customThemeDisabledDot : customThemeEnabledDot;
    }

}
