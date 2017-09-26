package xuanli.hbdx.xj.tvjoy.cn.study_hotfix.bean;

/**
 * author 岳明明
 * description
 * Created by yzm on 2017/9/5.
 */

public class People {
    private int age ;
    private String name ;

    public People(int age, String name) {
        this.age = age;
        this.name = name;
        System.out.println(this.toString());
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "People{" +
                "age=" + age +
                ", name=" + name +
                '}';
    }
}
