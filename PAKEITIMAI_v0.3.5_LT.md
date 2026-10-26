# v0.3.5 – Kotlin klaidų pataisymai

Pagal GitHub Actions žurnalą:
- `ComponentActivity.MODE_PRIVATE` pakeista į `Context.MODE_PRIVATE`.
- `save()` funkcija perkelta prieš atsarginės kopijos importo callback registraciją, kad būtų prieinama toje vietoje.
- `Modifier.weight(1)` pakeista į `Modifier.weight(1f)`.
- Biometrinio prisijungimo iškvietimas lieka per AndroidX `BiometricPrompt.authenticate(PromptInfo)` API.

Šis paketas dar nebuvo realiai sukompiliuotas. Reikia atnaujinti GitHub failus ir paleisti Actions dar kartą.
