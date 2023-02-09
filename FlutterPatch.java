
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.taobao.sophix.SophixManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.flutter.FlutterInjector;
import io.flutter.embedding.engine.loader.FlutterApplicationInfo;
import io.flutter.embedding.engine.loader.FlutterLoader;

public class FlutterPatch {
    private static final String TAG = "FlutterPatch";
    private static String libPathFromSophix = "";

    /**
     * 更新补丁
     */
    public static void findNewPatch(Context context) {
        Log.i(TAG, "--------------------版本1.0.0-0------------------");
        Log.i(TAG, "--------------------补丁版本-0------------------");
        SharedPreferences settings = context.getSharedPreferences("LoadNewPatch", 0);
        long now = System.currentTimeMillis();
        Log.i(TAG, "--当前时间-"+now);
        long last = settings.getLong("LoadNewPatchTime", 0L);
        Log.i(TAG, "--上一次更新时间-"+last);
        if(last == 0L || timeCompare(last,now,80)) {
            SophixManager.getInstance().queryAndLoadNewPatch();
        }
    }

    ///相差分钟
    private static boolean timeCompare(long date1, long date2, int basicDiff) {
        long nd = 1000 * 24 * 60 * 60;// 一天的毫秒数
        long nh = 1000 * 60 * 60;// 一小时的毫秒数
        long nm = 1000 * 60;// 一分钟的毫秒数
        long ns = 1000;// 一秒钟的毫秒数
        //格式化时间
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String startDate = simpleDateFormat.format(date1);//开始的时间戳
        String endDate = simpleDateFormat.format(date2);//结束的时间戳
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date beginTime = df.parse(startDate);
            Date endTime = df.parse(endDate);
            assert endTime != null;
            assert beginTime != null;
            long diff = endTime.getTime() - beginTime.getTime();
            long mins = diff / nm;
            return mins >= basicDiff;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String findLibraryFromSophix(Context context, String relativePath, String libName) throws UnsatisfiedLinkError {
        File file = new File(relativePath+"/libs/libapp.so");
        if (file.exists() && !file.isDirectory()) {
            libName = file.getAbsolutePath();
            Log.i(TAG, "so路径 is " + libName);
        } else {
            Log.i(TAG, "so路径 is not exist");
        }
        return libName;
    }

    public static void reflect(String libPath) {
        File file = new File(libPath);
        if (file.exists() && !file.isDirectory()) {
            try {
                FlutterLoader flutterLoader = FlutterInjector.instance().flutterLoader();
                Field field = FlutterLoader.class.getDeclaredField("flutterApplicationInfo");
                field.setAccessible(true);
                FlutterApplicationInfo flutterApplicationInfo = (FlutterApplicationInfo)field.get(flutterLoader);
                Field aotSharedLibraryNameField = FlutterApplicationInfo.class.getDeclaredField("aotSharedLibraryName");
                aotSharedLibraryNameField.setAccessible(true);
                aotSharedLibraryNameField.set(flutterApplicationInfo, libPath);
                field.set(flutterLoader, flutterApplicationInfo);
                Log.i(TAG, "sophix flutter patch is loaded successfully");
            } catch (Exception var5) {
                Log.i(TAG, "sophix flutter reflect is failed");
                var5.printStackTrace();
            }
        }
    }

    public static void copyFileByPath(String fromPath, String toPath) {
        File fromFile = new File(fromPath);
        File toFile = new File(toPath);
        if (!fromFile.exists()) {
            return;
        }
        if (!fromFile.isFile()) {
            return;
        }
        if (!fromFile.canRead()) {
            return;
        }
        if (!toFile.getParentFile().exists()) {
            toFile.getParentFile().mkdirs();
        }
        if (toFile.exists()) {
            toFile.delete();
        }
        try {
            FileInputStream fosfrom = new FileInputStream(fromFile);
            FileOutputStream fosto = new FileOutputStream(toFile);
            byte[] bt = new byte[1024];
            int c;
            while((c=fosfrom.read(bt)) > 0){
                fosto.write(bt,0,c);
            }
            //关闭输入、输出流
            fosfrom.close();
            fosto.close();
        } catch (IOException e) {
            e.printStackTrace();

        }
    }


}
