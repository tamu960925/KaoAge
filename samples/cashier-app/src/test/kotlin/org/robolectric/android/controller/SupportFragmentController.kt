package org.robolectric.android.controller

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import org.robolectric.Robolectric

class SupportFragmentController<T : Fragment> private constructor(
    private val fragment: T
) {

    private var activityController: ActivityController<FragmentActivity>? = null

    fun create(): SupportFragmentController<T> {
        val controller = Robolectric.buildActivity(FragmentActivity::class.java)
        activityController = controller
        controller.create()
        controller.get()
            .supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, fragment, "test-fragment")
            .commitNow()
        return this
    }

    fun start(): SupportFragmentController<T> {
        activityController?.start()
        return this
    }

    fun resume(): SupportFragmentController<T> {
        activityController?.resume()
        return this
    }

    fun visible(): SupportFragmentController<T> {
        activityController?.visible()
        return this
    }

    companion object {
        fun <T : Fragment> of(fragment: T): SupportFragmentController<T> =
            SupportFragmentController(fragment)
    }
}
