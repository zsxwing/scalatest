package org.scalatest.tools

import sbt.testing.{Status => SbtStatus, _}
import org.scalatest.Reporter

private class SbtReporter(suiteId: String, fullyQualifiedName: String, fingerprint: Fingerprint, eventHandler: EventHandler, report: Reporter) extends Reporter {

  import org.scalatest.events._

  private def getTestSelector(eventSuiteId: String, testName: String) = {
    if (suiteId == eventSuiteId)
      new TestSelector(testName)
    else
      new NestedTestSelector(eventSuiteId, testName)
  }

  private def getSuiteSelector(eventSuiteId: String) = {
    if (suiteId == eventSuiteId)
      new SuiteSelector
    else
      new NestedSuiteSelector(eventSuiteId)
  }

  private def getOptionalThrowable(throwable: Option[Throwable]): OptionalThrowable =
    throwable match {
      case Some(t) => new OptionalThrowable(t)
      case None => new OptionalThrowable
    }

  override def apply(event: Event) {
    report(event)
    event match {
      // the results of running an actual test
      case t: TestPending =>
        eventHandler.handle(ScalaTestSbtEvent(fullyQualifiedName, fingerprint, getTestSelector(t.suiteId, t.testName), SbtStatus.Pending, new OptionalThrowable, t.duration.getOrElse(0)))
      case t: TestFailed =>
        eventHandler.handle(ScalaTestSbtEvent(fullyQualifiedName, fingerprint, getTestSelector(t.suiteId, t.testName), SbtStatus.Failure, getOptionalThrowable(t.throwable), t.duration.getOrElse(0)))
      case t: TestSucceeded =>
        eventHandler.handle(ScalaTestSbtEvent(fullyQualifiedName, fingerprint, getTestSelector(t.suiteId, t.testName), SbtStatus.Success, new OptionalThrowable, t.duration.getOrElse(0)))
      case t: TestIgnored =>
        eventHandler.handle(ScalaTestSbtEvent(fullyQualifiedName, fingerprint, getTestSelector(t.suiteId, t.testName), SbtStatus.Ignored, new OptionalThrowable, -1))
      case t: TestCanceled =>
        eventHandler.handle(ScalaTestSbtEvent(fullyQualifiedName, fingerprint, getTestSelector(t.suiteId, t.testName), SbtStatus.Canceled, new OptionalThrowable, t.duration.getOrElse(0)))
      case t: SuiteAborted =>
        eventHandler.handle(ScalaTestSbtEvent(fullyQualifiedName, fingerprint, getSuiteSelector(t.suiteId), SbtStatus.Error, getOptionalThrowable(t.throwable), t.duration.getOrElse(0)))
      case _ =>
    }
  }
}