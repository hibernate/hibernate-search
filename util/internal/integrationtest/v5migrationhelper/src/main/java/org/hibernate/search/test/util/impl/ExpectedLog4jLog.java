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
import java.util.List;
import java.util.Set;

import org.hibernate.search.util.impl.test.rule.log4j.Log4j2ConfigurationAccessor;

import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.hamcrest.TypeSafeMatcher;

/**
 * @author Yoann Rodiere
 */
public class ExpectedLog4jLog implements TestRule {

	private static final String DEFAULT_LOGGER_NAME = "org.hibernate.search";

	/**
	 * @return a {@linkplain TestRule rule} targeting the logger named '{@value DEFAULT_LOGGER_NAME}',
	 * that originally does not mandate any particular log to be produced (identical to behavior without this rule).
	 */
	public static ExpectedLog4jLog create() {
		return create( DEFAULT_LOGGER_NAME );
	}

	/**
	 * @return a {@linkplain TestRule rule} targeting the logger whose name is given by {@code loggerName},
	 * that originally does not mandate any particular log to be produced (identical to behavior without this rule).
	 */
	public static ExpectedLog4jLog create(String loggerName) {
		return new ExpectedLog4jLog( loggerName );
	}

	private final String loggerName;

	protected List<Matcher<?>> expectations = new ArrayList<>();

	protected List<Matcher<?>> absenceExpectations = new ArrayList<>();

	private ExpectedLog4jLog(String loggerName) {
		this.loggerName = loggerName;
	}

	@Override
	public Statement apply(Statement base, org.junit.runner.Description description) {
		return new ExpectedLogStatement( base );
	}

	/**
	 * Verify that your code produces a log event matching the given matcher.
	 */
	public void expectEvent(Matcher<? extends LogEvent> matcher) {
		expectations.add( matcher );
	}

	/**
	 * Verify that your code <strong>doesn't</strong> produce a log event matching the given matcher.
	 */
	public void expectEventMissing(Matcher<? extends LogEvent> matcher) {
		absenceExpectations.add( matcher );
	}

	/**
	 * Verify that your code <strong>doesn't</strong> produce a log event matching the given level or higher.
	 */
	public void expectLevelMissing(Level level) {
		expectEventMissing( new TypeSafeMatcher<LogEvent>() {
			@Override
			public void describeTo(Description description) {
				description.appendText( "a LoggingEvent with " ).appendValue( level ).appendText( " level or higher" );
			}
			@Override
			protected boolean matchesSafely(LogEvent item) {
				return item.getLevel().isMoreSpecificThan( level );
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

	private Matcher<LogEvent> eventMessageMatcher(final Matcher<String> messageMatcher) {
		return new TypeSafeMatcher<LogEvent>() {

			@Override
			public void describeTo(Description description) {
				description.appendText( "a LoggingEvent with message matching " );
				messageMatcher.describeTo( description );
			}

			@Override
			protected boolean matchesSafely(LogEvent item) {
				return messageMatcher.matches( item.getMessage() );
			}
		};
	}

	private class ExpectedLogStatement extends Statement {

		private final Statement next;

		public ExpectedLogStatement(Statement base) {
			next = base;
		}

		@Override
		public void evaluate() throws Throwable {
			Log4j2ConfigurationAccessor programmaticConfig = new Log4j2ConfigurationAccessor( loggerName );
			TestAppender appender = new TestAppender( "TestAppender", ExpectedLog4jLog.this );
			programmaticConfig.addAppender( appender );
			try {
				next.evaluate();
			}
			finally {
				programmaticConfig.removeAppender();
			}
			Set<Matcher<?>> expectationsNotMet = appender.getExpectationsNotMet();
			Set<LogEvent> unexpectedEvents = appender.getUnexpectedEvents();
			if ( !expectationsNotMet.isEmpty() || !unexpectedEvents.isEmpty() ) {
				fail( buildFailureMessage( expectationsNotMet, unexpectedEvents ) );
			}
		}
	}

	private static String buildFailureMessage(Set<Matcher<?>> missingSet, Set<LogEvent> unexpectedEvents) {
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
			for ( LogEvent unexpected : unexpectedEvents ) {
				description.appendText( "\n\t" );
				description.appendText( unexpected.getMessage().getFormattedMessage() );
			}
		}
		return description.toString();
	}

}
