MiniHome - minimal startskarm for Google TV (Android TV)

BYGGA APK:n (gratis, i molnet)
1. Skapa ett konto pa github.com och ett nytt repository (privat gar bra).
2. Packa upp MiniHome.zip. Ladda upp ALLT innehall (inklusive den dolda mappen .github)
   via "Add file > Upload files" i repot och tryck Commit.
3. Ga till fliken Actions > "Build APK". Vanta ca 3-5 min tills den ar gron.
4. Klicka pa korningen > Artifacts > minihome-apk. Packa upp zip-filen.
   Du far app-debug.apk. Lagg den i platform-tools-mappen tillsammans med install_launcher.bat.

EGNA BILDER (valfritt)
Lagg jpg/png i en mapp och kor:
  adb shell mkdir -p /sdcard/Android/data/app.minihome.tv/files/walls
  adb push mina_bilder/. /sdcard/Android/data/app.minihome.tv/files/walls/
Ligger bilder dar roterar appen dem. Annars ritar den sex inbyggda naturmotiv.

DOCKAN
Standard: Netflix, TizenTube, SportzX, Downloader, TV Bro + "Alla appar".
Hall in OK pa en app i "Alla appar" for att lagga till/ta bort den i dockan.
