/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.extension;

import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.hibernate.search.util.impl.test.extension.log4j.Log4j2ConfigurationAccessor;
import org.hibernate.search.util.impl.test.extension.log4j.LogChecker;
import org.hibernate.search.util.impl.test.extension.log4j.LogExpectation;
import org.hibernate.search.util.impl.test.extension.log4j.TestAppender;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.hamcrest.TypeSafeMatcher;

public class ExpectedLog4jLog implements BeforeEachCallback, AfterEachCallback {

	protected static final String DEFAULT_LOGGER_NAME = "org.hibernate.search";

	/**
	 * @return an {@linkplain Extension extension} targeting the logger named '{@value DEFAULT_LOGGER_NAME}',
	 * that originally does not mandate any particular log to be produced (identical to behavior without this rule).
	 */
	public static ExpectedLog4jLog create() {
		return create( DEFAULT_LOGGER_NAME );
	}

	/**
	 * @return a {@linkplain Extension extension} targeting the logger whose name is given by {@code loggerName},
	 * that originally does not mandate any particular log to be produced (identical to behavior without this rule).
	 */
	public static ExpectedLog4jLog create(String loggerName) {
		return new ExpectedLog4jLog( loggerName );
	}

	private final String loggerName;

	private final List<LogExpectation> expectations = new ArrayList<>();
	private TestAppender currentAppender;
	private Log4j2ConfigurationAccessor programmaticConfig;

	protected ExpectedLog4jLog(String loggerName) {
		this.loggerName = loggerName;
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		ExpectedLog4jLog.this.expectations.clear();
		programmaticConfig = new Log4j2ConfigurationAccessor( loggerName );
		TestAppender appender = new TestAppender( "TestAppender" );
		programmaticConfig.addAppender( appender );
		ExpectedLog4jLog.this.currentAppender = appender;
	}

	@Override
	public void afterEach(ExtensionContext context) {
		programmaticConfig.removeAppender();
		Set<LogChecker> failingCheckers = currentAppender.getFailingCheckers();
		if ( !failingCheckers.isEmpty() ) {
			fail( buildFailureMessage( failingCheckers ) );
		}
	}

	/**
	 * Expect a logging event matching the given matcher.
	 * <p>
	 * Defaults to expecting the event once or more.
	 */
	public LogExpectation expectEvent(Matcher<? extends LogEvent> matcher) {
		LogExpectation expectation = new LogExpectation( matcher );
		expectations.add( expectation );
		if ( currentAppender != null ) {
			currentAppender.addChecker( expectation.createChecker() );
		}
		return expectation;
	}

	/**
	 * Expect a logging event matching the given level or higher,
	 * and with a message containing the given strings.
	 * <p>
	 * Defaults to expecting the event once or more.
	 */
	public LogExpectation expectEvent(Level level,
			String containedString, String... otherContainedStrings) {
		return expectEvent( CoreMatchers.allOf(
				eventLevelMatcher( level ),
				eventMessageMatcher( containsAllStrings( containedString, otherContainedStrings ) )
		) );
	}

	/**
	 * Expect a logging event matching the given level or higher,
	 * with a throwable matching the given matcher.
	 * <p>
	 * Defaults to expecting the event once or more.
	 */
	public LogExpectation expectEvent(Level level,
			Matcher<? super Throwable> throwableMatcher) {
		return expectEvent( CoreMatchers.allOf(
				eventLevelMatcher( level ),
				eventThrowableMatcher( throwableMatcher )
		) );
	}

	/**
	 * Expect a logging event matching the given level or higher,
	 * with a throwable matching the given matcher,
	 * and with a message containing the given strings.
	 * <p>
	 * Defaults to expecting the event once or more.
	 */
	public LogExpectation expectEvent(Level level,
			Matcher<? super Throwable> throwableMatcher,
			String containedString, String... otherContainedStrings) {
		return expectEvent( CoreMatchers.allOf(
				eventLevelMatcher( level ),
				eventThrowableMatcher( throwableMatcher ),
				eventMessageMatcher( containsAllStrings( containedString, otherContainedStrings ) )
		) );
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
	 * Expect a log message containing the given string.
	 * <p>
	 * Defaults to expecting the event once or more.
	 */
	public LogExpectation expectMessage(String containedString) {
		return expectMessage( CoreMatchers.containsString( containedString ) );
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
	 * Expect a log message matches the given Hamcrest matcher.
	 * <p>
	 * Defaults to expecting the event once or more.
	 */
	public LogExpectation expectMessage(Matcher<String> matcher) {
		return expectEvent( eventMessageMatcher( matcher ) );
	}

	private Matcher<String> containsAllStrings(String containedString, String... otherContainedStrings) {
		Collection<Matcher<? super String>> matchers = new ArrayList<>();
		matchers.add( CoreMatchers.containsString( containedString ) );
		for ( String otherContainedString : otherContainedStrings ) {
			matchers.add( CoreMatchers.containsString( otherContainedString ) );
		}
		return CoreMatchers.allOf( matchers );
	}

	private Matcher<LogEvent> eventLevelMatcher(Level level) {
		return new TypeSafeMatcher<LogEvent>() {
			@Override
			public void describeTo(Description description) {
				description.appendText( "a LogEvent with " ).appendValue( level ).appendText( " level or higher" );
			}

			@Override
			protected boolean matchesSafely(LogEvent item) {
				return item.getLevel().isMoreSpecificThan( level );
			}
		};
	}

	private Matcher<LogEvent> eventThrowableMatcher(Matcher<? super Throwable> throwableMatcher) {
		return new TypeSafeMatcher<LogEvent>() {
			@Override
			public void describeTo(Description description) {
				description.appendText( "a LogEvent with throwable " ).appendValue( throwableMatcher );
			}

			@Override
			protected boolean matchesSafely(LogEvent item) {
				Throwable throwable = item.getThrown();
				return throwableMatcher.matches( throwable == null ? null : throwable );
			}
		};
	}

	private Matcher<LogEvent> eventMessageMatcher(final Matcher<String> messageMatcher) {
		return new TypeSafeMatcher<LogEvent>() {

			@Override
			public void describeTo(Description description) {
				description.appendText( "a LogEvent with message matching " );
				messageMatcher.describeTo( description );
			}

			@Override
			protected boolean matchesSafely(LogEvent item) {
				return messageMatcher.matches( item.getMessage().getFormattedMessage() );
			}
		};
	}

	private static String buildFailureMessage(Set<LogChecker> failingCheckers) {
		Description description = new StringDescription();
		description.appendText( "Produced logs did not meet the following expectations:\n" );
		for ( LogChecker failingChecker : failingCheckers ) {
			failingChecker.appendFailure( description, "\n\t" );
		}
		return description.toString();
	}
}
