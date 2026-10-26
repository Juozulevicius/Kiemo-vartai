# Vartų valdymas – Android prototipas v0.3.0

## Kas įdiegta šiame prototipe
- Kelių šlagbaumų sąrašas.
- Šlagbaumo pavadinimas ir telefono numeris.
- Telefono kontaktų parinkiklis.
- Rankinis skambinimas per Android skambučių funkciją (leidus `CALL_PHONE`); jei leidimas nesuteiktas, atidaromas numerio rinkiklis.
- Vietinės konfigūracijos saugojimas telefone.
- Spalvos: geltona `#FDB913`, žalia `#006A44`, raudona `#C1272D`, grafito `#363636`.
- Automatikos nustatymų demonstracija, bet automatinio skambinimo variklis sąmoningai neįdiegtas.

## Kas dar neįdiegta
- Google Maps SDK žemėlapio pasirinkimo ekranas (šiuo metu įvedamos koordinatės).
- GPS / judėjimo stebėjimas fone.
- Automatinis skambinimas.
- APK automatinis atnaujinimas.
- Šifruota saugykla.

Tai pradinis kūrimo projektas, ne galutinė saugiai ištestuota programėlė.

## Kaip atidaryti Windows kompiuteryje
1. Įsidiek Android Studio ir Android SDK (SDK Platform 35).
2. Išarchyvuok ZIP į aplanką.
3. Android Studio -> Open -> pasirink aplanką `VartuValdymasAndroid`.
4. Palauk Gradle Sync. Jei paprašys, įsidiek JDK 17 (Android Studio Embedded JDK tinka).
5. Prijunk telefoną su USB debugging arba naudok emuliatorių.
6. Run ▶️. APK kūrimui: Build -> Build APK(s).
7. APK paprastai atsiras `app/build/outputs/apk/debug/app-debug.apk`.

## Pirmasis bandymas
- Įvesk bandomąjį numerį, kurio skambutis nekenks.
- Patikrink, kad skambučio mygtukas atidaro skambutį tinkamam numeriui.
- Tik po patikros naudok tikrą šlagbaumo numerį.
- Automatinis režimas šiame prototipe realių skambučių neatlieka.

## Svarbu dėl skambinimo
`ACTION_CALL` su `CALL_PHONE` leidimu gali pradėti skambutį tiesiogiai, priklausomai nuo Android versijos ir operatoriaus. Be leidimo naudojamas `ACTION_DIAL`, kur skambutį dar reikia patvirtinti.


## Saugumo prisijungimas v0.3.0

- Pirmą kartą paleidus programėlę reikia susikurti ir pakartoti 4 skaitmenų PIN kodą.
- PIN saugomas ne atviru tekstu: naudojama atsitiktinė druska (salt) ir SHA-256 maiša.
- Jei telefone nustatyta biometrinė apsauga, programa paleidimo metu pasiūlo patvirtinti tapatybę sistemos biometriniu langu.
- Jei piršto antspaudas neatpažįstamas arba vartotojas pasirenka PIN, galima prisijungti PIN kodu.
- Programėlė negauna ir nesaugo paties piršto antspaudo; biometrinį patvirtinimą tvarko Android sistema.
- Jei telefone nėra nustatyta biometrinė apsauga, prisijungimui naudojamas PIN.

## Šio leidimo ribos

Šiame leidime nėra pilnai įdiegtas Google Maps ekranas, GPS/judėjimo automatinio skambinimo variklis, skambučių istorija, duomenų eksportas/atkūrimas ar krypties atpažinimas. Automatinis skambinimas sąmoningai paliktas neaktyvus. Projektas prieš naudojimą turi būti sukompiliuotas ir išbandytas Android Studio aplinkoje.


## v0.3.0 papildymai

- Pridėtas JSON atsarginės kopijos eksportas per Android failų pasirinkimo langą.
- Pridėtas JSON atsarginės kopijos atkūrimas; importas tikrina failo formatą.
- Prie kiekvieno šlagbaumo pridėtas mygtukas „Žemėlapis“, kuris atidaro įrenginio žemėlapių programą. Jei įrašytos koordinatės, atidaroma konkreti vieta; kitu atveju ieškoma pagal pavadinimą.
- Išlaikytas PIN ir biometrinis prisijungimas bei rankinis skambinimas.

## Kas dar neįgyvendinta v0.3.0

- Integruotas Google Maps SDK ekranas ir vietos pasirinkimas žemėlapyje (reikalingas Google Maps API raktas).
- Tikras GPS geozonų / judėjimo automatinio skambinimo variklis ir foninis stebėjimas. Automatika palikta išjungta; UI nustatymai dar nesukelia automatinio skambučio.
- Tiesioginė Google Drive API sinchronizacija. Atsarginę kopiją galima eksportuoti Android failų lange ir pasirinkti Google Drive, jei jis įdiegtas / pasiekiamas kaip failų teikėjas.
- Skambučių istorija.

Šis projektas šiame vykdymo etape nebuvo sukompiliuotas ar išbandytas fiziniame telefone. Būtina patikrinti Android Studio, o prieš bet kokį realų automatinį skambinimą atlikti saugos bandymus.
