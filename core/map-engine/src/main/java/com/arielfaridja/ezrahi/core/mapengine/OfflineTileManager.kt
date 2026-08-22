package com.arielfaridja.ezrahi.core.mapengine

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Environment
import android.util.Log
import java.io.File

object OfflineTileManager {

    data class OfflinePackage(
        val id: String,
        val name: String,
        val file: File,
        val minZoom: Int,
        val maxZoom: Int
    )

    private val SEARCH_DIRS = listOf(
        Environment.getExternalStorageDirectory(),
        File(Environment.getExternalStorageDirectory(), "Ezrahi/maps"),
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    )

    fun discoverPackages(): List<OfflinePackage> {
        val result = mutableListOf<OfflinePackage>()
        for (dir in SEARCH_DIRS) {
            val files = runCatching { dir.listFiles { f -> f.extension.equals("mbtiles", true) } }.getOrNull() ?: continue
            for (file in files) {
                runCatching {
                    val meta = readMetadata(file)
                    result.add(OfflinePackage(file.nameWithoutExtension, meta.name, file, meta.minZoom, meta.maxZoom))
                }.onFailure { Log.w("OfflineTileManager", "skip ${file.name}: ${it.message}") }
            }
        }
        return result
    }

    fun resolveStyleUri(context: Context): String {
        val pkg = discoverPackages().firstOrNull()
        if (pkg != null) {
            runCatching {
                val extractedDir = extractToCache(context, pkg)
                val styleJson = buildStyleJson(extractedDir, pkg)
                val styleFile = File(context.cacheDir, "mbtiles_style_${pkg.id}.json")
                styleFile.writeText(styleJson)
                return "file://" + styleFile.absolutePath
            }.onFailure { Log.w("OfflineTileManager", "Failed to build mbtiles style: ${it.message}") }
        }
        return MapLibreConfig.ONLINE_STYLE_URI
    }

    private fun readMetadata(file: File): MetaData {
        val db = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        val cursor = db.query("metadata", null, null, null, null, null, null)
        val map = mutableMapOf<String, String>()
        while (cursor.moveToNext()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            val value = cursor.getString(cursor.getColumnIndexOrThrow("value"))
            map[name] = value
        }
        cursor.close()
        db.close()
        return MetaData(
            name = map["name"] ?: file.nameWithoutExtension,
            minZoom = map["minzoom"]?.toIntOrNull() ?: 0,
            maxZoom = map["maxzoom"]?.toIntOrNull() ?: 18
        )
    }

    private fun extractToCache(context: Context, pkg: OfflinePackage): File {
        val outDir = File(context.cacheDir, "mbtiles/${pkg.id}")
        if (outDir.listFiles()?.isNotEmpty() == true) return outDir
        outDir.mkdirs()
        val db = SQLiteDatabase.openDatabase(pkg.file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        val cursor = db.query("tiles", arrayOf("zoom_level", "tile_column", "tile_row", "tile_data"), null, null, null, null, null)
        while (cursor.moveToNext()) {
            val z = cursor.getInt(0)
            val x = cursor.getInt(1)
            val yFlipped = cursor.getInt(2)
            val y = (1 shl z) - 1 - yFlipped
            val data = cursor.getBlob(3)
            val tileDir = File(outDir, "$z/$x")
            tileDir.mkdirs()
            File(tileDir, "$y.png").writeBytes(data)
        }
        cursor.close()
        db.close()
        return outDir
    }

    private fun buildStyleJson(tilesDir: File, pkg: OfflinePackage): String {
        val tileUrl = "file://" + tilesDir.absolutePath + "/{z}/{x}/{y}.png"
        return """
        {
          "version": 8,
          "sources": {
            "offline": {
              "type": "raster",
              "tiles": ["$tileUrl"],
              "tileSize": 256,
              "minzoom": ${pkg.minZoom},
              "maxzoom": ${pkg.maxZoom}
            }
          },
          "layers": [
            { "id": "background", "type": "background", "paint": { "background-color": "#f8f4f0" } },
            { "id": "offline", "type": "raster", "source": "offline" }
          ]
        }
        """.trimIndent()
    }

    private data class MetaData(val name: String, val minZoom: Int, val maxZoom: Int)
}
