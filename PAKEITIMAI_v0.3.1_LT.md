# v0.3.1 – GitHub kompiliavimo paruošimas

- Pridėtas `.github/workflows/build-apk.yml` automatinio APK kūrimo procesas.
- Pridėtas `gradle/wrapper/gradle-wrapper.properties`.
- GitHub Actions darbo eiga nustato Java 17, paruošia Gradle ir sugeneruoja Gradle Wrapper, jei jo nėra.
- APK sėkmės atveju įkeliamas į Actions Artifacts.

Svarbu: Android projektas šiame etape nebuvo realiai sukompiliuotas. Pirmo build rezultatas patikrins, ar yra Kotlin / Gradle / Android konfigūracijos klaidų.
