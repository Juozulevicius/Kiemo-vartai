# Vartų valdymas v0.4.8 – bendras atnaujinimų paketas

Šis paketas sujungia dabartinį projekto variantą ir ankstesniuose etapuose aptartus pakeitimus.

- OpenStreetMap žemėlapiai.
- Vietos leidimo prašymas ir dabartinės vietos nustatymo bandymas žemėlapio pasirinkimo lange.
- Rankinis tiesioginis skambinimas su CALL_PHONE leidimo užklausa; jei nepavyksta, bandoma atidaryti rinkiklį.
- Pagrindinio ekrano slinkimas naudojant verticalScroll.
- Atnaujinimų tikrinimas viešoje `Juozulevicius/VartuValdymas-Releases` saugykloje.
- `release-apk.yml` workflow pasirašyto APK kūrimui ir paskelbimui prie viešo leidimo.
- Versija 0.4.8, versionCode 15.

## Prieš naudojimą

Šis ZIP nėra patvirtintas galutiniu Android kompiliavimu šiame paruošimo etape. Įkelkite failus į `Kiemo-vartai`, patikrinkite GitHub Actions kompiliavimą, o tik tada sukurkite `v0.4.8` žymą. Release workflow reikalauja repo secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`, `GH_RELEASE_TOKEN`. Vieša `VartuValdymas-Releases` saugykla turi būti sukurta, o tokenas turi turėti teisę rašyti jos Releases.

Automatinis APK įdiegimas vis tiek reikalauja Android sistemos vartotojo patvirtinimo. GPS ir skambinimas turi būti išbandyti realiame telefone.
