/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.reporting;

import static org.assertj.core.api.Assertions.fail;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.search.util.common.logging.impl.CommaSeparatedClassesFormatter;

/*
 * Notes on meta-characters used here:
 * - "\h" in a regex means "horizontal whitespace characters", i.e. spaces or tabs but not newline
 * - "\Q" and "\E" in a regex allow to escape all the characters enclosed between them,
 * which comes in handy to escape user-provided strings
 * - "." does not match newline characters
 * - "[\S\s]" matches any character, including newline characters
 */
public class FailureReportChecker implements Consumer<Throwable> {
	private final List<ElementToMatch> elementsToMatch = new ArrayList<>();
	private boolean lastPatternWasFailure = false;

	FailureReportChecker() {
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

	public FailureReportChecker constructorContext(Class<?>... parameterTypes) {
		return contextLiteral(
				"constructor with parameter types [" + CommaSeparatedClassesFormatter.format( parameterTypes ) + "]" );
	}

	public FailureReportChecker projectionConstructorContext() {
		return contextLiteral( "projection constructor" );
	}

	public FailureReportChecker methodParameterContext(int index) {
		return methodParameterContext( index, "<unknown name>" );
	}

	public FailureReportChecker methodParameterContext(int index, String name) {
		return contextLiteral( "parameter at index " + index + " (" + name + ")" );
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

	public FailureReportChecker indexSettingsCustomAttributeContext(String exactName) {
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
		elementsToMatch.add( new ElementToMatch( "\\n\\h+" + contextPattern + ": " ) );
		return this;
	}

	public FailureReportChecker failure(String... literalStringsContainedInFailureMessageInOrder) {
		if ( !lastPatternWasFailure ) {
			elementsToMatch.add( new ElementToMatch( "\\n\\h+failures: " ) );
		}
		lastPatternWasFailure = true;
		elementsToMatch.add( new ElementToMatch( "\\n\\h+-\\h" ) );
		for ( String contained : literalStringsContainedInFailureMessageInOrder ) {
			elementsToMatch.add( new ElementToMatch( ".*" + "\\Q" + contained + "\\E" ) );
		}
		// Consume the rest of the line
		elementsToMatch.add( new ElementToMatch( ".*" ) );
		return this;
	}

	public FailureReportChecker multilineFailure(String... literalStringsContainedInFailureMessageInOrder) {
		if ( !lastPatternWasFailure ) {
			elementsToMatch.add( new ElementToMatch( "\\n\\h+failures: " ) );
		}
		lastPatternWasFailure = true;
		elementsToMatch.add( new ElementToMatch( "\\n\\h+-\\h" ) );
		for ( String contained : literalStringsContainedInFailureMessageInOrder ) {
			String[] lines = contained.split( "\n" );
			for ( int i = 0; i < lines.length; i++ ) {
				if ( i == 0 ) {
					elementsToMatch.add( new ElementToMatch( ".*(\\n\\h+.*)?" + "\\Q" + lines[i] + "\\E" ) );
				}
				else {
					elementsToMatch.add( new ElementToMatch( "\\n\\h+" + "\\Q" + lines[i] + "\\E" ) );
				}
			}
		}
		// Match the rest of the line
		// We can't match multiple lines here, or we would run the risk of
		// matching text meant for the following elements to match
		elementsToMatch.add( new ElementToMatch( ".*" ) );
		return this;
	}

	private static class ElementToMatch {

		private final Pattern pattern;

		ElementToMatch(String patternString) {
			this.pattern = Pattern.compile( patternString );
		}

		int consumeFirst(String fullMessage) {
			Matcher matcher = pattern.matcher( fullMessage );
			if ( matcher.find() ) {
				return matcher.end();
			}
			else {
				return fail(
						"Expected to find substring matching the following pattern:"
								+ "\n\t%s"
								+ "\n\nbut did not."
								+ "\n\nFull actual message:\n\t%s",
						pattern.pattern(),
						fullMessage
				);
			}
		}

		int consumeNext(String fullMessage, int currentIndex) {
			Matcher matcher = pattern.matcher( fullMessage );
			if ( matcher.find( currentIndex ) && matcher.start() == currentIndex ) {
				return matcher.end();
			}
			else {
				return fail(
						"After:\n\t[...]%s"
								+ "\n\nExpected to find substring matching the following pattern:"
								+ "\n\t%s"
								+ "\n\nbut found this instead:\n\t%s[..]"
								+ "\n\nFull actual message:\n\t%s",
						fullMessage.substring( Math.max( 0, currentIndex - 200 ), currentIndex ),
						pattern.pattern(),
						fullMessage.substring( currentIndex,
								Math.min( fullMessage.length(), currentIndex + 100 + pattern.pattern().length() ) ),
						fullMessage
				);
			}
		}

	}

	@Override
	public void accept(Throwable throwable) {
		String message = throwable.getMessage();
		if ( elementsToMatch.isEmpty() ) {
			throw new IllegalStateException( "Must add at least one element to match" );
		}
		int currentIndex = elementsToMatch.get( 0 ).consumeFirst( message );
		for ( int i = 1; i < elementsToMatch.size(); i++ ) {
			currentIndex = elementsToMatch.get( i ).consumeNext( message, currentIndex );
		}
	}
}
