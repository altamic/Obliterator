package it.convergent.obliterator.models

import java.util.*

/**
 * Created by altamic on 08/10/16.
 */
object CarnetsManager {
    private val carnets: Map<String, Array<Carnet>> = emptyMap()

    fun isPredecessorAvailable(carnet: Carnet): Boolean {
        val uid = carnet.uid
        val remainingRides = carnet.remainingRides
        return find(uid, remainingRides) != null
    }

    fun predecessor(carnet: Carnet): Carnet? {
        val uid = carnet.uid
        val remainingRides = carnet.remainingRides

        return find(uid, remainingRides)
    }

    fun store(carnet: Carnet) {
        val uid = carnet.uid
        val values = carnets.getOrElse(uid, defaultValue = { emptyArray<Carnet>() })

        if (values.isEmpty()) {
            val carnetArray = arrayOf(carnet)
            carnets.entries.plusElement(mapOf(Pair(uid, carnetArray)))
        } else { // not empty array values for given uid
            if (values.none() { it == carnet }) { // carnet is present?
                // TODO place in the right position
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
        val values = carnets.getOrElse(uid, defaultValue = { emptyArray<Carnet>() })
        try {
            return values.first { it.remainingRides == ridesNumber }
        } catch (e: NoSuchElementException) {
            return null
        }
    }
}