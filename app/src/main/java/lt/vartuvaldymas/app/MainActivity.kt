package lt.vartuvaldymas.app

import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker as OsmMarker
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.events.MapEventsReceiver
import android.Manifest
import android.location.Location
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.OpenableColumns
import android.widget.Toast
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.ui.res.painterResource
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

private const val GITHUB_LATEST_RELEASE_API = "https://api.github.com/repos/Juozulevicius/VartuValdymas-Releases/releases/latest"
private const val CURRENT_APP_VERSION = "0.4.6"

private data class AppRelease(val tag: String, val name: String, val notes: String, val apkUrl: String?)

private fun isNewerVersion(latestTag: String, currentVersion: String): Boolean {
    fun parts(value: String) = value.trim().removePrefix("v").substringBefore("-")
        .split(".").map { it.toIntOrNull() ?: 0 }
    val latest = parts(latestTag)
    val current = parts(currentVersion)
    for (i in 0 until maxOf(latest.size, current.size)) {
        val a = latest.getOrElse(i) { 0 }
        val b = current.getOrElse(i) { 0 }
        if (a != b) return a > b
    }
    return false
}

private suspend fun fetchLatestRelease(): AppRelease = withContext(Dispatchers.IO) {
    val connection = (URL(GITHUB_LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 12000
        readTimeout = 12000
        setRequestProperty("Accept", "application/vnd.github+json")
        setRequestProperty("User-Agent", "VartuValdymas-Android")
    }
    try {
        if (connection.responseCode !in 200..299) {
            when (connection.responseCode) {
                404 -> throw IllegalStateException("GitHub Release nerastas. Patikrink, ar Kiemo-vartai saugykla vieša ir ar joje paskelbtas Release su APK.")
                403 -> throw IllegalStateException("GitHub laikinai apribojo užklausas. Bandyk vėliau.")
                else -> throw IllegalStateException("GitHub atsakė: ${connection.responseCode}")
            }
        }
        val json = connection.inputStream.bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
        val assets = json.optJSONArray("assets") ?: JSONArray()
        var apkUrl: String? = null
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                apkUrl = asset.optString("browser_download_url").takeIf { it.startsWith("https://") }
                if (apkUrl != null) break
            }
        }
        AppRelease(json.optString("tag_name"), json.optString("name").ifBlank { json.optString("tag_name") }, json.optString("body"), apkUrl)
    } finally { connection.disconnect() }
}

private suspend fun downloadReleaseApk(context: android.content.Context, url: String): File = withContext(Dispatchers.IO) {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 15000
        readTimeout = 30000
        setRequestProperty("User-Agent", "VartuValdymas-Android")
    }
    try {
        if (connection.responseCode !in 200..299) throw IllegalStateException("APK atsisiųsti nepavyko: ${connection.responseCode}")
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val file = File(dir, "vartu-valdymas-update.apk")
        connection.inputStream.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
        if (file.length() < 100_000) throw IllegalStateException("Atsisiųstas APK atrodo netinkamas")
        file
    } finally { connection.disconnect() }
}

private fun openApkInstaller(activity: android.app.Activity, apk: File) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !activity.packageManager.canRequestPackageInstalls()) {
        val settings = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}"))
        activity.startActivity(settings)
        Toast.makeText(activity, "Leisk diegti šios programos APK, tada dar kartą paspausk „Tikrinti atnaujinimus“.", Toast.LENGTH_LONG).show()
        return
    }
    val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", apk)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    activity.startActivity(intent)
}

data class Gate(val id: Long, val name: String, val phone: String, val lat: Double?, val lon: Double?, val radius: Int, val autoEnabled: Boolean)

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AuthenticationGate(activity = this) }
    }
}

@Composable
private fun AuthenticationGate(activity: FragmentActivity) {
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
    val activity = context as? android.app.Activity
    val updateScope = rememberCoroutineScope()
    var updateStatus by remember { mutableStateOf("Atnaujinimai tikrinami per GitHub Releases.") }
    var checkingUpdate by remember { mutableStateOf(false) }
    var availableRelease by remember { mutableStateOf<AppRelease?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    val prefs = remember { context.getSharedPreferences("gate_data", android.content.Context.MODE_PRIVATE) }
    var gates by remember { mutableStateOf(loadGates(prefs)) }
    var showAdd by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }
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

    fun performDirectCall(number: String) {
        val callIntent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.fromParts("tel", number, null)
        }
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                statusMessage = "Skambinimo leidimas nesuteiktas. Paspausk „Skambinti“ dar kartą ir leisk skambinti."
                Toast.makeText(context, statusMessage, Toast.LENGTH_LONG).show()
                return
            }
            context.startActivity(callIntent)
            statusMessage = "Pradedamas skambutis į $number…"
            Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            statusMessage = "Android neleido pradėti skambučio. Atidaromas rinkiklis."
            Toast.makeText(context, statusMessage, Toast.LENGTH_LONG).show()
            try {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)))
            } catch (_: Exception) {
                statusMessage = "Nepavyko atidaryti telefono rinkiklio. Patikrink SIM kortelę ir telefono programą."
                Toast.makeText(context, statusMessage, Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            statusMessage = "Nepavyko pradėti skambučio. Atidaromas rinkiklis."
            Toast.makeText(context, statusMessage, Toast.LENGTH_LONG).show()
            try {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)))
            } catch (_: Exception) {
                statusMessage = "Nepavyko atidaryti telefono rinkiklio."
                Toast.makeText(context, statusMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val number = pendingCall
        pendingCall = null
        if (granted && number != null) {
            performDirectCall(number)
        } else if (number != null) {
            statusMessage = "Skambinimo leidimas nesuteiktas – atidaromas rinkiklis."
            Toast.makeText(context, statusMessage, Toast.LENGTH_LONG).show()
            try {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)))
            } catch (_: Exception) {
                statusMessage = "Nepavyko atidaryti rinkiklio. Patikrink, ar telefone yra skambinimo programa."
                Toast.makeText(context, statusMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

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
    fun dialGateNumber(rawNumber: String) {
        val normalized = rawNumber.trim().replace(Regex("[\\s()\\-]"), "")
        if (normalized.isBlank() || normalized.count { it.isDigit() } < 3) {
            statusMessage = "Telefono numeris neteisingas. Redaguok šlagbaumą ir patikrink numerį."
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            performDirectCall(normalized)
        } else {
            pendingCall = normalized
            statusMessage = "Prašoma leidimo skambinti…"
            Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }


    MaterialTheme(colorScheme = lightColorScheme(primary = Green, secondary = Yellow, error = Red, background = Cream, surface = Color.White)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(painter = painterResource(id = R.mipmap.ic_launcher), contentDescription = "Vartų valdymas", tint = Color.Unspecified, modifier = Modifier.size(38.dp)); Column { Text("Mano šlagbaumai", fontWeight = FontWeight.Bold, color = Color.White); Text("Vartų valdymas", color = Color.White.copy(alpha=.8f), style = MaterialTheme.typography.labelSmall) } } },
                    actions = { IconButton(onClick = { showAdd = true; editGate = null; name=""; phone=""; lat=""; lon=""; radius=10; auto=false }) { Icon(Icons.Default.Add, "Pridėti", tint = Yellow) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Green)
                )
            },
            containerColor = Cream
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                        Text("Rankinis skambinimas atidaro telefono rinkiklį. GPS geozonos ir automatinio skambinimo variklis dar neįdiegtas; automatika išjungta.", color = Red, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (gates.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.EditLocation, null, tint = Green, modifier = Modifier.size(48.dp))
                            Text("Dar nėra pridėtų šlagbaumų", color = Charcoal, fontWeight = FontWeight.SemiBold)
                            Button(onClick = { showAdd=true; name=""; phone=""; lat=""; lon=""; radius=10; auto=false }, colors=ButtonDefaults.buttonColors(containerColor=Green)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Pridėti šlagbaumą") }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        gates.forEach { gate ->
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
                                    if (gate.lat != null && gate.lon != null) {
                                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Cream)) {
                                            Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Green)
                                                    Text("Vartų vieta", modifier = Modifier.weight(1f), color = Charcoal, fontWeight = FontWeight.SemiBold)
                                                    TextButton(onClick = {
                                                        val uri = Uri.parse("https://www.openstreetmap.org/?mlat=${gate.lat}&mlon=${gate.lon}#map=17/${gate.lat}/${gate.lon}")
                                                        try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) { statusMessage = "Nepavyko atidaryti žemėlapio." }
                                                    }) { Text("Atidaryti žemėlapį", color = Green, maxLines = 1) }
                                                }
                                                GateMapPreview(lat = gate.lat, lon = gate.lon, title = gate.name, modifier = Modifier.fillMaxWidth().height(150.dp))
                                                Text("© OpenStreetMap contributors", style = MaterialTheme.typography.labelSmall, color = Charcoal.copy(alpha = .65f))
                                            }
                                        }
                                    } else {
                                        Text("Vartų vieta nenustatyta. Redaguok vartus ir įvesk koordinates.", color = Charcoal.copy(alpha=.7f), style = MaterialTheme.typography.bodySmall)
                                    }
                                    Button(onClick = { dialGateNumber(gate.phone) }, modifier=Modifier.fillMaxWidth().height(52.dp), colors=ButtonDefaults.buttonColors(containerColor=Green)) {
                                        Icon(Icons.Default.Call, null); Spacer(Modifier.width(8.dp)); Text("Skambinti", maxLines = 1, softWrap = false)
                                    }
                                    if (statusMessage.isNotBlank()) {
                                        Text(statusMessage, color = if (statusMessage.startsWith("Nepavyko") || statusMessage.contains("nesuteiktas")) Red else Green, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = {
                                            val uri = if (gate.lat != null && gate.lon != null) Uri.parse("https://www.openstreetmap.org/?mlat=${gate.lat}&mlon=${gate.lon}#map=17/${gate.lat}/${gate.lon}") else Uri.parse("https://www.openstreetmap.org/search?query=${Uri.encode(gate.name)}")
                                            try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) { statusMessage = "Nepavyko atidaryti žemėlapio." }
                                        }, modifier=Modifier.weight(1f).height(48.dp)) { Icon(Icons.Default.Map, null); Spacer(Modifier.width(5.dp)); Text("Žemėlapis", maxLines = 1, softWrap = false, color=Charcoal) }
                                        OutlinedButton(onClick = {
                                            editGate=gate; name=gate.name; phone=gate.phone; lat=gate.lat?.toString() ?: ""; lon=gate.lon?.toString() ?: ""; radius=gate.radius; auto=gate.autoEnabled; showAdd=true
                                        }, modifier=Modifier.weight(1f).height(48.dp)) { Text("Redaguoti", maxLines = 1, softWrap = false, color=Charcoal) }
                                    }
                                }
                            }
                        }
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Programos atnaujinimas", color = Charcoal, fontWeight = FontWeight.Bold)
                        Text("Dabartinė versija v$CURRENT_APP_VERSION. Naujos versijos ieškomos viešoje GitHub Releases skiltyje.", color = Charcoal.copy(alpha=.75f), style = MaterialTheme.typography.bodySmall)
                        Button(onClick = {
                            if (!checkingUpdate) {
                            checkingUpdate = true
                            updateStatus = "Tikrinama naujausia versija…"
                            updateScope.launch {
                                try {
                                    val release = fetchLatestRelease()
                                    availableRelease = release
                                    if (release.tag.isNotBlank() && isNewerVersion(release.tag, CURRENT_APP_VERSION)) {
                                        updateStatus = "Rasta versija ${release.tag}."
                                        showUpdateDialog = true
                                    } else {
                                        updateStatus = "Naujesnės versijos nerasta. Įdiegta v$CURRENT_APP_VERSION."
                                    }
                                } catch (e: Exception) {
                                    updateStatus = "Nepavyko patikrinti: ${e.message ?: "ryšio klaida"}"
                                } finally { checkingUpdate = false }
                            }
                            }
                        }, enabled = !checkingUpdate, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                            Text(if (checkingUpdate) "Tikrinama…" else "Tikrinti atnaujinimus")
                        }
                        Text(updateStatus, color = if(updateStatus.startsWith("Nepavyko")) Red else Charcoal.copy(alpha=.75f), style = MaterialTheme.typography.bodySmall)
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
                Text("Duomenys saugomi šiame telefone. Versija v$CURRENT_APP_VERSION", color=Charcoal.copy(alpha=.6f), style=MaterialTheme.typography.labelSmall)
            }
        }
        if (showUpdateDialog && availableRelease != null) {
            val release = availableRelease!!
            AlertDialog(
                onDismissRequest = { showUpdateDialog = false },
                title = { Text("Yra nauja versija: ${release.tag}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(release.name)
                        Text(release.notes.take(700).ifBlank { "Naujos versijos aprašymas nepateiktas." }, style = MaterialTheme.typography.bodySmall)
                        if (release.apkUrl == null) Text("Šiam leidimui GitHub Releases nėra pridėto APK failo. Pirmiausia reikia paskelbti APK.", color = Red)
                        Text("Atnaujinimas bus įdiegtas tik patvirtinus Android sistemos lange. Jei parašas nesutaps, Android neleis atnaujinti esamos programos.", color = Charcoal.copy(alpha=.75f), style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    Button(enabled = release.apkUrl != null, onClick = {
                        showUpdateDialog = false
                        updateScope.launch {
                            try {
                                updateStatus = "Atsisiunčiamas APK…"
                                val apk = downloadReleaseApk(context, release.apkUrl!!)
                                updateStatus = "APK atsisiųstas. Atidaromas diegimas…"
                                if (activity != null) openApkInstaller(activity, apk)
                            } catch (e: Exception) { updateStatus = "Nepavyko atsisiųsti atnaujinimo: ${e.message ?: "klaida"}" }
                        }
                    }) { Text("Atsisiųsti ir diegti") }
                },
                dismissButton = { TextButton(onClick = { showUpdateDialog = false }) { Text("Vėliau") } }
            )
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
                        OutlinedButton(
                            onClick = { showMapPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Pasirinkti vietą žemėlapyje", maxLines = 1, softWrap = false)
                        }
                        Text("Žemėlapį ir vietos žymeklį galima perkelti. Pasirinkus vietą, koordinatės įrašomos automatiškai.", style=MaterialTheme.typography.bodySmall, color=Charcoal)
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
        if (showMapPicker) {
            val initialLat = lat.toDoubleOrNull() ?: 55.0000
            val initialLon = lon.toDoubleOrNull() ?: 24.0000
            MapPickerDialog(
                initialLat = initialLat,
                initialLon = initialLon,
                onDismiss = { showMapPicker = false },
                onChoose = { point ->
                    lat = point.latitude.toString()
                    lon = point.longitude.toString()
                    showMapPicker = false
                }
            )
        }
    }
}


@Composable
private fun GateMapPreview(lat: Double, lon: Double, title: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = {
            Configuration.getInstance().userAgentValue = context.packageName
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(16.0)
                controller.setCenter(GeoPoint(lat, lon))
                overlays.add(OsmMarker(this).apply {
                    position = GeoPoint(lat, lon)
                    setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_BOTTOM)
                    this.title = title
                    snippet = "Šlagbaumo vieta"
                })
            }
        },
        update = { map ->
            map.controller.setCenter(GeoPoint(lat, lon))
            map.overlays.removeAll { it is OsmMarker }
            map.overlays.add(OsmMarker(map).apply {
                position = GeoPoint(lat, lon)
                setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_BOTTOM)
                this.title = title
                snippet = "Šlagbaumo vieta"
            })
            map.invalidate()
        }
    )
}

@Composable
private fun MapPickerDialog(
    initialLat: Double,
    initialLon: Double,
    onDismiss: () -> Unit,
    onChoose: (GeoPoint) -> Unit
) {
    val context = LocalContext.current
    val fusedLocationClient = remember(context) { LocationServices.getFusedLocationProviderClient(context) }
    var chosen by remember { mutableStateOf(GeoPoint(initialLat, initialLon)) }
    var mapRef by remember { mutableStateOf<MapView?>(null) }
    var locationMessage by remember { mutableStateOf("Nustatoma dabartinė vieta…") }

    fun putChosenMarker(map: MapView, point: GeoPoint, center: Boolean) {
        chosen = point
        map.overlays.removeAll { it is OsmMarker }
        map.overlays.add(OsmMarker(map).apply {
            position = point
            setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_BOTTOM)
            title = "Pasirinkta šlagbaumo vieta"
            snippet = "Pasirinkta vieta"
        })
        if (center) map.controller.animateTo(point)
        map.invalidate()
    }

    fun locateNow() {
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) {
            locationMessage = "Leisk naudoti vietą arba paspausk vietos nustatymo mygtuką."
            return
        }
        locationMessage = "Ieškoma dabartinės vietos…"
        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val point = GeoPoint(location.latitude, location.longitude)
                    mapRef?.let { putChosenMarker(it, point, true) } ?: run { chosen = point }
                    locationMessage = if (location.accuracy <= 100f) {
                        "Vieta nustatyta (tikslumas apie ${location.accuracy.toInt()} m)."
                    } else {
                        "Vieta nustatyta apytiksliai (±${location.accuracy.toInt()} m). Jei reikia, paliesk žemėlapį arba bandyk dar kartą."
                    }
                } else {
                    locationMessage = "Vietos nustatyti nepavyko. Įjunk telefono vietos nustatymą ir bandyk dar kartą."
                }
            }.addOnFailureListener {
                locationMessage = "Nepavyko nustatyti vietos. Patikrink telefono GPS ir bandyk dar kartą."
            }
        } catch (_: SecurityException) {
            locationMessage = "Nėra leidimo nustatyti vietą. Paspausk vietos mygtuką ir suteik leidimą."
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            locateNow()
        } else {
            locationMessage = "Vietos leidimas nesuteiktas. Gali pasirinkti vietą paliesdamas žemėlapį."
        }
    }

    fun requestOrLocate() {
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fineGranted || coarseGranted) locateNow()
        else locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    LaunchedEffect(Unit) { requestOrLocate() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
            Column(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Pasirink šlagbaumo vietą",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Green
                    )
                    IconButton(onClick = { requestOrLocate() }) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Nustatyti dabartinę vietą", tint = Green)
                    }
                }
                Text("Žemėlapis bando nustatyti tavo dabartinę vietą. Paliesk žemėlapį, kad pažymėtum šlagbaumo vietą.", style = MaterialTheme.typography.bodySmall, color = Charcoal)
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(420.dp),
                    factory = { ctx ->
                        Configuration.getInstance().userAgentValue = ctx.packageName
                        MapView(ctx).apply {
                            mapRef = this
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(15.0)
                            controller.setCenter(chosen)
                            overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                    putChosenMarker(this@apply, p, false)
                                    return true
                                }
                                override fun longPressHelper(p: GeoPoint): Boolean {
                                    putChosenMarker(this@apply, p, false)
                                    return true
                                }
                            }))
                            overlays.add(OsmMarker(this).apply {
                                position = chosen
                                setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_BOTTOM)
                                title = "Pasirinkta šlagbaumo vieta"
                            })
                        }
                    },
                    update = { map ->
                        if (mapRef !== map) mapRef = map
                        map.invalidate()
                    }
                )
                Text(locationMessage, style = MaterialTheme.typography.bodySmall, color = if (locationMessage.startsWith("Nepavyko") || locationMessage.startsWith("Vietos leidimas")) Red else Green)
                Text("Platuma: %.6f   Ilguma: %.6f".format(chosen.latitude, chosen.longitude), style = MaterialTheme.typography.bodySmall, color = Charcoal)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Atšaukti", maxLines = 1) }
                    Button(onClick = { onChoose(chosen) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                        Text("Naudoti vietą", maxLines = 1, softWrap = false)
                    }
                }
            }
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
