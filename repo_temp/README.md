# ClassMate - Student Academic Hub & Admin Cloud Portal

A comprehensive Android application built with Jetpack Compose & Material Design 3 for university and college students, paired with a web-based Admin Portal (`admin-web`) to remotely manage sponsor banners, promotions, and academic notifications via Supabase.

---

## 📱 Android Application Features

- **Class Notes & Topics**: Organized hierarchically by Semester > Course > Topic > Notes, with Markdown support and multi-photo attachments.
- **Assignment & Exam Deadlines**: Track pending tasks, mark completed assignments, view upcoming exam dates, and receive automated system notifications (`AlarmManager` & `NotificationCompat`).
- **Weekly Routine Schedule**: Day-by-day class routine with start/end time indicators and room details.
- **CGPA Calculator**: Semester GPA & Cumulative GPA prediction with custom grading scales.
- **Dynamic Multi-Sponsor Carousel**: Auto-rotating compact sponsor banner directly above the bottom navigation bar with a smooth 5-second slide animation. Automatically hides when no active sponsors are available.
- **Offline-First Architecture**: Powered by modern Room database, Kotlin Coroutines, and Flow.

---

## 🌐 Admin Web Portal (`admin-web`)

The `admin-web/` directory contains a standalone web administration dashboard designed for seamless deployment on **Vercel**, **Netlify**, or **GitHub Pages**.

### Key Web Features:
- **2-Step Verification Security**: Password authentication + Email OTP verification + Master Emergency Recovery Key.
- **Multi-Sponsor Queue**: Manage multiple sponsors (Slot 1, Slot 2, Slot 3) with real-time live phone preview.
- **5-Second Carousel Simulator**: Mirrors the mobile app's 5-second auto-sliding behavior with dot indicators right in the browser.
- **Supabase Cloud Sync**: 1-click publishing to Supabase so all student devices receive updates instantly.
- **Analytics Dashboard**: Tracks live views, click-through rates (CTR), and impressions.

### 🚀 Deploying `admin-web` on Vercel:
1. Import this repository into [Vercel](https://vercel.com).
2. Set **Root Directory** to `admin-web`.
3. Click **Deploy**!

---

## 🛠 Tech Stack

- **Android App**: Kotlin, Jetpack Compose, Material 3, AndroidX Room, Navigation Compose, Coil, Kotlin Serialization.
- **Web Admin**: Modern HTML5, Tailwind CSS, Lucide Icons, JavaScript (ES6+), Supabase JS Client.
- **Backend / Cloud**: Supabase PostgreSQL & REST API.

---

## 📄 License
MIT License
