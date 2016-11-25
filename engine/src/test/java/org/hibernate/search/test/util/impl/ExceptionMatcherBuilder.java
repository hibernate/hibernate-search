/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util.impl;

import static org.hamcrest.CoreMatchers.containsString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * @author Yoann Rodiere
 */
public class ExceptionMatcherBuilder {

	public static ExceptionMatcherBuilder isException(Class<? extends Throwable> clazz) {
		return new ExceptionMatcherBuilder( clazz );
	}

	private final List<Matcher<?>> matchers = new ArrayList<>();

	private final List<Matcher<?>> suppressedMatchers = new ArrayList<>();

	private ExceptionMatcherBuilder(Class<? extends Throwable> clazz) {
		matchers.add( CoreMatchers.<Throwable>instanceOf( clazz ) );
	}

	public ExceptionMatcherBuilder withSuppressed(Matcher<?> matcher) {
		suppressedMatchers.add( matcher );
		return this;
	}

	public ExceptionMatcherBuilder causedBy(Class<? extends Throwable> clazz) {
		return new NestedExceptionCauseMatcherBuilder( clazz );
	}

	public Matcher<? super Throwable> build() {
		if ( matchers.size() == 1 && suppressedMatchers.isEmpty() ) {
			return castedMatchers().get( 0 );
		}
		else {
			if ( !suppressedMatchers.isEmpty() ) {
				@SuppressWarnings("unchecked")
				Matcher<? super Throwable>[] suppressedMatchersAsArray = castedSuppressedMatchers().toArray( new Matcher[suppressedMatchers.size()] );
				ExceptionMatcherBuilder.this.matching( hasSuppressed( CoreMatchers.hasItems( suppressedMatchersAsArray ) ) );
				suppressedMatchers.clear();
			}
			return CoreMatchers.allOf( castedMatchers() );
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" }) // Same hack as in JUnit's internal ExpectedExceptionMatcherBuilder
	private List<Matcher<? super Throwable>> castedMatchers() {
		return new ArrayList<Matcher<? super Throwable>>( (List) matchers );
	}

	@SuppressWarnings({ "unchecked", "rawtypes" }) // Same hack as in JUnit's internal ExpectedExceptionMatcherBuilder
	private List<Matcher<? super Throwable>> castedSuppressedMatchers() {
		return new ArrayList<Matcher<? super Throwable>>( (List) suppressedMatchers );
	}

	public ExceptionMatcherBuilder matching(final Matcher<?> messageMatcher) {
		matchers.add( messageMatcher );
		return this;
	}

	public ExceptionMatcherBuilder withMessage(String contained) {
		return withMessage( containsString( contained ) );
	}

	public ExceptionMatcherBuilder withMessage(final Matcher<String> messageMatcher) {
		return matching( hasMessage( messageMatcher ) );
	}

	/**
	 * @author Yoann Rodiere
	 */
	private class NestedExceptionCauseMatcherBuilder extends ExceptionMatcherBuilder {
		public NestedExceptionCauseMatcherBuilder(Class<? extends Throwable> clazz) {
			super( clazz );
		}

		@Override
		public Matcher<? super Throwable> build() {
			Matcher<? super Throwable> myMatcher = super.build();
			ExceptionMatcherBuilder.this.matching( hasCause( myMatcher ) );
			return ExceptionMatcherBuilder.this.build();
		}
	}

	/**
	 * Copied from the internal class from JUnit 4.11, itself licensed under EPL.
	 * Altered to fix the uselessly precise generic type parameter.
	 */
	public static class ThrowableMessageMatcher extends TypeSafeMatcher<Throwable> {

		private final Matcher<String> fMatcher;

		public ThrowableMessageMatcher(Matcher<String> matcher) {
			fMatcher = matcher;
		}

		@Override
		public void describeTo(Description description) {
			description.appendText( "exception with message " );
			description.appendDescriptionOf( fMatcher );
		}

		@Override
		protected boolean matchesSafely(Throwable item) {
			return fMatcher.matches( item.getMessage() );
		}

		@Override
		protected void describeMismatchSafely(Throwable item, Description description) {
			description.appendText( "message " );
			fMatcher.describeMismatch( item.getMessage(), description );
		}
	}

	private static Matcher<Throwable> hasMessage(final Matcher<String> matcher) {
		return new ThrowableMessageMatcher( matcher );
	}

	/**
	 * Copied from the internal class from JUnit 4.11, itself licensed under EPL.
	 * Altered to fix the incorrect generic type parameter.
	 */
	public static class ThrowableCauseMatcher extends TypeSafeMatcher<Throwable> {

		private final Matcher<?> causeMatcher;

		public ThrowableCauseMatcher(Matcher<?> causeMatcher) {
			this.causeMatcher = causeMatcher;
		}

		@Override
		public void describeTo(Description description) {
			description.appendText( "exception with cause " );
			description.appendDescriptionOf( causeMatcher );
		}

		@Override
		protected boolean matchesSafely(Throwable item) {
			return causeMatcher.matches( item.getCause() );
		}

		@Override
		protected void describeMismatchSafely(Throwable item, Description description) {
			description.appendText( "cause " );
			causeMatcher.describeMismatch( item.getCause(), description );
		}
	}

	private static Matcher<Throwable> hasCause(final Matcher<?> matcher) {
		return new ThrowableCauseMatcher( matcher );
	}

	public static class ThrowableSuppressedMatcher extends TypeSafeMatcher<Throwable> {

		private final Matcher<?> suppressedMatcher;

		public ThrowableSuppressedMatcher(Matcher<?> suppressedMatcher) {
			this.suppressedMatcher = suppressedMatcher;
		}

		@Override
		public void describeTo(Description description) {
			description.appendText( "exception with suppressed " );
			description.appendDescriptionOf( suppressedMatcher );
		}

		@Override
		protected boolean matchesSafely(Throwable item) {
			return suppressedMatcher.matches( Arrays.asList( item.getSuppressed() ) );
		}

		@Override
		protected void describeMismatchSafely(Throwable item, Description description) {
			description.appendText( "suppressed " );
			suppressedMatcher.describeMismatch( Arrays.asList( item.getSuppressed() ), description );
		}
	}

	private static Matcher<Throwable> hasSuppressed(Matcher<?> suppressedMatcher) {
		return new ThrowableSuppressedMatcher( suppressedMatcher );
	}
}
