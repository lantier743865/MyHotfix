package xuanli.hbdx.xj.tvjoy.cn.study_hotfix.bean;

/**
 * author 岳明明
 * description
 * Created by yzm on 2017/9/4.
 */

public class HotfixConfig {

    private String patchCode ;

    private String patchUrl ;

    private String tag = "就是想打个补丁了..." ;

    public String getPatchCode() {
        return patchCode;
    }

    public void setPatchCode(String patchCode) {
        this.patchCode = patchCode;
    }

    public String getPatchUrl() {
        return patchUrl;
    }

    public void setPatchUrl(String patchUrl) {
        this.patchUrl = patchUrl;
    }

    @Override
    public String toString() {
        return "HotfixConfig{" +
                "patchCode='" + patchCode + '\'' +
                ", patchUrl='" + patchUrl + '\'' +
                '}'+tag;
    }
}
