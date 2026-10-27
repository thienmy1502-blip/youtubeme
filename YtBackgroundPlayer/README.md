# YT Music Nền (Android)

Ứng dụng Android (Kotlin + Jetpack Compose) gồm 2 tab:

1. **Xem YouTube** – dán link/ID video, xem bằng IFrame Player chính thức
   (`android-youtube-player`). Player này tự dừng khi tắt màn hình.
2. **Nghe nền** – hàng đợi phát bằng Media3 ExoPlayer + `MediaSessionService`.
   Phát tiếp khi tắt màn hình, có thông báo và điều khiển trên màn hình khóa.
   Nhận mọi URL âm thanh trực tiếp (mp3, m4a, m3u8...).

## Cách chạy
1. Mở thư mục này bằng **Android Studio** (Koala 2024.1 trở lên, JDK 17).
2. Chờ Gradle sync xong (Studio sẽ tự tải Gradle 8.9).
3. Cắm điện thoại (bật USB debugging) hoặc dùng emulator → **Run ▶**.
4. Android 13+: cho phép thông báo khi app hỏi.

## Lưu ý
- Máy Xiaomi/Oppo/Samsung: tắt "tối ưu pin" cho app, nếu không hệ thống có thể giết service khi tắt màn hình.
- Nếu video YouTube báo lỗi khi phát, thử nâng phiên bản `androidyoutubeplayer:core` trong `app/build.gradle.kts`.
- Tab "Nghe nền" cố ý không tự lấy luồng âm thanh từ YouTube vì điều khoản YouTube không cho phép phát nền.
  Hãy dùng nguồn bạn có quyền (nhạc tự host, Jamendo, Internet Archive, radio stream...).

## Cấu trúc
- `PlaybackService.kt` – ExoPlayer + MediaSession (foreground service)
- `MainActivity.kt` – kết nối MediaController, quản lý hàng đợi
- `Ui.kt` – giao diện Compose (2 tab)

## Build APK bằng GitHub (không cần Android Studio)
1. Tạo repo mới trên github.com, upload toàn bộ nội dung thư mục này (nhớ cả thư mục `.github`).
2. Vào tab **Actions** > **Build APK** > **Run workflow** (hoặc tự chạy sau khi upload).
3. Khi chạy xong (khoảng 3-6 phút), mở lần chạy đó, kéo xuống mục **Artifacts**, tải `app-debug-apk`, giải nén ra `app-debug.apk`.
