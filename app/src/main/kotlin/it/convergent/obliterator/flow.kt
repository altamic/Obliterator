package it.convergent.obliterator

import it.convergent.obliterator.MainActivity.OnCalendarUpdated
import rx.Observable
import rx.Scheduler
import rx.Subscriber
import rx.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by altamic on 02/07/16.
 */

object flow {
    fun timer(scheduler: Scheduler = Schedulers.computation()): Observable<Long> {
        val initialDelayInMinutes: Long = 0
        val periodInMinutes: Long = 1

        return Observable.interval(initialDelayInMinutes,
                                    periodInMinutes, TimeUnit.MINUTES, scheduler)
    }

    fun generateFrom(calendar: Calendar,
                        timer: Observable<Long> = timer()): Observable<Calendar> {
        calendar.add(Calendar.MINUTE, -1)

        return Observable
                .just(calendar)
                .flatMap { timer }
                .map { _ ->
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
