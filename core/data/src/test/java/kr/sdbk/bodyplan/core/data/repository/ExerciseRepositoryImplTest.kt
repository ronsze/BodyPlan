package kr.sdbk.bodyplan.core.data.repository

import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kr.sdbk.bodyplan.core.data.repository.fake.FakeExerciseDao
import kr.sdbk.bodyplan.core.domain.model.BodyPart
import kr.sdbk.bodyplan.core.domain.model.IntensityType
import kr.sdbk.bodyplan.core.local.entity.ExerciseEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseRepositoryImplTest {
    private val dao = FakeExerciseDao()
    private val repository = ExerciseRepositoryImpl(dao)

    @Test
    fun `삭제한 종목이 observeExercises 결과에 나오지 않는다`() = runTest {
        val id = dao.seed(
            ExerciseEntity(bodyPart = BodyPart.CHEST.name, name = "벤치프레스", intensityType = IntensityType.WEIGHT.name),
        )

        repository.deleteExercise(id)

        val result = repository.observeExercises(BodyPart.CHEST).first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `observeExercises는 삭제되지 않은 같은 부위 종목만 낸다`() = runTest {
        dao.seed(
            ExerciseEntity(bodyPart = BodyPart.CHEST.name, name = "벤치프레스", intensityType = IntensityType.WEIGHT.name),
        )
        dao.seed(
            ExerciseEntity(bodyPart = BodyPart.BACK.name, name = "랫풀다운", intensityType = IntensityType.WEIGHT.name),
        )

        val result = repository.observeExercises(BodyPart.CHEST).first()

        assertEquals(1, result.size)
        assertEquals("벤치프레스", result.first().name)
    }

    @Test
    fun `저장 실패는 예외로 전파된다`() = runTest {
        dao.insertFailure = IOException("insert failed")

        var thrown = false
        try {
            repository.addExercise(BodyPart.CHEST, "새 종목", IntensityType.WEIGHT)
        } catch (e: IOException) {
            thrown = true
        }
        assertTrue(thrown)
    }

    @Test
    fun `삭제 실패는 예외로 전파된다`() = runTest {
        val id = dao.seed(
            ExerciseEntity(bodyPart = BodyPart.CHEST.name, name = "벤치프레스", intensityType = IntensityType.WEIGHT.name),
        )
        dao.markDeletedFailure = IOException("delete failed")

        var thrown = false
        try {
            repository.deleteExercise(id)
        } catch (e: IOException) {
            thrown = true
        }
        assertTrue(thrown)
    }

    @Test
    fun `updateExercise는 이름이 바뀐 종목을 그대로 저장한다`() = runTest {
        val id = dao.seed(
            ExerciseEntity(bodyPart = BodyPart.CHEST.name, name = "벤치프레스", intensityType = IntensityType.WEIGHT.name),
        )
        val exercise = repository.getExercise(id)!!.copy(name = "인클라인 벤치프레스")

        repository.updateExercise(exercise)

        assertEquals("인클라인 벤치프레스", repository.getExercise(id)!!.name)
        assertFalse(repository.getExercise(id)!!.isDeleted)
    }
}
