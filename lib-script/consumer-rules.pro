-keepclassmembers class * {
    @com.github.jing332.script.annotation.ScriptInterface <methods>;
}

-keep class com.github.jing332.script.runtime.**{ *;}
-keep class com.github.jing332.script.simple.**{ *;}

-keep class org.mozilla.javascript.**  { *; }