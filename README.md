# YAP LAN!

Kotlin + Jetpack Compose ile yazılmış, koyu temalı Android yapılacaklar uygulaması.
Görünüm "Tasarım 4 – Sade ve Anlaşılır"a göre: koyu zemin, tek vurgu rengi (sarı), IBM Plex yazı tipleri.

## Özellikler
- Alt menü: Görevler / Alışkanlıklar / Ara / Kategoriler
- Günlük ilerleme ("3/6 bugün bitti") ve cesaret mesajı; görev bitince "Yaptın lan!" kutlaması
- Görevler bölümlere ayrılır: Gecikenler, Bugün, Yarın, Daha sonra, Tarihsiz, Tamamlanan
- Alt sayfadan yeni görev: öncelik, kategori, son tarih (Bugün / Yarın / başka gün) ve saat
- **Not:** görevin yazısı ve işaretlenebilir maddeleri (ör. market listesi) tek kutuda. Madde yazıp Enter'a basınca
  sıradakine geçer; çok satırlı metin yapıştırılırsa her satır bir madde olur. Görev satırında "2/5" görünür.
- Kaydırarak ya da düzenleme ekranından silme, "Geri al" ile geri getirme
- Kullanıcı tanımlı renkli kategoriler, kategori başına görev sayısı
- Son tarihi olan her görev için o saatte bildirim gelir (ayrıca açıp kapatmaya gerek yok);
  telefon yeniden başlasa, uygulama güncellense ya da saat dilimi değişse de hatırlatmalar korunur.
  Listesi olan görevin bildiriminde kalan maddeler yazar.
- Bildirim düğmeleri: görevlerde **Tamamlandı** ve **10 dk ertele**, alışkanlıklarda **Bugün yaptım**.
  Cevapsız kalan görev bildirimi 30 dakika sonra bir kez daha hatırlatır.
- Ayrı bildirim kanalları (Görev, Düzenli, Alışkanlık, Zincir kurtarıcı, Zincir alarmı, Görev alarmı): her biri
  telefon ayarlarından ayrı ayrı kapatılabilir.
- **Düzenli hatırlat:** haftanın günleri (ör. Pt, Pe) ve bir saat seçilir; görev tamamlanana kadar
  o günlerde bildirim gelir. Düzenli hatırlatması olan görevin son tarihi olmaz.
- **Alarm:** hatırlatması olan görevde "Nasıl hatırlatayım?" bölümünden seçilir; kilit ekranında tam ekran çalar,
  Tamamlandı / 10 dk ertele / Kapat denene ya da 5 dakika geçene kadar susmaz. Cevapsız kalırsa 30 dakika sonra
  sessiz bir "Hâlâ bekliyor" bildirimi gelir.
- **Alışkanlıklar ("Zinciri kırma"):** her gün yapılan alışkanlıklar, aylık takvimde birleşik sarı zincir,
  güncel ve en uzun zincir, bu ayın skoru. Yalnızca bugün ve dün işaretlenebilir.
  İsteğe bağlı günlük hatırlatma: o saatte bugün yapılmadıysa bildirim gelir. Hatırlatma **alarm** olarak
  da seçilebilir: kilit ekranında tam ekran çalar, Yaptım / 10 dk ertele / Kapat denene ya da 5 dakika geçene kadar susmaz.
- **Zincir kurtarıcı:** her akşam 22:00'de, bugün yapılmazsa gece yarısı kırılacak zincirler için
  gece yarısına geri sayan tek bir bildirim.
- **Takvim widget'ı:** alışkanlık sayfasındaki "Widget olarak ekle" ile o alışkanlığın bu ayki takvimi
  ana ekrana eklenir (ya da ana ekran → Widget'lar → YAP LAN! → alışkanlığı seç). Dokununca alışkanlık açılır.
  Günün saatine ve zincirin durumuna göre mesajı ve kenar rengi değişir (16:00, 20:00, 22:00'de yenilenir).
- **Görev widget'ı:** tek bir görev ve listesi; görev ve maddeler ana ekrandan işaretlenir.
  Görev sayfasındaki "Widget olarak ekle" ya da ana ekran → Widget'lar → YAP LAN! → Görev.
- **Yapılacaklar widget'ı:** açık görevler (gecikenler kırmızı) ve bugün bitenler; ana ekrandan işaretlenir,
  "+" ile yeni görev eklenir.
- Uygulama ikonuna uzun basınca **Widget ekle** kısayolu; widget'ı olmayanlara listelerde kapatılabilir öneri kartı.

## Derleme ve çalıştırma
```fish
set -x JAVA_HOME /opt/android-studio/jbr
./gradlew assembleDebug testDebugUnitTest

~/Android/Sdk/emulator/emulator -avd Medium_Phone &
~/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Ya da klasörü Android Studio ile açıp ▶ Run'a basın.

## Yapı
- `data/` — Room veritabanı (görevler, kategoriler, alışkanlıklar), repository'ler ve zincir hesapları (`HabitStats.kt`)
- `reminder/` — alarm kurma, bildirim gösterme, açılışta alarmları yeniden kurma
- `ui/list`, `ui/search`, `ui/edit`, `ui/categories`, `ui/habits` — ekranlar ve ViewModel'ler
- `widget/` — Jetpack Glance ile alışkanlık takvimi, görev ve yapılacaklar widget'ları
- `app/schemas/` — Room şema dosyaları (veritabanı sürümü değişince migration için)
- `ui/components` — ortak parçalar (görev satırı, seçim kutuları, butonlar)
- `ui/theme` — renkler ve yazı tipleri

IBM Plex yazı tipleri SIL Open Font License ile dağıtılır (`app/src/main/assets/licenses/`).
