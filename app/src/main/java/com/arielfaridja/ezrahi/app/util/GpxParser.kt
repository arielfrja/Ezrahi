package com.arielfaridja.ezrahi.app.util

import com.arielfaridja.ezrahi.domain.model.GeoPoint
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

object GpxParser {

    fun parse(xml: String): List<GeoPoint> {
        val points = mutableListOf<GeoPoint>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(xml.reader())
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG &&
                (parser.name == "trkpt" || parser.name == "rtept")
            ) {
                val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                if (lat != null && lon != null) {
                    points += GeoPoint(latitude = lat, longitude = lon)
                }
            }
            eventType = parser.next()
        }
        return points
    }
}