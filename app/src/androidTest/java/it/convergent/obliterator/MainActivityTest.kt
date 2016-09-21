package it.convergent.obliterator


import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.view.View
import android.view.ViewGroup
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.IsInstanceOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun mainActivityTest() {
        val textView = onView(
                allOf(withId(R.id.gttTimeHex), isDisplayed()))

        textView.check { view, noMatchingViewException ->  }

        val toggleButton = onView(
                allOf(withId(R.id.toggleChangeDateTime),
                        childAtPosition(
                                allOf(withId(R.id.grabLayout),
                                        childAtPosition(
                                                IsInstanceOf.instanceOf<View>(android.widget.RelativeLayout::class.java),
                                                0)),
                                4),
                        isDisplayed()))
        toggleButton.check(matches(isDisplayed()))

        val textView2 = onView(
                allOf(withId(R.id.gttTimeBinary), withText("010111011110011111111001"),
                        childAtPosition(
                                allOf(withId(R.id.grabLayout),
                                        childAtPosition(
                                                IsInstanceOf.instanceOf<View>(android.widget.RelativeLayout::class.java),
                                                0)),
                                1),
                        isDisplayed()))
        textView2.check(matches(withText("010111011110011111111001")))

    }

    private fun childAtPosition(
            parentMatcher: Matcher<View>, position: Int): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
