package xuanli.hbdx.xj.tvjoy.cn.study_hotfix.utils;

import android.content.Context;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

/**
 * author 岳明明
 * description
 * Created by yzm on 2017/9/9.
 */

public class PatchFix {

    //补丁文件夹
    private String patchUrl ;

    private String optUrl ;

    /**
     * 通过补丁修复bug
     * @param context
     */
    public void fixPatch(Context context){

        optUrl = context.getDir("opt", Context.MODE_PRIVATE).getAbsolutePath() ;

        //取出所有补丁文件
        File[] files = new File(patchUrl).listFiles() ;
        if(files.length>0){
            // 对补丁按照下载日期排序(从小到大)
            sortPatch(files) ;
            for (File file : files) {
                if(file.getName().endsWith(".dex")){   //补丁文件
                    //加载补丁
                    loadPatch(file, context);
                }
            }
        }
    }

    /**
     * 加载补丁
     * @param file
     */
    private void loadPatch(File file, Context context){
        //系统的classloader
        PathClassLoader pLoader = (PathClassLoader) context.getClassLoader();
        // 程序员用于加载 jar、dex、apk的classloader
        DexClassLoader dLoader = new DexClassLoader(file.getAbsolutePath(),optUrl, null, pLoader) ;

        Class<?> pc = BaseDexClassLoader.class ;
        try {
            Field fPathList = pc.getDeclaredField("pathList");
            fPathList.setAccessible(true);
            //获取DexPathList字段的值
            Object pathList = fPathList.get(pLoader);

            Class<?> plc = Class.forName("dalvik.system.DexPathList") ;
            Field fElements =  plc.getDeclaredField("dexElements") ;
            //压制虚拟机对访问权限的检查
            fElements.setAccessible(true);
            //获取该字段的值----取到了系统的dex数组
            Object pElements = fElements.get(pathList) ;

            //获取补丁的Elements
            Object dPathList =  fPathList.get(dLoader) ;
            Field fdElements = dPathList.getClass().getDeclaredField("dexElements") ;
            //获取补丁Elements值 ---取到了补丁的Elements[]
            Object dElements = fdElements.get(dPathList) ;

            //接下来要做的是：将dElements[]插入到pElements[]最前边
            int len = Array.getLength(pElements) + Array.getLength(dElements);
            Object newElements = Array.newInstance(dElements.getClass().getComponentType(), len);
            for(int i=0;i<len;i++){
                if(i<Array.getLength(dElements)){
                    //获取补丁dElement第i个下标的值
                    Object dValue = Array.get(dElements, i);
                    Array.set(newElements,i, dValue);
                }else{
                    //获取系统的pElments第i个下标的值
                    Object pValue = Array.get(pElements, i-Array.getLength(dElements)) ;
                    Array.set(newElements, i, pValue);
                }
            }
            // 给系统的DexElments数组赋了新的值，就是我们的新数组（补丁dex，在系统dex之前）
            fElements.set(dPathList, newElements);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 对所有补丁按下载日期排序
     * @param files
     */
    private void sortPatch(File[] files){

        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File file, File t1) {
                long d = t1.lastModified() - file.lastModified() ;
                if(d>0){
                    return -1 ;
                }else if(d<0){
                    return 1 ;
                }
                return 0;
            }
        });
    }
}
