# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\android\android-sdk-windows/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-optimizationpasses 5          # 指定代码的压缩级别
-dontusemixedcaseclassnames   # 是否使用大小写混合
-dontpreverify           # 混淆时是否做预校验
-verbose                # 混淆时是否记录日志
-dontskipnonpubliclibraryclasses
-keepattributes SourceFile,LineNumberTable

-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*    # 混淆时所采用的算法

#-keep interface * extends android.os.IInterface  # 保持哪些类不被混淆
-keep class com.zhi_tech.magiceyessdk.MagicEyesActivity{*;}
-keep class com.zhi_tech.magiceyessdk.Utils.**{*;}

-keepnames class * implements java.io.Serializable  #保持 Serializable 不被混淆
-keepclasseswithmembernames class * {  # 保持 native 方法不被混淆
    native <methods>;
}
-keep class * implements android.os.Parcelable { # 保持 Parcelable 不被混淆
    public static final android.os.Parcelable$Creator *;
}