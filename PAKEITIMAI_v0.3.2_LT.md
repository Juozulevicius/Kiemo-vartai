# v0.3.2 – JVM target pataisymas

- `app/build.gradle.kts` Java `sourceCompatibility` ir `targetCompatibility` nustatyti į Java 17.
- Kotlin `jvmTarget` nustatytas į 17.
- Tai skirta pataisyti GitHub Actions klaidą, kur Java kompiliatorius naudojo JVM 1.8, o Kotlin – JVM 17.

Projektas po šio pakeitimo dar nėra perkompiliuotas. Įkelk pakeistus failus į GitHub ir pakartok Actions paleidimą. Jei atsiras kita klaida, ją reikės diagnozuoti pagal naują žurnalą.
