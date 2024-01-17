package com.arielfaridja.ezrahi.UI

class ExtensionMethods {
    companion object {
        inline fun <reified T : Enum<T>> Int.toEnum(): T? {
            return enumValues<T>().firstOrNull { it.ordinal == this }
        }

        //Enum to Int
        inline fun <reified T : Enum<T>> T.toInt(): Int {
            return this.ordinal
        }
    }
}