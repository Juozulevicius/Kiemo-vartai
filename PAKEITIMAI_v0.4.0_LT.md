# Vartų valdymas v0.4.0 – OpenStreetMap

## Pakeitimai
- Pašalinta priklausomybė nuo Google Maps SDK / Google Maps API rakto.
- Pridėta OpenStreetMap per osmdroid (MAPNIK plytelės).
- Šlagbaumo kortelėje žemėlapio peržiūra rodo koordinates ir žymeklį.
- Pridėtas vietos pasirinkimo dialogas: bakstelėjus žemėlapį, koordinatės užpildomos automatiškai; numatyta pradinė vieta Vilniuje, jei koordinatės dar neįvestos.
- Išorinis žemėlapio atidarymas nukreipiamas į OpenStreetMap.
- Versija pakeista į 0.4.0, versionCode 7.
- Išlaikytos esamos spalvos, PIN / biometrinio prisijungimo kodas, skambinimo srautas ir atsarginės kopijos failui funkcija.

## Svarbios ribos
- OpenStreetMap plytelėms reikia interneto. Viešasis OSM plytelių serveris nėra neribota nemokama paslauga; reikia laikytis tile usage policy ir nurodyti OSM atribuciją. Šis pakeitimas skirtas bandymui, prieš platesnį platinimą reikėtų apsvarstyti komercinį OSM plytelių tiekėją arba talpyklos politiką.
- Automatinio skambinimo variklis ir debesijos atsarginės kopijos nėra įgyvendinti.
- Šis ZIP nebuvo sukompiliuotas Android build aplinkoje; būtina paleisti GitHub Actions. Jei kompiliavimas nepavyks, taisyti pagal pilną žurnalą.
