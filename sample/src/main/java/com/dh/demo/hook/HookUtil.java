package com.dh.demo.hook;

import android.content.Context;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by Larry on 2017/4/10. 只能监听自己应用进程的启动，Hook只针对本进程，其他应用监听不到。
 */

public class HookUtil {

    private Class<?> proxyActivity;

    private Context context;

    public HookUtil(Class<?> proxyActivity, Context context) {
        this.proxyActivity = proxyActivity;
        this.context = context;
    }

    public void hookAms() {

        // 一路反射，直到拿到IActivityManager的对象
        try {
            Class<?> ActivityManagerNativeClss = Class.forName("android.app.ActivityManagerNative");
            Field defaultFiled = ActivityManagerNativeClss.getDeclaredField("gDefault");
            defaultFiled.setAccessible(true);
            Object defaultValue = defaultFiled.get(null);
            // 反射SingleTon
            Class<?> SingletonClass = Class.forName("android.util.Singleton");
            Field mInstance = SingletonClass.getDeclaredField("mInstance");
            mInstance.setAccessible(true);
            // 到这里已经拿到ActivityManager对象
            Object iActivityManagerObject = mInstance.get(defaultValue);


            // 开始动态代理，用代理对象替换掉真实的ActivityManager，瞒天过海
            Class<?> IActivityManagerIntercept = Class.forName("android.app.IActivityManager");

            AmsInvocationHandler handler = new AmsInvocationHandler(iActivityManagerObject);

            Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                    new Class<?>[] {IActivityManagerIntercept}, handler);

            // 现在替换掉这个对象
            mInstance.set(defaultValue, proxy);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class AmsInvocationHandler implements InvocationHandler {

        private Object iActivityManagerObject;

        private AmsInvocationHandler(Object iActivityManagerObject) {
            this.iActivityManagerObject = iActivityManagerObject;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            String argString = "";

            if (args != null && args.length > 0) {
                StringBuilder sb = new StringBuilder("(");
                for (int i = 0; i < args.length; i++) {
                    sb.append(args[i]);
                    if (i != args.length - 1) {
                        sb.append(",");
                    }
                }
                sb.append(")");
                argString = sb.toString();
            }
            Log.i("HookUtil", iActivityManagerObject.getClass().getSimpleName() + ":" + method.getName() + argString);
            // 我要在这里搞点事情
            if ("startActivity".contains(method.getName())) {
                Log.e("HookUtil", "Activity已经开始启动");
                Log.e("HookUtil", "小弟到此一游！！！");
            }

            return method.invoke(iActivityManagerObject, args);
        }
    }

}
