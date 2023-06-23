/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test;

import static org.hamcrest.CoreMatchers.containsString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.hamcrest.TypeSafeMatcher;

public class ExceptionMatcherBuilder {

	/**
	 * @param clazz The expected type of the exception.
	 * @return A matcher builder.
	 */
	public static ExceptionMatcherBuilder isException(Class<? extends Throwable> clazz) {
		return new ExceptionMatcherBuilder( CoreMatchers.instanceOf( clazz ) );
	}

	/**
	 * @param throwable The expected exception.
	 * @return A matcher builder.
	 */
	public static ExceptionMatcherBuilder isException(Throwable throwable) {
		return new ExceptionMatcherBuilder( CoreMatchers.sameInstance( throwable ) );
	}

	private final List<Matcher<?>> matchers = new ArrayList<>();

	private final List<Matcher<?>> suppressedMatchers = new ArrayList<>();

	private ExceptionMatcherBuilder(Matcher<? extends Throwable> matcher) {
		matchers.add( matcher );
	}

	public ExceptionMatcherBuilder withMainOrSuppressed(Throwable throwable) {
		return withMainOrSuppressed( CoreMatchers.sameInstance( throwable ) );
	}

	public ExceptionMatcherBuilder withSuppressed(Throwable throwable) {
		return withSuppressed( CoreMatchers.sameInstance( throwable ) );
	}

	public ExceptionMatcherBuilder withMainOrSuppressed(Matcher<?> matcher) {
		return matching( mainOrSuppressed( matcher ) );
	}

	public ExceptionMatcherBuilder withSuppressed(Matcher<?> matcher) {
		suppressedMatchers.add( matcher );
		return this;
	}

	public ExceptionMatcherBuilder causedBy(Class<? extends Throwable> clazz) {
		return new NestedExceptionCauseMatcherBuilder( CoreMatchers.instanceOf( clazz ) );
	}

	public ExceptionMatcherBuilder causedBy(Throwable throwable) {
		return new NestedExceptionCauseMatcherBuilder( CoreMatchers.sameInstance( throwable ) );
	}

	public ExceptionMatcherBuilder rootCause(Class<? extends Throwable> clazz) {
		return new NestedExceptionRootCauseMatcherBuilder( CoreMatchers.instanceOf( clazz ) );
	}

	public ExceptionMatcherBuilder rootCause(Throwable throwable) {
		return new NestedExceptionRootCauseMatcherBuilder( CoreMatchers.sameInstance( throwable ) );
	}

	public Matcher<? super Throwable> build() {
		if ( !suppressedMatchers.isEmpty() ) {
			@SuppressWarnings("unchecked")
			Matcher<? super Throwable>[] suppressedMatchersAsArray =
					castedSuppressedMatchers().toArray( new Matcher[suppressedMatchers.size()] );
			ExceptionMatcherBuilder.this.matching( hasSuppressed( CoreMatchers.hasItems( suppressedMatchersAsArray ) ) );
			suppressedMatchers.clear();
		}
		if ( matchers.size() == 1 ) {
			return castedMatchers().get( 0 );
		}
		else {
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

	public ExceptionMatcherBuilder matching(final Matcher<?> throwableMatcher) {
		matchers.add( throwableMatcher );
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
		public NestedExceptionCauseMatcherBuilder(Matcher<? extends Throwable> matcher) {
			super( matcher );
		}

		@Override
		public Matcher<? super Throwable> build() {
			Matcher<? super Throwable> myMatcher = super.build();
			ExceptionMatcherBuilder.this.matching( hasCause( myMatcher ) );
			return ExceptionMatcherBuilder.this.build();
		}
	}

	private class NestedExceptionRootCauseMatcherBuilder extends ExceptionMatcherBuilder {
		public NestedExceptionRootCauseMatcherBuilder(Matcher<? extends Throwable> matcher) {
			super( matcher );
		}

		@Override
		public Matcher<? super Throwable> build() {
			Matcher<? super Throwable> myMatcher = super.build();
			ExceptionMatcherBuilder.this.matching( hasRootCause( myMatcher ) );
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

	public static class ThrowableMainOrSuppressedMatcher extends TypeSafeDiagnosingMatcher<Throwable> {

		private final Matcher<?> mainOrSuppressedMatcher;

		public ThrowableMainOrSuppressedMatcher(Matcher<?> suppressedMatcher) {
			this.mainOrSuppressedMatcher = suppressedMatcher;
		}

		@Override
		public void describeTo(Description description) {
			description.appendText( "exception (or one of its suppressed exceptions) " );
			description.appendDescriptionOf( mainOrSuppressedMatcher );
		}

		@Override
		protected boolean matchesSafely(Throwable item, Description mismatchDescription) {
			Throwable[] suppressedArray = item.getSuppressed();
			List<Throwable> mainAndSuppressed = new ArrayList<>();
			mainAndSuppressed.add( item );
			Collections.addAll( mainAndSuppressed, suppressedArray );

			boolean first = true;
			for ( Throwable element : mainAndSuppressed ) {
				if ( mainOrSuppressedMatcher.matches( element ) ) {
					return true;
				}
				if ( !first ) {
					mismatchDescription.appendText( ", " );
				}
				mainOrSuppressedMatcher.describeMismatch( element, mismatchDescription );
				first = false;
			}
			return false;
		}
	}

	private static Matcher<Throwable> mainOrSuppressed(Matcher<?> mainOrSuppressedMatcher) {
		return new ThrowableMainOrSuppressedMatcher( mainOrSuppressedMatcher );
	}

	public static class ThrowableRootCauseMatcher extends TypeSafeDiagnosingMatcher<Throwable> {

		private final Matcher<?> rootCauseMatcher;

		public ThrowableRootCauseMatcher(Matcher<?> suppressedMatcher) {
			this.rootCauseMatcher = suppressedMatcher;
		}

		@Override
		public void describeTo(Description description) {
			description.appendText( "root cause " );
			description.appendDescriptionOf( rootCauseMatcher );
		}

		@Override
		protected boolean matchesSafely(Throwable item, Description mismatchDescription) {
			while ( item.getCause() != null ) {
				item = item.getCause();
			}

			if ( rootCauseMatcher.matches( item ) ) {
				return true;
			}
			else {
				rootCauseMatcher.describeMismatch( item, mismatchDescription );
				return false;
			}
		}
	}

	private static Matcher<Throwable> hasRootCause(Matcher<?> rootCauseMatcher) {
		return new ThrowableRootCauseMatcher( rootCauseMatcher );
	}
}
