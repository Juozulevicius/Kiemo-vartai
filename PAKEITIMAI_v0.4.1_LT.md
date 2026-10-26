# Pakeitimai v0.4.1

- Pridėtas „GitHub Releases“ naujausios versijos tikrinimas programėlėje.
- Jei paskelbtas naujesnis leidimas ir prie jo pridėtas APK, programėlė leidžia jį atsisiųsti ir atidaro Android diegimo patvirtinimo langą.
- Pridėtas FileProvider, kad APK būtų saugiai perduodamas Android diegimo programai.
- Versija pakelta į 0.4.1 (versionCode 8).
- Išliko OpenStreetMap žemėlapiai ir ankstesnės programėlės funkcijos.

## Svarbu

Atnaujinimų tikrinimas veiks tik tada, kai GitHub saugykloje `Juozulevicius/Vartu-Apps` bus paskelbtas GitHub Release su APK failu. APK turi būti pasirašytas tuo pačiu sertifikatu kaip jau įdiegta programėlė. Ankstesni GitHub Actions `debug` APK galėjo būti pasirašyti laikinu GitHub runnerio debug raktu; jei parašai nesutampa, Android atnaujinimo nepriims. Tokiu atveju pirmą kartą gali tekti pašalinti seną programą ir įdiegti naują (prieš tai eksportuoti atsarginę vartų kopiją).

## Release pasirašymo nustatymas

GitHub saugykloje pridėk Actions secrets:
- `ANDROID_KEYSTORE_BASE64` – pasirašymo `.jks` failo Base64 turinys.
- `ANDROID_KEYSTORE_PASSWORD` – keystore slaptažodis.
- `ANDROID_KEY_ALIAS` – rakto alias.
- `ANDROID_KEY_PASSWORD` – rakto slaptažodis.

Tik po to sukurk ir nusiųsk tagą `v0.4.1`. Release workflow sugeneruos pasirašytą APK ir pridės jį prie GitHub Release. **Niekada nekeisk ir neprarask keystore failo**, kitaip vėlesni APK negalės atnaujinti šiuo raktu pasirašytos programos.

### Kaip paruošti pasirašymo raktą (vieną kartą)

Kompiuteryje, kuriame įdiegtas JDK, vykdyk:

```bash
keytool -genkeypair -v -keystore vartu-release.jks -alias vartu-release -keyalg RSA -keysize 2048 -validity 10000
base64 -w 0 vartu-release.jks > vartu-release-base64.txt
```

Windows PowerShell aplinkoje Base64 failą galima paruošti taip:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("vartu-release.jks")) | Set-Content -NoNewline vartu-release-base64.txt
```

Į GitHub saugyklos **Settings → Secrets and variables → Actions** įrašyk keturis aukščiau nurodytus secrets. `ANDROID_KEYSTORE_BASE64` reikšmei nukopijuok visą `vartu-release-base64.txt` turinį. Slaptažodžius ir alias naudok tuos, kuriuos pasirinkai kurdamas raktą. Keystore ir jo slaptažodžių niekur viešai nekelk; pasidaryk saugią atsarginę kopiją.

Kai secrets nustatyti, GitHub saugykloje sukurk tagą `v0.4.1` ir nusiųsk jį į `main` saugyklą. Workflow sukurs GitHub Release su APK, kurį atnaujinimų tikrintuvas galės rasti. Prieš išleidžiant įprastai naudotojams, būtina sėkmingai paleisti workflow ir patikrinti diegimą tikrame telefone.
