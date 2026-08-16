# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the AGP-bundled proguard-android-optimize.txt.

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# WorkManager instantiates Worker subclasses reflectively via this constructor -
# without this rule R8 can strip or rename it, breaking CleanupWorker/SyncWorker.
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}
