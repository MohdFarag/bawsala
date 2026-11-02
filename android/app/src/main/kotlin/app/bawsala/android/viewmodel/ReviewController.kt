package app.bawsala.android.viewmodel

import app.bawsala.android.ui.review.reviewWeekStart
import app.bawsala.core.model.WeeklyReview
import kotlinx.coroutines.launch

/** This week's review (V3 M06): steps finished and the week's outcomes. */
class ReviewController(private val ctx: ControllerScope) {
    private fun blank() = WeeklyReview(reviewWeekStart(ctx.today()))

    /** Marks steps up to [step] (1 to 3) as finished; never goes back. */
    fun finishStep(step: Int) =
        ctx.scope.launch {
            ctx.repository.updateReview(blank()) {
                it.copy(stepsDone = maxOf(it.stepsDone, step.coerceIn(0, 3)))
            }
        }

    /** Saves the week's direction and completes the review. */
    fun complete(outcomes: List<String>) =
        ctx.scope.launch {
            ctx.repository.updateReview(blank()) {
                it.copy(
                    stepsDone = 3,
                    outcomes = outcomes.map(String::trim).filter(String::isNotEmpty).take(3),
                    completedAt = it.completedAt ?: ctx.now(),
                )
            }
            ctx.messages.show("reviewDone")
        }
}
