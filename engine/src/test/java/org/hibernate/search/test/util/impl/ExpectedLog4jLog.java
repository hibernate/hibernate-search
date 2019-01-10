/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util.impl;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.hamcrest.TypeSafeMatcher;
import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;

/**
 * @author Yoann Rodiere
 */
public class ExpectedLog4jLog implements TestRule {

	/**
	 * Returns a {@linkplain TestRule rule} that does not mandate any particular log to be produced (identical to
	 * behavior without this rule).
	 */
	public static ExpectedLog4jLog create() {
		return new ExpectedLog4jLog();
	}

	private List<LogExpectation> expectations = new ArrayList<>();
	private TestAppender currentAppender;

	private ExpectedLog4jLog() {
	}

	@Override
	public Statement apply(Statement base, org.junit.runner.Description description) {
		return new ExpectedLogStatement( base );
	}

	/**
	 * Expect a logging event matching the given matcher.
	 * <p>
	 * Defaults to expecting the event once or more.
	 */
	public LogExpectation expectEvent(Matcher<? extends LoggingEvent> matcher) {
		LogExpectation expectation = new LogExpectation( matcher );
		expectations.add( expectation );
		if ( currentAppender != null ) {
			currentAppender.addChecker( expectation.createChecker() );
		}
		return expectation;
	}

	/**
	 * Expect a logging event matching the given level or higher,
	 * <p>
	 * Defaults to expecting the event once or more.
	 */
	public void expectEvent(Level level,
			Matcher<? super Throwable> throwableMatcher,
			String containedString, String... otherContainedStrings) {
		expectEvent( CoreMatchers.allOf(
				eventLevelMatcher( level ),
				eventThrowableMatcher( throwableMatcher ),
				eventMessageMatcher( containsAllStrings( containedString, otherContainedStrings ) )
		) );
	}

	/**
	 * @deprecated Use {@code expectEvent( matcher ).never() }
	 */
	@Deprecated
	public void expectEventMissing(Matcher<? extends LoggingEvent> matcher) {
		expectEvent( matcher ).never();
	}

	/**
	 * Expect a logging event matching the given level or higher.
	 * <p>
	 * Defaults to expecting the event once or more.
	 */
	public LogExpectation expectLevel(Level level) {
		return expectEvent( eventLevelMatcher( level ) );
	}

	/**
	 * @deprecated Use {@code expectLevel( level ).never() }
	 */
	@Deprecated
	public void expectLevelMissing(Level level) {
		expectLevel( level ).never();
	}

	/**
	 * Expect a log message containing the given string.
	 * <p>
	 * Defaults to expecting the event once or more.
	 */
	public LogExpectation expectMessage(String containedString) {
		return expectMessage( CoreMatchers.containsString( containedString ) );
	}

	/**
	 * @deprecated Use {@code expectMessage( containedString ).never() }
	 */
	@Deprecated
	public void expectMessageMissing(String containedString) {
		expectMessage( containedString ).never();
	}

	/**
	 * Expect a log message containing all of the given string.
	 * <p>
	 * Defaults to expecting the event once or more.
	 */
	public LogExpectation expectMessage(String containedString, String... otherContainedStrings) {
		return expectMessage( containsAllStrings( containedString, otherContainedStrings ) );
	}

	/**
	 * @deprecated Use {@code expectMessage( matcher ).never() }
	 */
	@Deprecated
	public void expectMessageMissing(String containedString, String... otherContainedStrings) {
		expectMessage( containedString, otherContainedStrings ).times( 0 );
	}

	/**
	 * Expect a log message matches the given Hamcrest matcher.
	 * <p>
	 * Defaults to expecting the event once or more.
	 */
	public LogExpectation expectMessage(Matcher<String> matcher) {
		return expectEvent( eventMessageMatcher( matcher ) );
	}

	/**
	 * @deprecated Use {@code expectMessage( matcher ).never() }
	 */
	@Deprecated
	public void expectMessageMissing(Matcher<String> matcher) {
		expectMessage( matcher ).times( 0 );
	}

	private Matcher<String> containsAllStrings(String containedString, String... otherContainedStrings) {
		Collection<Matcher<? super String>> matchers = new ArrayList<>();
		matchers.add( CoreMatchers.containsString( containedString ) );
		for ( String otherContainedString : otherContainedStrings ) {
			matchers.add( CoreMatchers.containsString( otherContainedString ) );
		}
		return CoreMatchers.<String>allOf( matchers );
	}

	private Matcher<LoggingEvent> eventLevelMatcher(Level level) {
		return new TypeSafeMatcher<LoggingEvent>() {
			@Override
			public void describeTo(Description description) {
				description.appendText( "a LoggingEvent with " ).appendValue( level ).appendText( " level or higher" );
			}
			@Override
			protected boolean matchesSafely(LoggingEvent item) {
				return item.getLevel().isGreaterOrEqual( level );
			}
		};
	}

	private Matcher<LoggingEvent> eventThrowableMatcher(Matcher<? super Throwable> throwableMatcher) {
		return new TypeSafeMatcher<LoggingEvent>() {
			@Override
			public void describeTo(Description description) {
				description.appendText( "a LoggingEvent with throwable " ).appendValue( throwableMatcher );
			}
			@Override
			protected boolean matchesSafely(LoggingEvent item) {
				ThrowableInformation throwableInfo = item.getThrowableInformation();
				return throwableMatcher.matches( throwableInfo == null ? null : throwableInfo.getThrowable() );
			}
		};
	}

	private Matcher<LoggingEvent> eventMessageMatcher(final Matcher<String> messageMatcher) {
		return new TypeSafeMatcher<LoggingEvent>() {

			@Override
			public void describeTo(Description description) {
				description.appendText( "a LoggingEvent with message matching " );
				messageMatcher.describeTo( description );
			}

			@Override
			protected boolean matchesSafely(LoggingEvent item) {
				return messageMatcher.matches( item.getMessage() );
			}
		};
	}

	private class TestAppender extends AppenderSkeleton {
		private final List<LogChecker> checkers;

		private TestAppender() {
			this.checkers = new ArrayList<>();
		}

		void addChecker(LogChecker checker) {
			checkers.add( checker );
		}

		@Override
		public void close() {
			// Nothing to clean up
		}

		@Override
		public boolean requiresLayout() {
			return false;
		}

		@Override
		protected void append(LoggingEvent event) {
			for ( LogChecker checker : checkers ) {
				checker.process( event );
			}
		}

		Set<LogChecker> getFailingCheckers() {
			Set<LogChecker> failingCheckers = new HashSet<>();
			for ( LogChecker checker : checkers ) {
				if ( !checker.areExpectationsMet() ) {
					failingCheckers.add( checker );
				}
			}
			return failingCheckers;
		}
	}

	private class ExpectedLogStatement extends Statement {

		private final Statement next;

		ExpectedLogStatement(Statement base) {
			next = base;
		}

		@Override
		public void evaluate() throws Throwable {
			final Logger logger = Logger.getRootLogger();
			TestAppender appender = new TestAppender();
			for ( LogExpectation expectation : ExpectedLog4jLog.this.expectations ) {
				appender.addChecker( expectation.createChecker() );
			}
			ExpectedLog4jLog.this.currentAppender = appender;
			logger.addAppender( appender );
			try {
				next.evaluate();
			}
			finally {
				logger.removeAppender( appender );
			}
			Set<LogChecker> failingCheckers = appender.getFailingCheckers();
			if ( !failingCheckers.isEmpty() ) {
				fail( buildFailureMessage( failingCheckers ) );
			}
		}
	}

	private static String buildFailureMessage(Set<LogChecker> failingCheckers) {
		Description description = new StringDescription();
		description.appendText( "Produced logs did not meet the following expectations:\n" );
		for ( LogChecker failingChecker : failingCheckers ) {
			failingChecker.appendFailure( description, "\n\t" );
		}
		return description.toString();
	}

	public static class LogExpectation {
		private final Matcher<?> matcher;
		private Integer expectedCount;

		LogExpectation(Matcher<?> matcher) {
			this.matcher = matcher;
		}

		public void never() {
			times( 0 );
		}

		public void times(int expectedCount) {
			if ( this.expectedCount != null ) {
				throw new IllegalStateException( "Can only set log expectations once" );
			}
			this.expectedCount = expectedCount;
		}

		LogChecker createChecker() {
			return new LogChecker( this );
		}

		Matcher<?> getMatcher() {
			return matcher;
		}

		int getMinExpectedCount() {
			return expectedCount == null ? 1 : expectedCount;
		}

		Integer getMaxExpectedCount() {
			return expectedCount;
		}
	}

	public static class LogChecker {
		private final LogExpectation expectation;
		private int count = 0;
		private List<LoggingEvent> extraEvents;

		public LogChecker(LogExpectation expectation) {
			this.expectation = expectation;
		}

		void process(LoggingEvent event) {
			if ( expectation.getMaxExpectedCount() == null && expectation.getMinExpectedCount() <= count ) {
				// We don't care about events anymore, expectations are met and it won't change
				return;
			}
			if ( expectation.getMatcher().matches( event ) ) {
				++count;
			}
			if ( expectation.getMaxExpectedCount() != null && count > expectation.getMaxExpectedCount() ) {
				if ( extraEvents == null ) {
					extraEvents = new ArrayList<>();
				}
				extraEvents.add( event );
			}
		}

		boolean areExpectationsMet() {
			return expectation.getMinExpectedCount() <= count
					&& ( expectation.getMaxExpectedCount() == null || count <= expectation.getMaxExpectedCount() );
		}

		void appendFailure(Description description, String newline) {
			description.appendText( newline );
			if ( count < expectation.getMinExpectedCount() ) {
				description.appendText( "Expected at least " + expectation.getMinExpectedCount() + " time(s) " );
				expectation.getMatcher().describeTo( description );
				description.appendText( " but only got " + count + " such event(s)." );
			}
			if ( expectation.getMaxExpectedCount() != null && expectation.getMaxExpectedCount() < count ) {
				description.appendText( "Expected at most " + expectation.getMaxExpectedCount() + " time(s) " );
				expectation.getMatcher().describeTo( description );
				description.appendText( " but got " + count + " such event(s)." );
				description.appendText( " Extra events: " );
				for ( LoggingEvent extraEvent : extraEvents ) {
					description.appendText( newline );
					description.appendText( "\t - " );
					description.appendText( extraEvent.getRenderedMessage() );
				}
			}
		}


	}

}
