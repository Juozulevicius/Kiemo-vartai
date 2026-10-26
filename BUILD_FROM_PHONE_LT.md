# Kaip sukurti APK neturint kompiuterio

Šiame ZIP yra GitHub Actions darbo eiga, kuri gali sukompiliuoti bandomąjį APK debesyje. Reikės GitHub paskyros ir interneto naršyklės telefone.

1. Atsisiųsk ZIP ir išarchyvuok jį telefone (pvz., Android failų tvarkykle).
2. Naršyklėje atidaryk https://github.com/new ir sukurk naują privatų repozitoriumą, pvz. `vartu-valdymas-android`.
3. Repozitoriume pasirink **Add file → Upload files** ir įkelk visus išarchyvuoto aplanko failus bei aplankus. Įsitikink, kad `.github/workflows/build-apk.yml` taip pat įkeltas. Jei mobilioji svetainė neleidžia patogiai įkelti aplankų, gali tekti naudoti GitHub mobiliąją programą arba kompiuterį.
4. Patvirtink įkėlimą į `main` šaką.
5. Atidaryk repozitoriumo **Actions** skiltį, pasirink **Build Android APK**, spausk **Run workflow**.
6. Kai veiksmas baigsis žalia varnele, atidaryk jo vykdymą, skiltį **Artifacts**, atsisiųsk `VartuValdymas-debug-apk` ZIP.
7. Išarchyvuok tą ZIP telefone. Jame bus `app-debug.apk`, kurį galėsi įdiegti.

Pastabos:
- Tai bandomasis debug APK, ne viešam platinimui skirtas leidimas.
- Android gali paprašyti leisti diegti programas iš naršyklės ar failų tvarkyklės.
- Ši versija turi rankinio skambinimo prototipą. Automatinio skambinimo mechanizmas ir tikras Google Maps pasirinkimo ekranas dar neįdiegti; automatikos jungiklis realaus automatinio skambinimo neįjungia.
- APK nebuvo sukompiliuotas ir išbandytas šiame pokalbio vykdymo aplinkos kompiuteryje. Debesies darbo eiga jį sukompiliuos ir parodys klaidas, jei jų bus.

## GitHub Actions APK kūrimas

Projekte pridėta `.github/workflows/build-apk.yml`. Įkėlus šio projekto turinį į GitHub saugyklos `main` šaką, darbo eiga turėtų prasidėti automatiškai; ją taip pat galima paleisti per **Actions → Build Android APK → Run workflow**. Sėkmingo darbo rezultatas bus **Artifacts → VartuValdymas-debug-apk**.

Pastaba: darbo eiga sugeneruoja Gradle Wrapper, jei `gradlew` nėra. Tai nepatvirtina, kad Android programos Kotlin kodas susikompiliuos; pirmasis paleidimas gali parodyti kodo ar priklausomybių klaidų, kurias reikės taisyti.

### v0.3.4 pastaba
Nauja darbo eiga naudoja GitHub Actions aplinkoje įdiegtą Gradle 8.9 tiesiogiai. `gradlew` ir `gradle-wrapper.jar` šiai darbo eigai nebūtini. Įsitikink, kad GitHub saugykloje atnaujinti tiek `app/build.gradle.kts`, tiek `.github/workflows/build-apk.yml`.
