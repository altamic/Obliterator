package it.convergent.obliterator

import java.util.*
import rx.Observable
import rx.Subscriber
import it.convergent.obliterator.MainActivity.OnCalendarUpdated
import java.util.concurrent.TimeUnit

/**
 * Created by altamic on 02/07/16.
 */
object flow {
    fun generateFrom(calendar: Calendar): Observable<Calendar> {
        val initialDelayInMinutes: Long = 0
        val periodInMinutes: Long = 1
        val timer = Observable.interval(initialDelayInMinutes,
                                        periodInMinutes, TimeUnit.MINUTES)

        calendar.add(Calendar.MINUTE, -1)

        return Observable
                .just(calendar)
                .flatMap { timer }
                .map { tick ->
                    calendar.add(Calendar.MINUTE, 1)
                    calendar
                }
        }

    fun calendarSubscriber(listener: OnCalendarUpdated): Subscriber<Calendar> {
      return object: Subscriber<Calendar>() {
          override fun onNext(t: Calendar?) {
              listener.onCalendarUpdated(t)
          }

          override fun onError(e: Throwable?) {
              listener.onCalendarUpdateError(e)
          }

          override fun onCompleted() {
          }
      }
    }
}
