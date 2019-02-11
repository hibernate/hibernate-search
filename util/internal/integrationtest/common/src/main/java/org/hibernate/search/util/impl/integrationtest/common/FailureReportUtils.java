/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.search.util.EventContext;
import org.hibernate.search.util.EventContextElement;
import org.hibernate.search.util.SearchException;

import org.assertj.core.api.Assertions;

public final class FailureReportUtils {

	private FailureReportUtils() {
	}

	/**
	 * @param first The first part of the expected context.
	 * @param others The other parts of the expected context, if any. To be concatenated to the first part.
	 * @return A consumer representing an assertion to be passed as a parameter to
	 * {@link org.assertj.core.api.AbstractThrowableAssert#satisfies(Consumer)}.
	 */
	public static Consumer<Throwable> hasContext(EventContext first, EventContext... others) {
		return hasContext(
				EventContext.concat( first, others ).getElements().toArray( new EventContextElement[] { } )
		);
	}

	/**
	 * @param contextElements The expect context elements, in order.
	 * @return A consumer representing an assertion to be passed as a parameter to
	 * {@link org.assertj.core.api.AbstractThrowableAssert#satisfies(Consumer)}.
	 */
	public static Consumer<Throwable> hasContext(EventContextElement... contextElements) {
		return throwable -> {
			Assertions.assertThat( throwable )
					.isInstanceOf( SearchException.class );
			EventContext actualContext = ( (SearchException) throwable ).getContext();
			Assertions.assertThat( actualContext ).as( "throwable.getContext()" ).isNotNull();
			Assertions.assertThat( actualContext.getElements() )
					.containsExactly( contextElements );
			String renderedContextElements = Arrays.stream( contextElements ).map( EventContextElement::render )
					.collect( Collectors.joining( ", " ) );
			Assertions.assertThat( throwable.getMessage() )
					.endsWith( "Context: " + renderedContextElements );
		};
	}

	public static Consumer<Throwable> hasCauseWithContext(EventContext first, EventContext ... others) {
		Consumer<Throwable> delegate = hasContext( first, others );
		return e -> delegate.accept( e.getCause() );

	}

	public static FailureReportPatternBuilder buildFailureReportPattern() {
		return new FailureReportPatternBuilder();
	}

	/*
	 * Notes on meta-characters used here:
	 * - "\h" in a regex means "horizontal whitespace characters", i.e. spaces or tabs but not newline
	 * - "\Q" and "\E" in a regex allow to escape all the characters enclosed between them,
	 * which comes in handy to escape user-provided strings
	 * - "." does not match newline characters
	 * - "[\S\s]" matches any character, including newline characters
	 */
	public static class FailureReportPatternBuilder {
		private final StringBuilder patternBuilder = new StringBuilder();
		private boolean lastPatternWasFailure = false;

		private FailureReportPatternBuilder() {
		}

		public FailureReportPatternBuilder typeContext(String exactTypeName) {
			return contextLiteral( "type '" + exactTypeName + "'" );
		}

		public FailureReportPatternBuilder indexContext(String exactIndexName) {
			return contextLiteral( "index '" + exactIndexName + "'" );
		}

		public FailureReportPatternBuilder backendContext(String exactBackendName) {
			return contextLiteral( "backend '" + exactBackendName + "'" );
		}

		public FailureReportPatternBuilder pathContext(String pathPattern) {
			return contextLiteral( "path '" + pathPattern + "'" );
		}

		public FailureReportPatternBuilder annotationContextAnyParameters(Class<? extends Annotation> annotationType) {
			return contextPattern( "annotation '@\\Q" + annotationType.getName() + "\\E\\(.*'" );
		}

		public FailureReportPatternBuilder contextLiteral(String contextLiteral) {
			return contextPattern( "\\Q" + contextLiteral + "\\E" );
		}

		public FailureReportPatternBuilder contextPattern(String contextPattern) {
			lastPatternWasFailure = false;
			patternBuilder.append( "\n\\h+" )
					.append( contextPattern )
					.append( ": " );
			return this;
		}

		public FailureReportPatternBuilder failure(String ... literalStringsContainedInFailureMessageInOrder) {
			if ( !lastPatternWasFailure ) {
				patternBuilder.append( "\n\\h+failures: " );
			}
			lastPatternWasFailure = true;
			patternBuilder.append( "\n\\h+-\\h" );
			for ( String contained : literalStringsContainedInFailureMessageInOrder ) {
				patternBuilder.append( ".*" )
						.append( "\\Q" ).append( contained ).append( "\\E" );
			}
			patternBuilder.append( ".*" );
			return this;
		}

		public FailureReportPatternBuilder multilineFailure(String ... literalStringsContainedInFailureMessageInOrder) {
			if ( !lastPatternWasFailure ) {
				patternBuilder.append( "\n\\h+failures: " );
			}
			lastPatternWasFailure = true;
			patternBuilder.append( "\n\\h+-\\h" );
			for ( String contained : literalStringsContainedInFailureMessageInOrder ) {
				patternBuilder.append( "[\\S\\s]*" )
						.append( "\\Q" ).append( contained ).append( "\\E" );
			}
			patternBuilder.append( "[\\S\\s]*" );
			return this;
		}

		public String build() {
			/*
			 * Prepend and append "[\S\s]*" because we have to match against the entire failure report,
			 * so we must match any characters before and after what we're looking for.
			 */
			return "[\\S\\s]*"
					+ patternBuilder.toString()
					+ "[\\S\\s]*";
		}
	}

}
