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

	private List<Matcher<?>> expectations = new ArrayList<>();

	private List<Matcher<?>> absenceExpectations = new ArrayList<>();

	private ExpectedLog4jLog() {
	}

	@Override
	public Statement apply(Statement base, org.junit.runner.Description description) {
		return new ExpectedLogStatement( base );
	}

	/**
	 * Verify that your code produces a log event matching the given matcher.
	 */
	public void expectEvent(Matcher<? extends LoggingEvent> matcher) {
		expectations.add( matcher );
	}

	/**
	 * Verify that your code <strong>doesn't</strong> produce a log event matching the given matcher.
	 */
	public void expectEventMissing(Matcher<? extends LoggingEvent> matcher) {
		absenceExpectations.add( matcher );
	}

	/**
	 * Verify that your code <strong>doesn't</strong> produce a log event matching the given level or higher.
	 */
	public void expectLevelMissing(Level level) {
		expectEventMissing( new TypeSafeMatcher<LoggingEvent>() {
			@Override
			public void describeTo(Description description) {
				description.appendText( "a LoggingEvent with " ).appendValue( level ).appendText( " level or higher" );
			}
			@Override
			protected boolean matchesSafely(LoggingEvent item) {
				return item.getLevel().isGreaterOrEqual( level );
			}
		} );
	}

	/**
	 * Verify that your code produces a log message containing the given string.
	 */
	public void expectMessage(String containedString) {
		expectMessage( CoreMatchers.containsString( containedString ) );
	}

	/**
	 * Verify that your code <strong>doesn't</strong> produce a log message containing the given string.
	 */
	public void expectMessageMissing(String containedString) {
		expectMessageMissing( CoreMatchers.containsString( containedString ) );
	}

	/**
	 * Verify that your code produces a log message containing all of the given string.
	 */
	public void expectMessage(String containedString, String... otherContainedStrings) {
		expectMessage( containsAllStrings( containedString, otherContainedStrings ) );
	}

	/**
	 * Verify that your code <strong>doesn't</strong> produce a log message containing all of the given string.
	 */
	public void expectMessageMissing(String containedString, String... otherContainedStrings) {
		expectMessageMissing( containsAllStrings( containedString, otherContainedStrings ) );
	}

	/**
	 * Verify that your code produces a log message matches the given Hamcrest matcher.
	 */
	public void expectMessage(Matcher<String> matcher) {
		expectEvent( eventMessageMatcher( matcher ) );
	}

	/**
	 * Verify that your code <strong>doesn't</strong> produce a log message matches the given Hamcrest matcher.
	 */
	public void expectMessageMissing(Matcher<String> matcher) {
		expectEventMissing( eventMessageMatcher( matcher ) );
	}

	private Matcher<String> containsAllStrings(String containedString, String... otherContainedStrings) {
		Collection<Matcher<? super String>> matchers = new ArrayList<>();
		matchers.add( CoreMatchers.containsString( containedString ) );
		for ( String otherContainedString : otherContainedStrings ) {
			matchers.add( CoreMatchers.containsString( otherContainedString ) );
		}
		return CoreMatchers.<String>allOf( matchers );
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
		private final Set<Matcher<?>> expectationsMet = new HashSet<>();
		private final Set<LoggingEvent> unexpectedEvents = new HashSet<>();

		@Override
		public void close() {
		}

		@Override
		public boolean requiresLayout() {
			return false;
		}

		@Override
		protected void append(LoggingEvent event) {
			for ( Matcher<?> expectation : ExpectedLog4jLog.this.expectations ) {
				if ( !expectationsMet.contains( expectation ) && expectation.matches( event ) ) {
					expectationsMet.add( expectation );
				}
			}
			for ( Matcher<?> absenceExpectation : ExpectedLog4jLog.this.absenceExpectations ) {
				if ( absenceExpectation.matches( event ) ) {
					unexpectedEvents.add( event );
				}
			}
		}

		public Set<Matcher<?>> getExpectationsNotMet() {
			Set<Matcher<?>> expectationsNotMet = new HashSet<>();
			expectationsNotMet.addAll( expectations );
			expectationsNotMet.removeAll( expectationsMet );
			return expectationsNotMet;
		}

		public Set<LoggingEvent> getUnexpectedEvents() {
			return unexpectedEvents;
		}

	}

	private class ExpectedLogStatement extends Statement {

		private final Statement next;

		public ExpectedLogStatement(Statement base) {
			next = base;
		}

		@Override
		public void evaluate() throws Throwable {
			final Logger logger = Logger.getRootLogger();
			TestAppender appender = new TestAppender();
			logger.addAppender( appender );
			try {
				next.evaluate();
			}
			finally {
				logger.removeAppender( appender );
			}
			Set<Matcher<?>> expectationsNotMet = appender.getExpectationsNotMet();
			Set<LoggingEvent> unexpectedEvents = appender.getUnexpectedEvents();
			if ( !expectationsNotMet.isEmpty() || !unexpectedEvents.isEmpty() ) {
				fail( buildFailureMessage( expectationsNotMet, unexpectedEvents ) );
			}
		}
	}

	private static String buildFailureMessage(Set<Matcher<?>> missingSet, Set<LoggingEvent> unexpectedEvents) {
		Description description = new StringDescription();
		description.appendText( "Produced logs did not meet the expectations." );
		if ( !missingSet.isEmpty() ) {
			description.appendText( "\nMissing logs:" );
			for ( Matcher<?> missing : missingSet ) {
				description.appendText( "\n\t" );
				missing.describeTo( description );
			}
		}
		if ( !unexpectedEvents.isEmpty() ) {
			description.appendText( "\nUnexpected logs:" );
			for ( LoggingEvent unexpected : unexpectedEvents ) {
				description.appendText( "\n\t" );
				description.appendText( unexpected.getRenderedMessage() );
			}
		}
		return description.toString();
	}

}
