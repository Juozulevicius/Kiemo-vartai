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
    var chosen by remember { mutableStateOf(GeoPoint(initialLat, initialLon)) }
    var mapRef by remember { mutableStateOf<MapView?>(null) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Pasirink šlagbaumo vietą", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Green)
                Text("Paliesk žemėlapį, kad perkeltum žymeklį. Priartink dviem pirštais.", style = MaterialTheme.typography.bodySmall, color = Charcoal)
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(420.dp),
                    factory = { ctx ->
                        Configuration.getInstance().userAgentValue = ctx.packageName
                        MapView(ctx).apply {
                            mapRef = this
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(16.0)
                            controller.setCenter(chosen)
                            overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                    chosen = p
                                    mapRef?.overlays?.removeAll { it is OsmMarker }
                                    mapRef?.overlays?.add(OsmMarker(this@apply).apply {
                                        position = p
                                        setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_BOTTOM)
                                        title = "Pasirinkta šlagbaumo vieta"
                                    })
                                    mapRef?.invalidate()
                                    return true
                                }
                                override fun longPressHelper(p: GeoPoint): Boolean {
                                    chosen = p
                                    mapRef?.overlays?.removeAll { it is OsmMarker }
                                    mapRef?.overlays?.add(OsmMarker(this@apply).apply {
                                        position = p
                                        setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_BOTTOM)
                                        title = "Pasirinkta šlagbaumo vieta"
                                    })
                                    mapRef?.invalidate()
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
                    update = { it.invalidate() }
                )
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
