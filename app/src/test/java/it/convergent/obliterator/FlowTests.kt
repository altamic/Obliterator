package it.convergent.obliterator

import org.junit.Assert.assertEquals
import org.junit.Test
import rx.Observable
import rx.Subscriber
import rx.schedulers.TestScheduler
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by altamic on 23/08/16.
 */

class FlowTests {

    private val testScheduler = TestScheduler()

    private val mockTimer     = {
        val initialDelayInMinutes: Long = 0
        val periodInMinutes: Long = 1

        Observable.interval(initialDelayInMinutes,
                periodInMinutes,
                TimeUnit.MINUTES,
                testScheduler)
    }.invoke()


    @Test
    fun generateFromCalendar() {
        val calendar = Calendar.getInstance()
        val flow = flow.generateFrom(calendar, timer = mockTimer)
        val result = ArrayList<Calendar>()

        val subscriber = object: Subscriber<Calendar>() {
            override fun onNext(t: Calendar?) {
                result.add(t!!)
            }

            override fun onError(e: Throwable?) {

            }

            override fun onCompleted() {

            }
        }

        val eventsNumber = 100
        flow.take(eventsNumber).subscribe(subscriber)

        eventsNumber.times { index ->
            testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
            calendar.add(Calendar.MINUTE, 1)

            assertEquals(calendar, result[index])

        }
    }

    @Test
    fun calendarSubscriberListener() {

    }
}