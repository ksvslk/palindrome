import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.android.material.snackbar.Snackbar
import com.indest.hah.MainActivity
import com.indest.hah.R
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.`is`
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class InGameNavigationTest {

    @Rule
    @JvmField
    var mActivityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun inGameNavigationTest() {
        val view = onView(
            allOf(
                withId(com.google.android.material.R.id.touch_outside),
                childAtPosition(
                    allOf(
                        withId(com.google.android.material.R.id.coordinator),
                        childAtPosition(
                            withId(R.id.container),
                            0
                        )
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        view.perform(click())

        val bottomNavigationItemView = onView(
            allOf(
                withId(R.id.navigation_stages), withContentDescription("Levels"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.bottomNavView),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        bottomNavigationItemView.perform(click())

        val anyButton = onView(
            allOf(
                withId(R.id.submit_your_level_button),
                childAtPosition(
                    childAtPosition(
                        withId(com.google.android.material.R.id.design_bottom_sheet),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        anyButton.perform(click())

        pressBack()

        val bottomNavigationItemView2 = onView(
            allOf(
                withId(R.id.navigation_stages), withContentDescription("Levels"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.bottomNavView),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        bottomNavigationItemView2.perform(click())

        val anyButton2 = onView(
            allOf(
                withId(R.id.player_levels_button),
                childAtPosition(
                    childAtPosition(
                        withId(com.google.android.material.R.id.design_bottom_sheet),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        anyButton2.perform(click())

        pressBack()

        val bottomNavigationItemView3 = onView(
            allOf(
                withId(R.id.navigation_stages), withContentDescription("Levels"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.bottomNavView),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        bottomNavigationItemView3.perform(click())

        val anyButton3 = onView(
            allOf(
                withId(R.id.game_levels_button),
                childAtPosition(
                    childAtPosition(
                        withId(com.google.android.material.R.id.design_bottom_sheet),
                        0
                    ),
                    3
                ),
                isDisplayed()
            )
        )
        anyButton3.perform(click())

        pressBack()

        val bottomNavigationItemView4 = onView(
            allOf(
                withId(R.id.navigation_stages), withContentDescription("Levels"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.bottomNavView),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        bottomNavigationItemView4.perform(click())

        val anyButton4 = onView(
            allOf(
                withId(R.id.player_levels_button),
                childAtPosition(
                    childAtPosition(
                        withId(com.google.android.material.R.id.design_bottom_sheet),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        anyButton4.perform(click())

        val recyclerView = onView(
            allOf(
                withId(R.id.player_level_select_recyclerview),
                childAtPosition(
                    withId(R.id.player_level_select_dialog),
                    1
                )
            )
        )
        recyclerView.perform(actionOnItemAtPosition<ViewHolder>(7, click()))

        val bottomNavigationItemView5 = onView(
            allOf(
                withId(R.id.navigation_stages), withContentDescription("Levels"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.bottomNavView),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        bottomNavigationItemView5.perform(click())

        val anyButton5 = onView(
            allOf(
                withId(R.id.player_levels_button),
                childAtPosition(
                    childAtPosition(
                        withId(com.google.android.material.R.id.design_bottom_sheet),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        anyButton5.perform(click())

        val view2 = onView(
            allOf(
                withId(com.google.android.material.R.id.touch_outside),
                childAtPosition(
                    allOf(
                        withId(com.google.android.material.R.id.coordinator),
                        childAtPosition(
                            withId(R.id.container),
                            0
                        )
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        view2.perform(click())

        val bottomNavigationItemView6 = onView(
            allOf(
                withId(R.id.navigation_stages), withContentDescription("Levels"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.bottomNavView),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        bottomNavigationItemView6.perform(click())

        val anyButton6 = onView(
            allOf(
                withId(R.id.game_levels_button),
                childAtPosition(
                    childAtPosition(
                        withId(com.google.android.material.R.id.design_bottom_sheet),
                        0
                    ),
                    3
                ),
                isDisplayed()
            )
        )
        anyButton6.perform(click())

        val recyclerView2 = onView(
            allOf(
                withId(R.id.level_select_recyclerview),
                childAtPosition(
                    withId(R.id.level_select_dialog),
                    1
                )
            )
        )
        recyclerView2.perform(actionOnItemAtPosition<ViewHolder>(2, click()))

        val materialButton = onView(
            allOf(
                withId(com.google.android.material.R.id.snackbar_action), withText("Play"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("com.google.android.material.snackbar.Snackbar${Snackbar.SnackbarLayout::javaClass}")),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        materialButton.perform(click())

        val view3 = onView(
            allOf(
                withId(com.google.android.material.R.id.touch_outside),
                childAtPosition(
                    allOf(
                        withId(com.google.android.material.R.id.coordinator),
                        childAtPosition(
                            withId(R.id.container),
                            0
                        )
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        view3.perform(click())

        val bottomNavigationItemView7 = onView(
            allOf(
                withId(R.id.navigation_google_games), withContentDescription("Play Games"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.bottomNavView),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        bottomNavigationItemView7.perform(click())

        val anyButton7 = onView(
            allOf(
                withId(R.id.leaderboards_button),
                childAtPosition(
                    childAtPosition(
                        withId(com.google.android.material.R.id.design_bottom_sheet),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        anyButton7.perform(click())

        val view4 = onView(
            allOf(
                withId(com.google.android.material.R.id.touch_outside),
                childAtPosition(
                    allOf(
                        withId(com.google.android.material.R.id.coordinator),
                        childAtPosition(
                            withId(R.id.container),
                            0
                        )
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        view4.perform(click())

        val bottomNavigationItemView8 = onView(
            allOf(
                withId(R.id.navigation_settings), withContentDescription("Settings"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.bottomNavView),
                        0
                    ),
                    3
                ),
                isDisplayed()
            )
        )
        bottomNavigationItemView8.perform(click())

        val linearLayoutCompat = onView(
            allOf(
                withId(R.id.tutorial_button),
                childAtPosition(
                    childAtPosition(
                        withId(com.google.android.material.R.id.design_bottom_sheet),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        linearLayoutCompat.perform(click())

        val view5 = onView(
            allOf(
                withId(com.google.android.material.R.id.touch_outside),
                childAtPosition(
                    allOf(
                        withId(com.google.android.material.R.id.coordinator),
                        childAtPosition(
                            withId(R.id.container),
                            0
                        )
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        view5.perform(click())

        val bottomNavigationItemView9 = onView(
            allOf(
                withId(R.id.navigation_settings), withContentDescription("Settings"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.bottomNavView),
                        0
                    ),
                    3
                ),
                isDisplayed()
            )
        )
        bottomNavigationItemView9.perform(click())

        val linearLayoutCompat2 = onView(
            allOf(
                withId(R.id.tutorial_button),
                childAtPosition(
                    childAtPosition(
                        withId(com.google.android.material.R.id.design_bottom_sheet),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        linearLayoutCompat2.perform(click())

        val materialButton2 = onView(
            allOf(
                withId(R.id.continueButton), withText("Next"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.tutorial_fragment),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        materialButton2.perform(click())

        val materialButton3 = onView(
            allOf(
                withId(R.id.startButton), withText("Next"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.third_tutorial_fragment),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        materialButton3.perform(click())

        val view6 = onView(
            allOf(
                withId(com.google.android.material.R.id.touch_outside),
                childAtPosition(
                    allOf(
                        withId(com.google.android.material.R.id.coordinator),
                        childAtPosition(
                            withId(R.id.container),
                            0
                        )
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        view6.perform(click())

        val bottomNavigationItemView10 = onView(
            allOf(
                withId(R.id.navigation_settings), withContentDescription("Settings"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.bottomNavView),
                        0
                    ),
                    3
                ),
                isDisplayed()
            )
        )
        bottomNavigationItemView10.perform(click())

        val linearLayoutCompat3 = onView(
            allOf(
                withId(R.id.tutorial_button),
                childAtPosition(
                    childAtPosition(
                        withId(com.google.android.material.R.id.design_bottom_sheet),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        linearLayoutCompat3.perform(click())

        val materialButton4 = onView(
            allOf(
                withId(R.id.continueButton), withText("Next"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.tutorial_fragment),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        materialButton4.perform(click())

        pressBack()

        val bottomNavigationItemView11 = onView(
            allOf(
                withId(R.id.navigation_settings), withContentDescription("Settings"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.bottomNavView),
                        0
                    ),
                    3
                ),
                isDisplayed()
            )
        )
        bottomNavigationItemView11.perform(click())

        val linearLayoutCompat4 = onView(
            allOf(
                withId(R.id.tutorial_button),
                childAtPosition(
                    childAtPosition(
                        withId(com.google.android.material.R.id.design_bottom_sheet),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        linearLayoutCompat4.perform(click())

        val materialButton5 = onView(
            allOf(
                withId(R.id.continueButton), withText("Next"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.tutorial_fragment),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        materialButton5.perform(click())

        val materialButton6 = onView(
            allOf(
                withId(R.id.startButton), withText("Next"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.third_tutorial_fragment),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        materialButton6.perform(click())

        val materialButton7 = onView(
            allOf(
                withId(R.id.continueButton), withText("Play"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.second_tutorial_fragment),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        materialButton7.perform(click())

        val bottomNavigationItemView12 = onView(
            allOf(
                withId(R.id.navigation_settings), withContentDescription("Settings"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.bottomNavView),
                        0
                    ),
                    3
                ),
                isDisplayed()
            )
        )
        bottomNavigationItemView12.perform(click())

        val linearLayoutCompat5 = onView(
            allOf(
                withId(R.id.rate_game_button),
                childAtPosition(
                    childAtPosition(
                        withId(com.google.android.material.R.id.design_bottom_sheet),
                        0
                    ),
                    3
                ),
                isDisplayed()
            )
        )
        linearLayoutCompat5.perform(click())

        val anyButton8 = onView(
            allOf(
                withId(R.id.share_button_settings),
                childAtPosition(
                    childAtPosition(
                        withId(com.google.android.material.R.id.design_bottom_sheet),
                        0
                    ),
                    4
                ),
                isDisplayed()
            )
        )
        anyButton8.perform(click())

        val anyButton9 = onView(
            allOf(
                withId(R.id.share_button_settings),
                childAtPosition(
                    childAtPosition(
                        withId(com.google.android.material.R.id.design_bottom_sheet),
                        0
                    ),
                    4
                ),
                isDisplayed()
            )
        )
        anyButton9.perform(click())

        val view7 = onView(
            allOf(
                withId(com.google.android.material.R.id.touch_outside),
                childAtPosition(
                    allOf(
                        withId(com.google.android.material.R.id.coordinator),
                        childAtPosition(
                            withId(R.id.container),
                            0
                        )
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        view7.perform(click())

        val bottomNavigationItemView13 = onView(
            allOf(
                withId(R.id.navigation_stages), withContentDescription("Levels"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.bottomNavView),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        bottomNavigationItemView13.perform(click())

        val anyButton10 = onView(
            allOf(
                withId(R.id.player_levels_button),
                childAtPosition(
                    childAtPosition(
                        withId(com.google.android.material.R.id.design_bottom_sheet),
                        0
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        anyButton10.perform(click())

        val view8 = onView(
            allOf(
                withId(com.google.android.material.R.id.touch_outside),
                childAtPosition(
                    allOf(
                        withId(com.google.android.material.R.id.coordinator),
                        childAtPosition(
                            withId(R.id.container),
                            0
                        )
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        view8.perform(click())
    }

    private fun childAtPosition(
        parentMatcher: Matcher<View>, position: Int
    ): Matcher<View> {

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
