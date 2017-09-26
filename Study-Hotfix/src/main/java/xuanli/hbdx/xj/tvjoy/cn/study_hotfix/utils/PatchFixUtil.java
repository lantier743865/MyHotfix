package xuanli.hbdx.xj.tvjoy.cn.study_hotfix.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * author 岳明明
 * description
 * Created by yzm on 2017/9/5.
 */

public class PatchFixUtil {

    private static PatchFixUtil mPatchUtil ;

    private static final String TAG = PatchFixUtil.class.getSimpleName() ;

    private static final String PATCH_CODE = "patch_code" ;

    private static final String PATCH_CONFIG = "patch_config" ;

    private OkHttpClient mClient ;

    //检测补丁链接
    private static final String CHECK_URL = "http://192.168.2.121:8080/examples/hotfix/config/config.json" ;

    //补丁保存地址(文件夹)
    private String patchPath ;

    //hack.dex存放地址
    private String hackPath ;

    //补丁优化释放地址
    private String optPath ;

    private Context context ;

    private String patchCode ;

    private String patchUrl ;

    private PatchFixUtil(Context context){
        this.context = context.getApplicationContext() ;
        mClient = new OkHttpClient() ;
        initPatchPaths();
        loadHackDex() ;
    }

    /**
     *初始化补丁路径
     */
    private void initPatchPaths(){
        //  /data/data/com.yam.tet/app_hack
        File fHackPath = context.getDir("hack", Context.MODE_PRIVATE) ;
        createDirs(fHackPath);
        hackPath = fHackPath.getAbsolutePath() ;
        File fPatchPath = context.getDir("patch", Context.MODE_PRIVATE) ;
        createDirs(fPatchPath);
        patchPath = fPatchPath.getAbsolutePath() ;
        File fOptPath = context.getDir("opt", Context.MODE_PRIVATE) ;
        createDirs(fOptPath);
        optPath = fOptPath.getAbsolutePath() ;
    }

    /**
     * 创建文件夹
     * @param file
     */
    private void createDirs(File file){
        if(!file.exists()){
            file.mkdirs() ;
        }
    }

    public static PatchFixUtil getInstance(Context context){
        synchronized (PatchFixUtil.class){
            if(mPatchUtil == null){
                mPatchUtil = new PatchFixUtil(context) ;
            }
        }
        return mPatchUtil ;
    }

    private void loadHackDex(){
        //判断hack.dex是否存在
        File hack = new File(hackPath+File.separator+"hack.dex") ;
        if(!hack.exists()){
            //从Assets文件夹中拷贝到“hackPath”
            copyHackDex(hack);
            //加载hack.dex
            load(hack);
        }else{
            //加载hack.dex
            load(hack);
        }
        Log.d(TAG, "hack.dex加载完成...") ;
    }

    /**
     * 复制hack.dex到安全文件夹
     * @param hack
     * @return
     */
    private boolean copyHackDex(File hack){
        Log.d(TAG,"copy hack.dex...") ;
        try {
            OutputStream os = new FileOutputStream(hack) ;
            InputStream is = context.getAssets().open("hack.dex") ;
            int len ;
            byte[] buffer = new byte[1024] ;
            while((len=is.read(buffer,0,buffer.length))!=-1){
                os.write(buffer, 0, len);
            }
            os.close();
            is.close();
            Log.d(TAG,"copy hack.dex finish...") ;
            return true ;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false ;
    }

    /**
     * 加载hack.dex和所有补丁包
     */
    private void load(File hack) {
        loadPatch(hack);
        fixPatch() ;
    }

    /**
     * 操作补丁相关
     */
    public void doPatchFix(){
        //检测是否存在补丁包
        Request request = new Request.Builder()
                .get()
                .url(CHECK_URL)
                .build() ;
        Call call = mClient.newCall(request) ;
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "check patch failed....") ;
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                JSONObject jsonObject = JSON.parseObject(response.body().string());
                patchCode = jsonObject.getString("patchCode") ;
                Log.d(TAG, "pathCode:"+patchCode) ;

                //判断是否存在补丁
                if(!"-1".equals(patchCode)){
                    //判断当前补丁是否已经下载过
                    if(isFixed()){
                        Log.d(TAG, "this version pathCode is fixed...") ;
                        fixPatch();
                    }else{
                        //获取补丁链接
                        patchUrl = jsonObject.getString("patchUrl") ;
                        //开启补丁下载
                        downLoadPatch() ;
                    }
                }
            }
        });
    }

    /**
     * 下载补丁
     */
    private void downLoadPatch() {
        Request request = new Request.Builder()
                .get()
                .url(patchUrl)
                .build() ;
        Call call = mClient.newCall(request) ;
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "the patch download failed...") ;
                Log.d(TAG, e.getMessage()) ;
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                byte[] buffer = new byte[2048] ;
                int len = 0  ;
                OutputStream os = new FileOutputStream(patchPath+File.separator+patchCode+".dex") ;
                InputStream is = response.body().byteStream() ;
                Log.d(TAG, "start download patch...") ;
                while((len=is.read(buffer,0,buffer.length))!=-1){
                    os.write(buffer,0, len);
                }
                os.close();
                is.close();
                Log.d(TAG, "download patch finish...") ;
                Log.d(TAG, "start fixed patch...") ;
            }
        });
    }

    /**
     * 修复补丁(热修复的核心代码)
     * 几个知识点：
     *
     *  1. Android类加载机制中2个重要类加载器：”PathClassLoader“和”DexClassLoader“均继承自BaseDexClassLoader
     *    PathClassLoader：用于加载系统类(类均在dex文件中)，PS：app运行时，会一并将本app所有类加载
     *    DexClassLoader：用于加载应用类，我们自己的动态加载的类
     *
     *  2. 系统运行APP的时候，会用“PathClassLoader”装载运行该app所有需的dex，从java层面，这个过程我们是无法干预的
     *
     *  3. 当app运行中，用到某个类A的时候，PathClassLoader会去查找自己的PathList中的Element[]数组中查找类A，
     *    一旦找到，立马返回，不去管Element[]数组中是否还有一个类A
     *
     *  4. 通过源码，可知道Element类，是PathList的一个内部类，其中存储的是dex文件、dex路径等等，找类A的真正实现，
     *     是通过本地方法从dex文件中查找的。结论：用到某个类时，PathClassLoader会通过PathList遍历Elements[]中的dex查找此类
     *
     *  通过以上几个知识点，我们可以想到什么？假如：我们发布的app中的A类存在bug，我们修改A类bug后，将其打包成dex文件，
     *                                              并插入到Elements[]数组的最前端，是不是就成功替换了bugA类？因为，
     *                                              PathClassLoader从Elements[]中找到最前面的修复BUG的A类，就返回了，不会再调用到
     *                                              有BUG的A类了。
     *
     *  注意点：PathClassLoader和DexClassLoader中代码量很少，真正查找类的方法都在其父类BaseDexClassLoader中，
     *          所以需要从BaseDexClassLoader类入手，通过反射机制实现
     */
    private void fixPatch() {
        //获取patch文件夹下所有的补丁文件
        File[] files = new File(patchPath).listFiles() ;
        if(files.length>0){
            //补丁按日期排序(最新补丁放前面)
            patchSort(files);
            for (File file : files) {
                //判断file是否为补丁
                if(file.isFile() && file.getAbsolutePath().endsWith(".dex")){
                    System.out.println("---:"+file.getName());
                    loadPatch(file);
                }
            }
            Log.d(TAG, "fiexd success....") ;
        }
    }

    /**
     * 加载补丁
     * @param file
     */
    private void loadPatch(File file){
        Log.d(TAG, file.getAbsolutePath()) ;
        if(file.exists()){
            Log.d(TAG,"文件存在...") ;
        }else{
            Log.d(TAG, "文件不存在...") ;
        }
        //获取系统PathClassLoader
        PathClassLoader pLoader = (PathClassLoader) context.getClassLoader();
        //获取PathClassLoader中的PathList
        Object pPathList = getPathList(pLoader) ;
        if(pPathList == null){
            Log.d(TAG, "get PathClassLoader pathlist failed...") ;
            return ;
        }
        //加载补丁
        DexClassLoader dLoader = new DexClassLoader(file.getAbsolutePath(),optPath, null, pLoader) ;
        //获取DexClassLoader的pathLit，即BaseDexClassLoader中的pathList
        Object dPathList = getPathList(dLoader) ;
        if(dPathList == null){
            Log.d(TAG, "get DexClassLoader pathList failed...") ;
            return ;
        }
        //获取DexElements
        Object pElements = getElements(pPathList) ;
        Object dElements = getElements(dPathList) ;

        //将补丁dElements[]插入系统pElements[]的最前面
        Object newElements = insertElements(pElements, dElements) ;
        if(newElements == null){
            Log.d(TAG, "patch insert failed...") ;
            return ;
        }
        //用插入补丁后的新Elements[]替换系统Elements[]
        try {
            Field fElements = pPathList.getClass().getDeclaredField("dexElements") ;
            fElements.setAccessible(true);
            fElements.set(pPathList, newElements);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "fixed failed....") ;
            return ;
        }
    }

    /**
     * 将补丁插入系统DexElements[]最前端
     * @param pElements
     * @param dElements
     * @return
     */
    private Object insertElements(Object pElements, Object dElements){
        //判断是否为数组
        if(pElements.getClass().isArray() && dElements.getClass().isArray()){
            //获取数组长度
            int pLen = Array.getLength(pElements) ;
            int dLen = Array.getLength(dElements) ;
            //创建新数组
            Object newElements = Array.newInstance(pElements.getClass().getComponentType(), pLen+dLen) ;
            //循环插入
            for(int i=0; i<pLen+dLen;i++){
                if(i<dLen){
                    Array.set(newElements, i, Array.get(dElements, i));
                }else{
                    Array.set(newElements, i, Array.get(pElements, i-dLen)) ;
                }
            }
            return newElements ;
        }
        return null ;
    }

    /**
     *  获取DexElements
     * @param object
     * @return
     */
    private Object getElements(Object object){
        try {
            Class<?> c = object.getClass() ;
            Field fElements = c.getDeclaredField("dexElements") ;
            fElements.setAccessible(true);
            Object obj = fElements.get(object) ;
            return obj ;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null ;
    }

    /**
     * 通过反射机制获取PathList
     * @param loader
     * @return
     */
    private Object getPathList(BaseDexClassLoader loader){
        try {
            Class<?> c = Class.forName("dalvik.system.BaseDexClassLoader") ;
            //获取成员变量pathList
            Field fPathList = c.getDeclaredField("pathList") ;
            //抑制jvm检测访问权限
            fPathList.setAccessible(true);
            //获取成员变量pathList的值
            Object obj = fPathList.get(loader) ;
            return obj ;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null ;
    }

    /**
     * 对补丁按日期排序
     * @param files
     */
    private void patchSort(File[] files){
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File file, File t1) {
                System.out.println(file.getName()+":"+file.lastModified());
                System.out.println(t1.getName()+":"+t1.lastModified());
                //从大到小排序
                long d = t1.lastModified() - file.lastModified() ;
                if(d>0){
                    return -1 ;
                }else if(d<0){
                    return 1 ;
                }else{
                    return 0 ;
                }
            }
            @Override
            public boolean equals(Object obj) {
                return true ;
            }
        });
    }

    /**
     * 判断此版本补丁是否已修复
     * @return
     */
    private boolean isFixed(){
        SharedPreferences sp = context.getSharedPreferences(PATCH_CONFIG, Context.MODE_PRIVATE) ;
        String code = sp.getString(PATCH_CODE, "") ;
        if(code.equals(patchCode)){
            return true ;
        }
        return false ;
    }
}
