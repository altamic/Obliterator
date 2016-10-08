package it.convergent.obliterator.models

import it.convergent.obliterator.hexStringToByteArray

/**
 * Created by altamic on 08/10/16.
 */
object CarnetsManager {
    fun isPredecessorAvailable(carnet: Carnet): Boolean {
        return false
    }

    fun predecessor(carnet: Carnet): Carnet {
        return Carnet(data = hexStringToByteArray("0404"))
    }

    fun store(carnet: Carnet) {
//        calculate bucket from uid
//
    }

}