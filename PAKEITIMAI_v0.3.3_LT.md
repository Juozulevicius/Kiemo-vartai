# v0.3.3 – Kotlin DSL nustatymo pataisymas

- Perkeltas `kotlinOptions { jvmTarget = "17" }` į `android { ... }` bloką faile `app/build.gradle.kts`.
- Tai taiso klaidą „Unresolved reference: kotlinOptions“, matytą GitHub Actions žurnale.
- Projektas dar nebuvo paleistas naujame GitHub Actions build'e; po įkėlimo reikia patikrinti naują rezultatą.
