/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.reporting.EventContextElement;
import org.hibernate.search.util.common.SearchException;

public final class FailureReportUtils {

	private FailureReportUtils() {
	}

	/**
	 * @param first The first part of the expected context.
	 * @param others The other parts of the expected context, if any. To be concatenated to the first part.
	 * @return A consumer representing an assertion to be passed as a parameter to
	 * {@link org.assertj.core.api.AbstractThrowableAssert#satisfies(Consumer[])}.
	 */
	public static <T extends Throwable> Consumer<T> hasContext(EventContext first, EventContext... others) {
		return hasContext(
				EventContext.concat( first, others ).elements().toArray( new EventContextElement[] { } )
		);
	}

	/**
	 * @param contextElements The expect context elements, in order.
	 * @return A consumer representing an assertion to be passed as a parameter to
	 * {@link org.assertj.core.api.AbstractThrowableAssert#satisfies(Consumer[])}.
	 */
	public static <T extends Throwable> Consumer<T> hasContext(EventContextElement... contextElements) {
		return throwable -> {
			assertThat( throwable )
					.isInstanceOf( SearchException.class );
			EventContext actualContext = ( (SearchException) throwable ).context();
			assertThat( actualContext ).as( "throwable.getContext()" ).isNotNull();
			assertThat( actualContext.elements() )
					.containsExactly( contextElements );
			String renderedContextElements = Arrays.stream( contextElements ).map( EventContextElement::render )
					.collect( Collectors.joining( ", " ) );
			assertThat( throwable.getMessage() )
					.endsWith( "Context: " + renderedContextElements );
		};
	}

	public static Consumer<Throwable> hasCauseWithContext(EventContext first, EventContext ... others) {
		Consumer<Throwable> delegate = hasContext( first, others );
		return e -> delegate.accept( e.getCause() );

	}

	public static FailureReportChecker hasFailureReport() {
		return new FailureReportChecker();
	}

	/*
	 * Notes on meta-characters used here:
	 * - "\h" in a regex means "horizontal whitespace characters", i.e. spaces or tabs but not newline
	 * - "\Q" and "\E" in a regex allow to escape all the characters enclosed between them,
	 * which comes in handy to escape user-provided strings
	 * - "." does not match newline characters
	 * - "[\S\s]" matches any character, including newline characters
	 */
	public static class FailureReportChecker implements Consumer<Throwable> {
		private final StringBuilder patternBuilder = new StringBuilder();
		private boolean lastPatternWasFailure = false;

		private FailureReportChecker() {
		}

		public FailureReportChecker typeContext(String exactTypeName) {
			return contextLiteral( "type '" + exactTypeName + "'" );
		}

		public FailureReportChecker indexContext(String exactIndexName) {
			return contextLiteral( "index '" + exactIndexName + "'" );
		}

		public FailureReportChecker indexSchemaRootContext() {
			return contextLiteral( "index schema root" );
		}

		public FailureReportChecker defaultBackendContext() {
			return contextLiteral( "default backend" );
		}

		public FailureReportChecker backendContext(String exactBackendName) {
			return contextLiteral( "backend '" + exactBackendName + "'" );
		}

		public FailureReportChecker pathContext(String pathPattern) {
			return contextLiteral( "path '" + pathPattern + "'" );
		}

		public FailureReportChecker indexFieldContext(String exactPath) {
			return contextLiteral( "field '" + exactPath + "'" );
		}

		public FailureReportChecker mappingAttributeContext(String exactName) {
			return contextLiteral( "attribute '" + exactName + "'" );
		}

		public FailureReportChecker indexFieldTemplateContext(String exactPath) {
			return contextLiteral( "field template '" + exactPath + "'" );
		}

		public FailureReportChecker fieldTemplateAttributeContext(String exactPath) {
			return contextLiteral( "attribute '" + exactPath + "'" );
		}

		public FailureReportChecker analyzerContext(String exactName) {
			return contextLiteral( "analyzer '" + exactName + "'" );
		}

		public FailureReportChecker normalizerContext(String exactName) {
			return contextLiteral( "normalizer '" + exactName + "'" );
		}

		public FailureReportChecker charFilterContext(String exactName) {
			return contextLiteral( "char filter '" + exactName + "'" );
		}

		public FailureReportChecker tokenizerContext(String exactName) {
			return contextLiteral( "tokenizer '" + exactName + "'" );
		}

		public FailureReportChecker tokenFilterContext(String exactName) {
			return contextLiteral( "token filter '" + exactName + "'" );
		}

		public FailureReportChecker analysisDefinitionParameterContext(String exactName) {
			return contextLiteral( "parameter '" + exactName + "'" );
		}

		public FailureReportChecker aliasContext(String exactName) {
			return contextLiteral( "alias '" + exactName + "'" );
		}

		public FailureReportChecker aliasAttributeContext(String exactName) {
			return contextLiteral( "attribute '" + exactName + "'" );
		}

		public FailureReportChecker annotationContextAnyParameters(Class<? extends Annotation> annotationType) {
			return contextPattern( "annotation '@\\Q" + annotationType.getName() + "\\E\\(.*'" );
		}

		public FailureReportChecker annotationTypeContext(Class<? extends Annotation> annotationType) {
			return contextLiteral( "annotation type '@" + annotationType.getName() + "'" );
		}

		public FailureReportChecker contextLiteral(String contextLiteral) {
			return contextPattern( "\\Q" + contextLiteral + "\\E" );
		}

		public FailureReportChecker contextPattern(String contextPattern) {
			lastPatternWasFailure = false;
			patternBuilder.append( "\n\\h+" )
					.append( contextPattern )
					.append( ": " );
			return this;
		}

		public FailureReportChecker failure(String ... literalStringsContainedInFailureMessageInOrder) {
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

		public FailureReportChecker multilineFailure(String ... literalStringsContainedInFailureMessageInOrder) {
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

		@Override
		public void accept(Throwable throwable) {
			/*
			 * Prepend and append "[\S\s]*" because we have to match against the entire failure report,
			 * so we must match any characters before and after what we're looking for.
			 */
			String pattern = "[\\S\\s]*"
					+ patternBuilder
					+ "[\\S\\s]*";
			assertThat( throwable ).hasMessageMatching( pattern );
		}
	}

}
