package xuanli.hbdx.xj.tvjoy.cn.study_hotfix.application;

import android.app.Application;

import xuanli.hbdx.xj.tvjoy.cn.study_hotfix.utils.PatchFixUtil;

/**
 * author 岳明明
 * description
 * Created by yzm on 2017/9/6.
 */

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //加载hack.dex
        PatchFixUtil.getInstance(this);
    }
}
