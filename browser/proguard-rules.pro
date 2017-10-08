#指定代码的压缩级别
-optimizationpasses 10
#包明不混合大小写
-dontusemixedcaseclassnames
#不去忽略非公共的库类
-dontskipnonpubliclibraryclasses
#忽略警告
-ignorewarning
#屏蔽警告，脚本中把这行注释去掉
-dontshrink
 #优化  不优化输入的类文件
-dontoptimize
 #预校验
-dontpreverify
 #混淆时是否记录日志
-verbose
 # 混淆时所采用的算法
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

#保持系统的四大组建以及一些类不被混淆
-keep public class * extends android.app.Fragment
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class com.android.vending.licensing.ILicensingService


# 保持自定义控件类不要被混淆和jni方法不被混淆;
-keepclasseswithmembernames class * {
    native <methods>;
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

#保持自定义控件类不被混淆
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

#UMENG 相关混淆
-keepclassmembers class * {
   public <init> (org.json.JSONObject);
}

#UMENG 相关混淆
-keep public class [com.blink.browser].R$*{
public static final int *;
}

#保持 Parcelable 不被混淆
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

#保持 Serializable 不被混淆
-keepnames class * implements java.io.Serializable

#保持枚举 enum 类不被混淆
-keepclassmembers enum * {
  public static **[] values();
  public static ** valueOf(java.lang.String);
}

#本地的R类不要被混淆,不然就找不到相应的资源
-keep class **.R$*{ *; }

#保持org.json类不要混淆
-keep class org.json.** { *; }

#保持所有继承该类的子类的名字不要被混淆
-keep class com.blink.browser.bean.** { *; }

# 保持webView不被混淆
-dontwarn android.webkit.WebView
#保持内部类，异常类
-keepattributes Exceptions, InnerClasses
#保持泛型、注解、源代码之类的不被混淆
-keepattributes Signature, Deprecated, SourceFile
-keepattributes LineNumberTable, *Annotation*, EnclosingMethod
#保持v4包下面的类不要被混淆
-keep class android.support.v4.app.** { *; }
-keep interface android.support.v4.app.** { *; }
#如果引用了v4或者v7包
-dontwarn android.support.**
-keepattributes *JavascriptInterface*
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.blink.browser.video.JsInterfaceInject {*;}

-keep public class * extends com.blink.browser.bean.CommonEntity {*;}
-keep public @interface *  {*;}

#####################记录生成的日志数据,gradle build时在本项目根目录输出################
#apk 包内所有 class 的内部结构
-dump class_files.txt
#未混淆的类和成员
-printseeds seeds.txt
#列出从 apk 中删除的代码
-printusage unused.txt
#混淆前后的映射
-printmapping mapping.txt


#保持 Umeng类 不被混淆
-keep class com.umeng.analytics.** { *; }

#手动启用support keep注解
#http://tools.android.com/tech-docs/support-annotations
-keep,allowobfuscation @interface android.support.annotation.Keep

-keep @android.support.annotation.Keep class *

-keepclassmembers class * {

    @android.support.annotation.Keep *;
}

#openFileChooser is a hidden method when android version <= 4.4
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public void openFileChooser(...);
}
