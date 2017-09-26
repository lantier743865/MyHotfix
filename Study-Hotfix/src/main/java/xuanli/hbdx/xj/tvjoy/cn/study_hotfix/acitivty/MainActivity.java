package xuanli.hbdx.xj.tvjoy.cn.study_hotfix.acitivty;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import xuanli.hbdx.xj.tvjoy.cn.study_hotfix.R;
import xuanli.hbdx.xj.tvjoy.cn.study_hotfix.bean.People;
import xuanli.hbdx.xj.tvjoy.cn.study_hotfix.utils.PatchFixUtil;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PatchFixUtil.getInstance(this).doPatchFix();
    }

    public void jumpToA2(View v){
        Intent intent = new Intent(this, Activity2.class) ;
        startActivity(intent) ;
    }

   public void hotFixBug(View v){
        Toast.makeText(this, new People(20, "小明").toString(), Toast.LENGTH_LONG).show() ;
   }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.exit(0);
    }
}
