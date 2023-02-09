
import android.content.Context;


import io.flutter.app.FlutterApplication;


public class AppApplication extends FlutterApplication {


    // 测试sophix时，请注掉attachBaseContext
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //直接干
        String filesPath = getBaseContext().getFilesDir().getAbsolutePath();
        String patchFilePath = FlutterPatch.findLibraryFromSophix(getBaseContext(), filesPath + "/sophix", "libapp.so");
        FlutterPatch.reflect(patchFilePath);
    }
}
