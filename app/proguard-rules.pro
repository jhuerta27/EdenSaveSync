# Shizuku UserService & AIDL contract instantiated via reflection in the Shizuku process
-keep class com.jotasoft.edensavesync.data.storage.shizuku.PeanutsShizukuService { *; }
-keep interface com.jotasoft.edensavesync.data.storage.shizuku.IPeanutsFileService { *; }
-keep class com.jotasoft.edensavesync.data.storage.shizuku.IPeanutsFileService$** { *; }
-keep class rikka.shizuku.** { *; }
