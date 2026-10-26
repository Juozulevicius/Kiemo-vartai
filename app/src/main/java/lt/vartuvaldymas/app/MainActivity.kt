package lt.vartuvaldymas.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.OpenableColumns
import android.widget.Toast
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

private val Yellow = Color(0xFFFDB913)
private val Green = Color(0xFF006A44)
private val Red = Color(0xFFC1272D)
private val Charcoal = Color(0xFF363636)
private val Cream = Color(0xFFF6F4EE)

data class Gate(val id: Long, val name: String, val phone: String, val lat: Double?, val lon: Double?, val radius: Int, val autoEnabled: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AuthenticationGate(activity = this) }
    }
}

@Composable
private fun AuthenticationGate(activity: ComponentActivity) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gate_security", android.content.Context.MODE_PRIVATE) }
    var unlocked by remember { mutableStateOf(false) }
    var setupMode by remember { mutableStateOf(prefs.getString("pin_hash", null) == null) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var useBiometric by remember { mutableStateOf(prefs.getBoolean("biometric_enabled", false)) }
    val canBiometric = remember {
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticateBiometric() {
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                unlocked = true
                error = ""
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                error = "Biometrija nepavyko. Įvesk 4 skaitmenų PIN kodą."
            }
            override fun onAuthenticationFailed() {
                error = "Piršto antspaudas neatpažintas. Bandyk dar kartą arba naudok PIN."
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Vartų valdymas")
            .setSubtitle("Patvirtink tapatybę piršto antspaudu")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .setNegativeButtonText("Naudoti PIN kodą")
            .build()
        prompt.authenticate(info)
    }

    LaunchedEffect(setupMode, useBiometric) {
        if (!setupMode && useBiometric && canBiometric && !unlocked) authenticateBiometric()
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = Green, secondary = Yellow, error = Red, background = Cream, surface = Color.White)) {
        if (unlocked) {
            GateApp()
        } else {
            Scaffold(containerColor = Cream) { padding ->
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("VARTŲ VALDYMAS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Green)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (setupMode) "Pirmą kartą nustatyk 4 skaitmenų PIN kodą." else "Patvirtink tapatybę, kad galėtum valdyti šlagbaumus.",
                        color = Charcoal
                    )
                    Spacer(Modifier.height(18.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pin = it; error = "" },
                        label = { Text("4 skaitmenų PIN") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword)
                    )
                    if (setupMode) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = confirmPin,
                            onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) confirmPin = it; error = "" },
                            label = { Text("Pakartok PIN kodą") },
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            if (canBiometric) "Po PIN nustatymo galėsi įjungti telefono biometrinį patvirtinimą." else "Telefone biometrinis patvirtinimas neprieinamas arba dar nenustatytas. PIN kodas veiks kaip pagrindinis prisijungimas.",
                            color = Charcoal.copy(alpha = .75f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (error.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(error, color = Red)
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (setupMode) {
                                when {
                                    pin.length != 4 -> error = "PIN turi būti tiksliai 4 skaitmenų."
                                    pin != confirmPin -> error = "PIN kodai nesutampa."
                                    pin == "0000" || pin == "1234" -> error = "Pasirink saugesnį PIN kodą."
                                    else -> {
                                        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
                                        prefs.edit()
                                            .putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                                            .putString("pin_hash", hashPin(pin, salt))
                                            .putBoolean("biometric_enabled", canBiometric)
                                            .apply()
                                        setupMode = false
                                        useBiometric = canBiometric
                                        pin = ""
                                        confirmPin = ""
                                        if (!canBiometric) error = "PIN nustatytas. Naudok jį prisijungimui."
                                    }
                                }
                            } else {
                                val saltString = prefs.getString("pin_salt", null)
                                val expected = prefs.getString("pin_hash", null)
                                val salt = try { Base64.decode(saltString, Base64.NO_WRAP) } catch (_: Exception) { null }
                                if (salt != null && expected != null && hashPin(pin, salt) == expected) {
                                    unlocked = true
                                    error = ""
                                } else {
                                    error = "Neteisingas PIN kodas."
                                    pin = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Green)
                    ) {
                        Text(if (setupMode) "Sukurti PIN kodą" else "Prisijungti su PIN")
                    }
                    if (!setupMode && canBiometric) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { authenticateBiometric() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Naudoti piršto antspaudą", color = Charcoal)
                        }
                    }
                }
            }
        }
    }
}

private fun hashPin(pin: String, salt: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256")
    digest.update(salt)
    return Base64.encodeToString(digest.digest(pin.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GateApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gate_data", android.content.Context.MODE_PRIVATE) }
    var gates by remember { mutableStateOf(loadGates(prefs)) }
    var showAdd by remember { mutableStateOf(false) }
    var editGate by remember { mutableStateOf<Gate?>(null) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }
    var radius by remember { mutableIntStateOf(10) }
    var auto by remember { mutableStateOf(false) }
    var testMode by remember { mutableStateOf(true) }
    var pendingCall by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf("") }

    val exportBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            try {
                val payload = JSONObject().apply {
                    put("format", "vartu-valdymas-backup")
                    put("version", 1)
                    put("createdAt", System.currentTimeMillis())
                    put("gates", gatesToJson(gates))
                }
                context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(payload.toString(2)) }
                statusMessage = "Atsarginė kopija išsaugota. Failą gali laikyti telefone arba įkelti į Google Drive."
            } catch (e: Exception) { statusMessage = "Nepavyko sukurti kopijos: ${e.message ?: "klaida"}" }
        }
    }
    fun save() { saveGates(prefs, gates) }

    val importBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: throw IllegalStateException("Failo nepavyko perskaityti")
                val rootJson = JSONObject(json)
                if (rootJson.optString("format") != "vartu-valdymas-backup") throw IllegalArgumentException("Tai ne Vartų valdymas kopija")
                val restored = jsonToGates(rootJson.getJSONArray("gates"))
                gates = restored
                save()
                statusMessage = "Atkurta šlagbaumų: ${restored.size}."
            } catch (e: Exception) { statusMessage = "Nepavyko atkurti kopijos: ${e.message ?: "netinkamas failas"}" }
        }
    }

    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) {
            val cursor = context.contentResolver.query(uri, arrayOf(ContactsContract.Contacts._ID), null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val id = it.getString(0)
                    val p = context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?", arrayOf(id), null
                    )
                    p?.use { c -> if (c.moveToFirst()) phone = c.getString(0) }
                }
            }
        }
    }
    val callPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val number = pendingCall
        if (granted && !number.isNullOrBlank()) {
            try { context.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(number)}"))) }
            catch (_: Exception) { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}"))) }
        } else if (!number.isNullOrBlank()) {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}")))
        }
        pendingCall = null
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = Green, secondary = Yellow, error = Red, background = Cream, surface = Color.White)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Column { Text("Mano šlagbaumai", fontWeight = FontWeight.Bold, color = Color.White); Text("Vartų valdymas", color = Color.White.copy(alpha=.8f), style = MaterialTheme.typography.labelSmall) } },
                    actions = { IconButton(onClick = { showAdd = true; editGate = null; name=""; phone=""; lat=""; lon=""; radius=10; auto=false }) { Icon(Icons.Default.Add, "Pridėti", tint = Yellow) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Green)
                )
            },
            containerColor = Cream
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Saugumo režimas", color = Charcoal, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Automatinis skambinimas", color = Charcoal)
                                Text("IŠJUNGTA – automatika dar nepatikrinta", color = Red, style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(checked = false, onCheckedChange = { testMode = true }, enabled = false, colors = SwitchDefaults.colors(checkedTrackColor = Green))
                        }
                        Text("Rankinis skambinimas veikia. GPS geozonos ir automatinio skambinimo variklis dar neįdiegtas; automatikos jungikliai nieko automatiškai nedaro.", color = Red, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (gates.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.EditLocation, null, tint = Green, modifier = Modifier.size(48.dp))
                            Text("Dar nėra pridėtų šlagbaumų", color = Charcoal, fontWeight = FontWeight.SemiBold)
                            Button(onClick = { showAdd=true; name=""; phone=""; lat=""; lon=""; radius=10; auto=false }, colors=ButtonDefaults.buttonColors(containerColor=Green)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Pridėti šlagbaumą") }
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                        items(gates, key = { it.id }) { gate ->
                            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(gate.name, fontWeight = FontWeight.Bold, color = Charcoal)
                                            Text(gate.phone, color = Charcoal.copy(alpha=.75f), style = MaterialTheme.typography.bodyMedium)
                                            Text(if (gate.autoEnabled) "Automatika pažymėta (prototipas)" else "Rankinis valdymas", color = if(gate.autoEnabled) Green else Charcoal.copy(alpha=.65f), style = MaterialTheme.typography.labelSmall)
                                        }
                                        IconButton(onClick = { gates = gates.filterNot { it.id == gate.id }; save() }) { Icon(Icons.Default.Delete, "Ištrinti", tint=Red) }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = {
                                            val uri = if (gate.lat != null && gate.lon != null) Uri.parse("geo:${gate.lat},${gate.lon}?q=${gate.lat},${gate.lon}(${Uri.encode(gate.name)})") else Uri.parse("geo:0,0?q=${Uri.encode(gate.name)}")
                                            try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) { statusMessage = "Žemėlapio programėlė nerasta." }
                                        }) { Icon(Icons.Default.Map, null); Text("Žemėlapis", color=Charcoal) }
                                        Button(onClick = {
                                            pendingCall = gate.phone
                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                                                try { context.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(gate.phone)}"))) }
                                                catch (_: Exception) { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(gate.phone)}"))) }
                                            } else callPermission.launch(Manifest.permission.CALL_PHONE)
                                        }, modifier=Modifier.weight(1f), colors=ButtonDefaults.buttonColors(containerColor=Green)) {
                                            Icon(Icons.Default.Call, null); Spacer(Modifier.width(6.dp)); Text("Skambinti")
                                        }
                                        OutlinedButton(onClick = {
                                            editGate=gate; name=gate.name; phone=gate.phone; lat=gate.lat?.toString() ?: ""; lon=gate.lon?.toString() ?: ""; radius=gate.radius; auto=gate.autoEnabled; showAdd=true
                                        }) { Text("Redaguoti", color=Charcoal) }
                                    }
                                }
                            }
                        }
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Atsarginė kopija", color = Charcoal, fontWeight = FontWeight.Bold)
                        Text("Eksportuok kopiją į failą arba pasirink Google Drive kaip išsaugojimo vietą. Debesijos paskyros duomenų programa pati nesinchronizuoja.", color = Charcoal.copy(alpha=.75f), style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { exportBackup.launch("vartu-valdymas-kopija.json") }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.FileDownload, null); Spacer(Modifier.width(4.dp)); Text("Eksportuoti") }
                            OutlinedButton(onClick = { importBackup.launch(arrayOf("application/json", "text/*")) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.FileUpload, null); Spacer(Modifier.width(4.dp)); Text("Atkurti") }
                        }
                        if (statusMessage.isNotBlank()) Text(statusMessage, color = if(statusMessage.startsWith("Nepavyko")) Red else Green, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text("Duomenys saugomi šiame telefone. Versija v0.3.0", color=Charcoal.copy(alpha=.6f), style=MaterialTheme.typography.labelSmall)
            }
        }
        if (showAdd) {
            AlertDialog(
                onDismissRequest = { showAdd=false },
                title = { Text(if(editGate == null) "Pridėti šlagbaumą" else "Redaguoti šlagbaumą") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(name, { name=it }, label={Text("Pavadinimas")}, singleLine=true)
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            OutlinedTextField(phone, { phone=it }, label={Text("Telefono numeris")}, singleLine=true, modifier=Modifier.weight(1f))
                            IconButton(onClick={contactPicker.launch(null)}) { Icon(Icons.Default.Call, "Pasirinkti kontaktą", tint=Green) }
                        }
                        OutlinedTextField(lat, { lat=it }, label={Text("Platuma (neprivaloma)")}, singleLine=true)
                        OutlinedTextField(lon, { lon=it }, label={Text("Ilguma (neprivaloma)")}, singleLine=true)
                        Text("Vietą žemėlapyje galėsime pasirinkti kitame etape. Kol kas galima įvesti koordinates.", style=MaterialTheme.typography.bodySmall, color=Charcoal)
                        Text("Automatikos atstumas: $radius m")
                        Slider(value=radius.toFloat(), onValueChange={radius=it.toInt()}, valueRange=5f..100f, steps=18)
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Text("Leisti automatikai naudoti šį šlagbaumą (automatikos variklis šiame leidime dar neaktyvus)", modifier=Modifier.weight(1f))
                            Switch(checked=auto, onCheckedChange={auto=it})
                        }
                    }
                },
                confirmButton = { Button(onClick={
                    if(name.isBlank() || phone.isBlank()) {
                        Toast.makeText(context, "Įvesk pavadinimą ir telefono numerį", Toast.LENGTH_SHORT).show()
                    } else {
                        val gate=Gate(editGate?.id ?: System.currentTimeMillis(), name.trim(), phone.trim(), lat.toDoubleOrNull(), lon.toDoubleOrNull(), radius, auto)
                        gates = if(editGate == null) gates + gate else gates.map { if(it.id==gate.id) gate else it }
                        save(); showAdd=false
                    }
                }, colors=ButtonDefaults.buttonColors(containerColor=Green)) { Text("Išsaugoti") } },
                dismissButton = { TextButton(onClick={showAdd=false}) { Text("Atšaukti") } }
            )
        }
    }
}

private fun loadGates(prefs: android.content.SharedPreferences): List<Gate> = try {
    jsonToGates(JSONArray(prefs.getString("gates", "[]")))
} catch (_: Exception) { emptyList() }

private fun jsonToGates(arr: JSONArray): List<Gate> = (0 until arr.length()).map { i ->
    val o = arr.getJSONObject(i)
    Gate(o.getLong("id"), o.getString("name"), o.getString("phone"),
        if(o.isNull("lat")) null else o.getDouble("lat"),
        if(o.isNull("lon")) null else o.getDouble("lon"),
        o.optInt("radius", 10), o.optBoolean("autoEnabled", false))
}

private fun gatesToJson(gates: List<Gate>): JSONArray {
    val arr = JSONArray()
    gates.forEach { g ->
        arr.put(JSONObject().apply {
            put("id", g.id); put("name", g.name); put("phone", g.phone)
            if(g.lat == null) put("lat", JSONObject.NULL) else put("lat", g.lat)
            if(g.lon == null) put("lon", JSONObject.NULL) else put("lon", g.lon)
            put("radius", g.radius); put("autoEnabled", g.autoEnabled)
        })
    }
    return arr
}

private fun saveGates(prefs: android.content.SharedPreferences, gates: List<Gate>) {
    prefs.edit().putString("gates", gatesToJson(gates).toString()).apply()
}
