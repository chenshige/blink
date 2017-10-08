package com.blink.browser.util;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.blink.browser.util.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class TelephonyManagerUtil {
    private final static String TAG = "TelephonyManagerUtil";

    public static String getDefaultImei(Context context) {
        TelephonyManager tm = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);

        return tm.getDeviceId();
    }

    public static String getDefaultNetworkOperator(Context context) {
        TelephonyManager tm = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);

        return tm.getSimOperator();
    }

    public static String getDefaultImsi(Context context) {
        TelephonyManager tm = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);

        return tm.getSubscriberId();
    }

    public static String getImeiBySlot(Context context, int slotIndex) {
        Object ret = null;
        try {
            ret = getSimOperatorInfoBySlot(context, "getDeviceIdGemini", slotIndex);

        } catch (Exception e) {
            Logger.debug(TAG, "no method getDeviceIdGemini");
            try {
                ret = getSimOperatorInfoBySlot(context, "getDeviceId", slotIndex);
            } catch (Exception e1) {
                Logger.debug(TAG, "no method getDeviceId");
            }
        }

        if (ret != null)
            return ret.toString();
        return null;
    }

    public static String getNetworkOperatorBySlot(Context context, int slotIndex) {
        Object ret = null;
        try {
            ret = getSimOperatorInfoBySlot(context, "getSimOperatorGemini", slotIndex);

        } catch (Exception e) {
            Logger.debug(TAG, "no method getSimOperatorGemini");
            try {
                ret = getSimOperatorInfoBySlot(context, "getSimOperator", slotIndex);
            } catch (Exception e1) {
                Logger.debug(TAG, "no method getSimOperator");
            }
        }

        if (ret != null)
            return ret.toString();
        return null;
    }

    public static String getImsiBySlot(Context context, int slotIndex) {
        Object ret = null;
        try {
            ret = getSimOperatorInfoBySlot(context, "getSubscriberIdGemini", slotIndex);

        } catch (Exception e) {
            Logger.debug(TAG, "no method getSubscriberIdGemini");
            try {
                ret = getSimOperatorInfoBySlot(context, "getSubscriberId", slotIndex);
            } catch (Exception e1) {
                Logger.debug(TAG, "no method getSubscriberId");
            }
        }

        if (ret != null)
            return ret.toString();
        return null;
    }


    static Object getSimOperatorInfoBySlot(Context context,
                                           String methodName, int slotIndex) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        TelephonyManager tm = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);

        Class clz = tm.getClass();
        Class<?>[] paramClasses = new Class[]{int.class};
        Method method = clz.getMethod(methodName, paramClasses);
        Object ret = method.invoke(tm, slotIndex);
        return ret;
    }

    public static String getMccInfo(Context context) {
        String result = getNetworkOperatorBySlot(context, 1);
        String mcc2 = getNetworkOperatorBySlot(context, 2);
        if(!TextUtils.isEmpty(result)){
            result = result.substring(0,3);
        }
        if(!TextUtils.isEmpty(mcc2)){
            mcc2 = mcc2.substring(0,3);
        }
        if (result == null) {
            result = mcc2;
        } else {
            if (!TextUtils.isEmpty(mcc2))
                result = result + "," + mcc2;
        }
        return result;
    }

    public static boolean isDoubleSimsSupported(Context context) {
        try {

            getSimOperatorInfoBySlot(context, "getSimStateGemini", 1);
            getSimOperatorInfoBySlot(context, "getSimStateGemini", 2);
            return true;

        } catch (Exception e) {
            Logger.debug(TAG, "no method getSimStateGemini");
            try {
                getSimOperatorInfoBySlot(context, "getSimState", 1);
                getSimOperatorInfoBySlot(context, "getSimState", 2);
                return true;
            } catch (Exception e1) {
                Logger.debug(TAG, "no method getSimState");
            }
        }

        return false;
    }
}
