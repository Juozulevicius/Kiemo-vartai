# v0.3.4 – JVM target ir GitHub Actions pataisymas

- Pašalintas `kotlinOptions` blokas; vietoje jo naudojamas Kotlin Gradle Plugin 2.0 `compilerOptions` DSL su `JvmTarget.JVM_17`.
- Java source/target compatibility nustatyti į 17.
- GitHub Actions naudoja paruoštą Gradle 8.9 tiesiogiai, todėl jam nereikia `gradlew` ir wrapper JAR failų.
- APK artifact įkeliamas iš `app/build/outputs/apk/debug/*.apk`.

Svarbu: šiame vykdymo etape APK nebuvo realiai kompiliuotas. Po failų atnaujinimo GitHub reikia paleisti Actions ir peržiūrėti naują logą.
