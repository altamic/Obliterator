package it.convergent.obliterator.models

import it.convergent.obliterator.hexStringToByteArray

/**
 * Created by altamic on 08/10/16.
 */
object CarnetsManager {
    private val carnets: Map<String, Array<Carnet>> = emptyMap()

    fun isPredecessorAvailable(carnet: Carnet): Boolean {
//        get remaining rides R from carnet
//        calculate bucket from uid
//        if R == total rides
//          if !exists
//              return false
//        if R < total rides
//                lookup for carnet with R - 1 rides
//                 if exists
//                    return true
//                 else
//                    return false
        return false
    }

    fun predecessor(carnet: Carnet): Carnet {
        return Carnet(data = hexStringToByteArray("0404"))
    }

    fun store(carnet: Carnet) {
        val uid = carnet.uid

        val values = carnets.getOrElse(uid, defaultValue = { emptyArray<Carnet>() })

        if (values.isEmpty()) {
            val carnetArray = arrayOf(carnet)
            carnets.entries.plusElement(mapOf(Pair(uid, carnetArray)))
        } else { // not empty values
            if (values.any() { it == carnet })
                return

            val oldValues = values
            val newValues = values.asList()
                    .reversed()
                    .plus(carnet)
                    .reversed() // double reverse to prepend the item
                    .toTypedArray()

            carnets.entries.minusElement(mapOf(Pair(uid, oldValues)))
                    .plusElement(mapOf(Pair(uid, newValues)))
        }

    }

    private fun exists(carnet: Carnet): Boolean {
        val uid = carnet.uid
        val values = carnets.getOrElse(uid, defaultValue = { emptyArray<Carnet>() })

        if (!values.isEmpty()) {
            return values.any() { it == carnet }
        }

        return false
    }

    private fun find(uid: String, ridesNumber: Int): Carnet? {
        if (carnets.keys.contains(uid)) {
            val values = carnets.get(uid).orEmpty()

            if (!values.isEmpty()) {
                return values.first { it.remainingRides == ridesNumber }
            }
        }

        return null
    }
}