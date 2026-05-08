# Keep SQLDelight generated classes
-keep class com.evertask.database.** { *; }

# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keepclassmembers class com.evertask.shared.domain.model.** { *; }
-keepclassmembers class com.evertask.shared.data.repository.** { *; }

# Keep Koin
-keep class org.koin.** { *; }
-keep class com.evertask.app.di.** { *; }
-keep class com.evertask.shared.di.** { *; }

# Keep Glance widget classes
-keep class com.evertask.app.widget.** { *; }

# Keep ViewModel and Compose
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends androidx.lifecycle.ViewModel

# Keep BroadcastReceivers and deep-link handlers (reflection via Koin in background)
-keep class com.evertask.app.receiver.** { *; }
-keep class com.evertask.app.deeplink.** { *; }

# Keep WorkManager workers
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
