# Pakeitimai v0.3.6

- Biometrinio prisijungimo veiklai `BiometricPrompt` reikalinga `FragmentActivity`, o ne paprasta `ComponentActivity`.
- `MainActivity` ir `AuthenticationGate` dabar naudoja `FragmentActivity`, kad `BiometricPrompt` konstruktorius ir `authenticate(PromptInfo)` būtų pasiekiami pagal AndroidX Biometric API.
- Padidintas Android programėlės versijos kodas iki 5, versija 0.3.6.

Ši versija dar turi būti patikrinta GitHub Actions aplinkoje. Jei kompiliavimas sustos, reikia atsiųsti naujo paleidimo klaidos žurnalą.
