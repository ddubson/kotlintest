package io.kotlintest

import io.kotlintest.core.TestCaseConfig
import io.kotlintest.core.TestContext
import io.kotlintest.core.fromSpecClass
import io.kotlintest.core.sourceRef
import org.junit.platform.commons.annotation.Testable
import java.lang.AutoCloseable
import java.util.*

@Testable
abstract class AbstractSpec : Spec {

  var acceptingTopLevelRegistration = true

  private val rootTestCases = mutableListOf<TestCase>()

  override fun testCases(): List<TestCase> = rootTestCases.toList()

  protected fun createTestCase(name: String, test: suspend TestContext.() -> Unit, config: TestCaseConfig, type: TestType) =
      TestCase(Description.fromSpecClass(this::class).append(name), this, test, sourceRef(), type, config)

  protected fun addTestCase(name: String, test: suspend TestContext.() -> Unit, config: TestCaseConfig, type: TestType) {
    if (rootTestCases.any { it.name == name })
      throw IllegalArgumentException("Cannot add test with duplicate name $name")
    if (!acceptingTopLevelRegistration)
      throw IllegalArgumentException("Cannot add nested test here. Please see documentation on testing styles for how to layout nested tests correctly")
    rootTestCases.add(createTestCase(name, test, config, type))
  }

  private val closeablesInReverseOrder = LinkedList<AutoCloseable>()

  /**
   * Registers a field for auto closing after all tests have run.
   */
  protected fun <T : AutoCloseable> autoClose(closeable: T): T {
    closeablesInReverseOrder.addFirst(closeable)
    return closeable
  }

  override fun closeResources() {
    closeablesInReverseOrder.forEach { it.close() }
  }

  /**
   * Config applied to each test case if not overridden per test case.
   */
  protected open val defaultTestCaseConfig: TestCaseConfig = TestCaseConfig()
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DisplayName(val name: String)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DoNotParallelize
