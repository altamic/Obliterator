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
    @Test(expected = Throwable::class)
    fun generateFromCalendar() {
        val testScheduler = TestScheduler()

        val mockTimer     = {
            val initialDelayInMinutes: Long = 0
            val periodInMinutes: Long = 1

            Observable.interval(initialDelayInMinutes,
                    periodInMinutes,
                    TimeUnit.MINUTES,
                    testScheduler)
        }.invoke()

        val calendar = Calendar.getInstance()
        val flowObservable = flow.generateFrom(calendar, timer = mockTimer)

        val result = ArrayList<Calendar>()

        val listener = object: MainActivity.OnCalendarUpdated {
            override fun onCalendarUpdated(calendar: Calendar?) {
                result.add(calendar!!)
            }

            override fun onCalendarUpdateError(e: Throwable?) {
                throw e!!
            }
        }

        val subscriber = flow.calendarSubscriber(listener = listener)

        val eventsNumber = 100
        flowObservable.take(eventsNumber + 1).subscribe(subscriber)

        eventsNumber.times { index ->
            testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
            calendar.add(Calendar.MINUTE, 1)

            assertEquals(calendar, result[index])

        }

        subscriber.onError(Throwable())
    }

    @Test
    fun timer() {
        val testScheduler = TestScheduler()
        val timer  = flow.timer(scheduler = testScheduler)

        val result = ArrayList<Long>()

        val subscriber = object: Subscriber<Long>() {
            override fun onNext(t: Long?) {
                result.add(t!!)
            }

            override fun onError(e: Throwable?) {

            }

            override fun onCompleted() {

            }
        }

        val eventsNumber = 100
        timer.take(eventsNumber).subscribe(subscriber)

        eventsNumber.times { index ->
            testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)
            assertEquals(index.toLong(), result[index])
        }
    }
}
