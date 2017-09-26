package xuanli.hbdx.xj.tvjoy.cn.study_hotfix.acitivty;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * author 岳明明
 * description
 * Created by yzm on 2017/9/1.
 */

public class Activity2 extends Activity {

    private TextView view ;

    private RelativeLayout rootLayou ;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        setContentView(rootLayou);
    }

    private void init(){
        rootLayou = new RelativeLayout(this) ;
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) ;
        rootLayou.setLayoutParams(params);

        view = new TextView(this) ;
        view.setText("Activit2");
        view.setTextColor(Color.parseColor("#ff0000"));
        view.setTextSize(26);
        RelativeLayout.LayoutParams vp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT) ;
        vp.addRule(RelativeLayout.CENTER_IN_PARENT);
        view.setLayoutParams(vp);
        rootLayou.addView(view);
    }
}
