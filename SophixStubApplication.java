

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Keep;

import com.taobao.sophix.PatchStatus;
import com.taobao.sophix.SophixApplication;
import com.taobao.sophix.SophixEntry;
import com.taobao.sophix.SophixManager;
import com.taobao.sophix.listener.PatchLoadStatusListener;

import java.io.File;

/**
 * Sophix入口类，专门用于初始化Sophix，不应包含任何业务逻辑。
 * 此类必须继承自SophixApplication，onCreate方法不需要实现。
 * 此类不应与项目中的其他类有任何互相调用的逻辑，必须完全做到隔离。
 * AndroidManifest中设置application为此类，而SophixEntry中设为原先Application类。
 * 注意原先Application里不需要再重复初始化Sophix，并且需要避免混淆原先Application类。
 * 如有其它自定义改造，请咨询官方后妥善处理。
 */
public class SophixStubApplication extends SophixApplication {
    private final String TAG = "SophixStubApplication";
    // 此处SophixEntry应指定真正的Application，并且保证RealApplicationStub类名不被混淆。
    @Keep
    @SophixEntry(AppApplication.class)
    static class RealApplicationStub {}
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
//         如果需要使用MultiDex，需要在此处调用。
//         MultiDex.install(this);
        initSophix();
    }
    private void initSophix() {
        String appVersion = "0.0.0";
        try {
            appVersion = this.getPackageManager()
                    .getPackageInfo(this.getPackageName(), 0)
                    .versionName;
        } catch (Exception e) {
        }
        final SophixManager instance = SophixManager.getInstance();
        instance.setContext(this)
                .setAppVersion(appVersion)
                .setSecretMetaData(null, null, null)
                .setEnableDebug(true)
                .setEnableFullLog()
                .setPatchLoadStatusStub(new PatchLoadStatusListener() {
                    @Override
                    public void onLoad(final int mode, final int code, final String info, final int handlePatchVersion) {
                        if (code == PatchStatus.CODE_LOAD_SUCCESS) {
                            String filesPath = getBaseContext().getFilesDir().getAbsolutePath();
                            String parentPath = getBaseContext().getFilesDir().getParentFile().getAbsolutePath();
                            String patchFilePath = FlutterPatch.findLibraryFromSophix(getBaseContext(), filesPath + "/sophix", "libapp.so");
                            SharedPreferences settings = getBaseContext().getSharedPreferences("FlutterSharedPreferences", 0);
                            int sophixPatchVersion = settings.getInt("flutter.sophixPatchVersion", -99);
                            Log.i(TAG, "parent so路径 is " + parentPath + "/libapp.so");
                            boolean isNewPatch = sophixPatchVersion == -99 || handlePatchVersion != sophixPatchVersion;
                            if (!new File(parentPath + "/libapp.so").exists() || isNewPatch) {
                                //拷贝libapp.so到配置好的加载路径
                                /*FlutterPatch.copyFileByPath(patchFilePath, parentPath + "/libapp.so");
                                File sof = new File(parentPath + "/libapp.so");
                                sof.setExecutable(true);
                                sof.setReadable(true);*/
                                SharedPreferences.Editor editor = settings.edit();
                                editor.putInt("flutter.sophixPatchVersion", handlePatchVersion);
                                editor.commit();
                                Log.i(TAG, "sophix补丁加载成功!版本号为" + handlePatchVersion);
                            }
                        } else if (code == PatchStatus.CODE_LOAD_RELAUNCH) {
                            // 如果需要在后台重启，建议此处用SharePreference保存状态。
                            long now = System.currentTimeMillis();
                            SharedPreferences settings = getBaseContext().getSharedPreferences("LoadNewPatch", 0);
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putLong("LoadNewPatchTime", now);
                            editor.commit();
                            Looper.prepare();
                            Toast.makeText(getBaseContext(), "检测到版本更新完成，重启后生效", Toast.LENGTH_SHORT).show();
                            Looper.loop();
                            Log.i(TAG, "sophix补丁预加载成功. 重启后生效.本次加载时间为" + now);
                        } else if (code == PatchStatus.CODE_REQ_NOUPDATE || code == PatchStatus.CODE_REQ_NOTNEWEST
                                || code == PatchStatus.CODE_DOWNLOAD_BROKEN || code == PatchStatus.CODE_UNZIP_FAIL
                                || code == PatchStatus.CODE_REQ_UNAVAIABLE || code == PatchStatus.CODE_REQ_SYSTEMERR) {
                            long now = System.currentTimeMillis();
                            SharedPreferences settings = getBaseContext().getSharedPreferences("LoadNewPatch", 0);
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putLong("LoadNewPatchTime", now);
                            editor.commit();
                            Log.i(TAG, "sophix查询结束.本次加载时间为" + now);
                        }
                    }

                }).initialize();
    }
}
