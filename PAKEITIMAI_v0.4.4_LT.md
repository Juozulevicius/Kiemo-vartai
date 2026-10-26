# Vartų valdymas v0.4.4 – pasirašyti leidimai

Šis paketas paruoštas naudoti privačiame `Juozulevicius/Kiemo-vartai` kode ir atskiroje viešoje `Juozulevicius/VartuValdymas-Releases` leidimų saugykloje. Programėlės naujinimo tikrintuvas ieško naujausio leidimo viešoje saugykloje.

## Prieš paleidžiant leidimo workflow

1. Sukurk **viešą** GitHub saugyklą `Juozulevicius/VartuValdymas-Releases` (be programos šaltinio kodo).
2. Sukurk fine-grained Personal Access Token, apribotą tik `VartuValdymas-Releases` saugyklai, suteik `Contents: Read and write`. Į privačią `Kiemo-vartai` saugyklą įkelk jį kaip Actions secret `GH_RELEASE_TOKEN`. Tokeno į pokalbį nesiųsk.
3. Jau turi būti sukurti šie repository secrets `Kiemo-vartai` saugykloje: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
4. Įkelk šio ZIP projekto failus į `main` šaką ir pirmiausia paleisk įprastą `Build Android APK` workflow, kad įsitikintum, jog pakeitimai įkelti.
5. Sukurk tag `v0.4.4` ir pushink jį į `main` saugyklą. Tag push paleis `release-apk.yml`, kuris kompiliuos pasirašytą APK ir sukurs leidimą viešoje leidimų saugykloje.

## Svarbūs saugumo punktai

- Niekada neįkelk `.jks`, slaptažodžių ar Base64 rakto į GitHub kodo failus.
- Išsaugok `vartu-release.jks` ir slaptažodžius atskiroje saugioje vietoje. Visi būsimi leidimai turi būti pasirašyti tuo pačiu raktu.
- APK perleidimo workflow dar nepatvirtintas realiu vykdymu. Pirmą kartą patikrink `Actions` rezultatą ir `VartuValdymas-Releases` Releases puslapį.
- Atnaujinimo įdiegimas Android sistemoje vis tiek reikalauja naudotojo patvirtinimo. Telefone gali reikėti leisti programėlei diegti APK iš šio šaltinio.
- Esamos v0.4.3 programėlės atnaujinimas gali būti atmestas, nes ji galėjo būti pasirašyta kitu raktu. Kadangi pasirinkai, kad galima diegti iš naujo, prireikus pašalink seną programėlę tik po atsarginės duomenų kopijos; pašalinimas ištrina vietinius šlagbaumų duomenis.

## Versija

- `versionName`: `0.4.4`
- `versionCode`: `11`
- Naujausios versijos API: `https://api.github.com/repos/Juozulevicius/VartuValdymas-Releases/releases/latest`
