# Vartų valdymas v0.3.7 – dizaino, ikonėlės ir žemėlapio atnaujinimas

## Įtraukta į šį paketą
- Integruota pasirinkto stiliaus programėlės paleidimo ikonėlė (žalia, raudona, geltona ir kreminė spalvos).
- Pagrindinės antraštės logotipas.
- Vienodo aukščio, vienodo pločio skambinimo, žemėlapio ir redagavimo mygtukai; tekstas nekeliamas į antrą eilutę.
- Įdėtas Google Maps SDK žemėlapio peržiūros komponentas kiekvienai vietai, kuriai nustatytos koordinatės, su šlagbaumo žymekliu.
- Mygtukas atidaryti konkrečią vietą Google Maps programėlėje / svetainėje.
- Programėlės versijos numeris atnaujintas į 0.3.7.

## Būtinas Google Maps API raktas
Google Maps SDK reikalauja Google Cloud projekto API rakto ir įjungto Maps SDK for Android. Faile `app/src/main/res/values/strings.xml` pakeiskite `PASTE_YOUR_GOOGLE_MAPS_API_KEY_HERE` į savo raktą. Be galiojančio rakto APK gali susikompiliuoti, tačiau įterptas žemėlapis nerodys tikro žemėlapio. Apribokite raktą pagal Android programėlės package name `lt.vartuvaldymas.app` ir savo pasirašymo SHA-1.

## Patikrinimo būsena
- ZIP struktūra ir failų pakeitimai paruošti.
- Šioje aplinkoje Android/Gradle kompiliacija ir fizinio telefono testas neatlikti.
- Todėl negalima teigti, kad APK jau patikrintas; prieš diegiant reikia paleisti GitHub Actions build ir peržiūrėti rezultatą.
- Automatinis skambinimas šiame atnaujinime neįjungiamas ir nėra laikomas patikrinta funkcija.
