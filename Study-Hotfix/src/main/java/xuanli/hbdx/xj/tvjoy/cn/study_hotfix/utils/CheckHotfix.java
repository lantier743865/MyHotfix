package xuanli.hbdx.xj.tvjoy.cn.study_hotfix.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xuanli.hbdx.xj.tvjoy.cn.study_hotfix.bean.HotfixConfig;
import xuanli.hbdx.xj.tvjoy.cn.study_hotfix.interfaces.HotfixCallBack;

/**
 * author 岳明明
 * description
 * Created by yzm on 2017/9/4.
 */

public class CheckHotfix {

    private static final String HOTFIX_SP = "hotfix_sp" ;
    private static final String HOTFIX_CODE = "hotfix_code" ;
    private static final String checkUrl = "http://192.168.2.121:8080/examples/hotfix/config/config.json" ;
    private static OkHttpClient client = new OkHttpClient() ;

    /**
     * 检测补丁
     * @param context
     * @param callBack
     */
    public static void check(final Context context, final HotfixCallBack callBack){
        System.out.println("网络请求失败..");
        Request request = new Request.Builder()
                .url(checkUrl)
                .build() ;
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.out.println("网络请求失败..");
                //callBack.result(1);
                //判断当前是否存在补丁
                SharedPreferences sp = context.getSharedPreferences(HOTFIX_SP, Context.MODE_PRIVATE) ;
                String code = sp.getString(HOTFIX_CODE,"") ;
                if(!TextUtils.isEmpty(code)){
                    File filePath = context.getDir("patch",Context.MODE_PRIVATE) ;
                    File file = new File(filePath.getAbsolutePath()+File.separator+code+".dex") ;
                    System.out.println("最后修改日期："+file.lastModified());
                    //直接修复
                    fixPatch(context, file.getAbsolutePath(),callBack);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String res = response.body().string() ;
                checkDownLoad(context, res, callBack);
                System.out.println("请求成功："+res);
            }
        });
    }

    /**
     * 检测下载补丁
     * @param context
     * @param res
     * @param callBack
     */
    private static void checkDownLoad(Context context, String res, HotfixCallBack callBack){
        HotfixConfig hotfixConfig = JSON.parseObject(res, HotfixConfig.class);
        System.out.println("请求结果："+hotfixConfig.toString());
        //判断是否存在修复补丁
        if(!hotfixConfig.getPatchCode().equals("-1")){
            //判断此版本的补丁是否已修复
            SharedPreferences sp = context.getSharedPreferences(HOTFIX_SP, Context.MODE_PRIVATE) ;
            String code = sp.getString(HOTFIX_CODE,"") ;
            if(!code.equals(hotfixConfig.getPatchCode())){
                //开始下载补丁
                downDex(context, hotfixConfig, callBack) ;
            }else{
                File filePath = context.getDir("patch",Context.MODE_PRIVATE) ;
                if(!filePath.exists()){
                    filePath.mkdirs() ;
                }
                File file = new File(filePath.getAbsolutePath()+File.separator+hotfixConfig.getPatchCode()+".dex") ;
                System.out.println("最后修改日期："+file.lastModified());
                fixPatch(context, file.getAbsolutePath(),callBack);
                //此版本bug已修复
                //callBack.result(2);
            }
        }else{
            //callBack.result(0);
        }
    }

    private static void downDex(final Context context, final HotfixConfig hotfixConfig, final HotfixCallBack callBack){
        Request request = new Request.Builder()
                .url(hotfixConfig.getPatchUrl())
                .build() ;
        Call call = client.newCall(request) ;
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //补丁下载失败
                //callBack.result(3);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                File filePath = context.getDir("patch",Context.MODE_PRIVATE) ;
                if(!filePath.exists()){
                    filePath.mkdirs() ;
                }
                File file = new File(filePath.getAbsolutePath()+File.separator+hotfixConfig.getPatchCode()+".dex") ;

                OutputStream os = new FileOutputStream(file) ;
                InputStream is = response.body().byteStream();
                byte[] buffer = new byte[2048] ;
                int len ;
                while((len=is.read(buffer,0,buffer.length))!=-1){
                    os.write(buffer,0,len);
                }
                is.close();
                os.close();
                System.out.println("最后修改日期："+file.lastModified());
                SharedPreferences sp = context.getSharedPreferences(HOTFIX_SP, Context.MODE_PRIVATE) ;
                sp.edit().putString(HOTFIX_CODE, hotfixConfig.getPatchCode()).commit() ;
                //下载完成
                //callBack.result(4);
                System.out.println("补丁下载完成...");
                //安装补丁
                fixPatch(context, file.getAbsolutePath(), callBack);
            }
        });
    }

    /**
     * 安装补丁
     * @param context
     * @param dexPath
     */
    private static void fixPatch(Context context, String dexPath, HotfixCallBack callBack){
        File optPath = new File(context.getDir("opt", Context.MODE_PRIVATE).getAbsolutePath()) ;
        if(!optPath.exists()){
            optPath.mkdirs() ;
        }
        PathClassLoader pLoader = (PathClassLoader) context.getClassLoader();
        DexClassLoader dLoader = new DexClassLoader(dexPath, optPath.getAbsolutePath(), null, pLoader) ;

        //通过反射机制获取BaseDexClassLoader中DexPathList中的Elements[]数组(其中保存了dex文件)
        //获取去PathClassLoader(系统的类加载器)的Elements[]
        Object pElements = getElements(pLoader) ;
        //获取DexClassLoader(我们自己的类加载器)的Elements[]
        Object dElements = getElements(dLoader) ;

        if(pElements != null && dElements != null) {
            //将我们加载的Elements[]插入到系统加载的Elments[]的前面
            //获取Element[]数组长度
            int pLen = Array.getLength(pElements);
            int dLen = Array.getLength(dElements);
            System.out.println("pLen:"+pLen);
            System.out.println("dLen:"+dLen);
            //创建用于合并的新数组
            Class<?> componentType = pElements.getClass().getComponentType();
            Object newElements = Array.newInstance(componentType, pLen + dLen);
            //通过循环将dElement[]插入pElements[]前面
            for (int i = 0; i < Array.getLength(newElements); i++) {
                if (i < dLen) {
                    Array.set(newElements, i, Array.get(dElements, i));
                } else {
                    Array.set(newElements, i, Array.get(pElements, i-dLen));
                }
            }
            //将插入后的新数组复制给系统的BaseDexClassLoader中DexPathList中的Elements[]数组
            try {
                Class<?> baseObj = Class.forName("dalvik.system.BaseDexClassLoader");
                Field dPathList = baseObj.getDeclaredField("pathList");
                dPathList.setAccessible(true);
                Object pathList = dPathList.get(pLoader);
                Field f = pathList.getClass().getDeclaredField("dexElements");
                f.setAccessible(true);
                f.set(pathList, newElements);
                //补丁安装成功
                //callBack.result(6);
                System.out.println("补丁安装成功!");
            } catch (Exception e) {
                e.printStackTrace();
                //callBack.result(5);
            }
        }else{
            //callBack.result(5);
        }
    }

    /**
     * 通过反射机制获取Elements[]数组
     * @param object
     * @return
     */
    private static Object getElements(Object object){
        try {
            Class<?> obj = Class.forName("dalvik.system.BaseDexClassLoader") ;
            //获取成员变量DexPathList
            Field fDexPathList = obj.getDeclaredField("pathList") ;
            //压制虚拟机检测访问权限
            fDexPathList.setAccessible(true);
            //pathList的值
            Object dexPathList = fDexPathList.get(object) ;
            //获取PathList中的字段Elements[]数组
            Field fElements = dexPathList.getClass().getDeclaredField("dexElements") ;
            fElements.setAccessible(true);
            //获取Elements数组值
            return fElements.get(dexPathList) ;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null ;
    }
}
