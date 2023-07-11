/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.checkstyle.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;

public class ReportGenerator {

	public static void main(String[] args) throws IOException {
		generateReport(
				args[1],
				args[2],
				createIndex( args[0] ),
				createIgnoreRules( args )
		);
	}

	private static void generateReport(String outputPath, String annotationName, Index index, Set<Pattern> ignoreRules)
			throws IOException {
		List<AnnotationInstance> incubating = index.getAnnotations( DotName.createSimple( annotationName ) );

		try ( Writer writer = new OutputStreamWriter( new FileOutputStream( outputPath ), StandardCharsets.UTF_8 ) ) {
			writer.write( "@defaultMessage Do not use code marked with " + annotationName + " annotation" );
			writer.write( '\n' );
			for ( AnnotationInstance annotationInstance : incubating ) {
				AnnotationTarget target = annotationInstance.target();
				String path = determinePath( target );
				if ( path != null ) {
					matchAnyIgnoreRule( path, ignoreRules ).ifPresent( rule -> {
						try {
							writer.write( "# Ignoring the following line because of the `" + rule.pattern() + "` rule:\n# " );
						}
						catch (IOException e) {
							// just rethrow the exception as a runtime one:
							throw new RuntimeException( e );
						}
					} );

					writer.write( path );
					writer.write( '\n' );
				}
			}
		}
	}

	private static Optional<Pattern> matchAnyIgnoreRule(String path, Set<Pattern> rules) {
		for ( Pattern rule : rules ) {
			if ( rule.matcher( path ).matches() ) {
				return Optional.of( rule );
			}
		}
		return Optional.empty();
	}

	private static Set<Pattern> createIgnoreRules(String[] args) {
		if ( args.length > 3 ) {
			HashSet<Pattern> result = new HashSet<>();
			for ( int index = 3; index < args.length; index++ ) {
				result.add( Pattern.compile( args[index] ) );
			}
			return result;
		}
		else {
			return Collections.emptySet();
		}
	}

	private static Index createIndex(String sourcesPath) throws IOException {
		List<File> classFiles = new ArrayList<>();
		Files.walkFileTree( Path.of( sourcesPath ), new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
				if ( file.getFileName().toString().endsWith( "class" ) ) {
					classFiles.add( file.toFile() );
				}
				return FileVisitResult.CONTINUE;
			}
		} );

		return Index.of( classFiles.toArray( File[]::new ) );
	}

	private static String determinePath(AnnotationTarget usageLocation) {
		switch ( usageLocation.kind() ) {
			case CLASS: {
				final DotName name = usageLocation.asClass().name();
				if ( name.local().equals( "package-info" ) ) {
					return name.packagePrefix();
				}
				return name.toString();
			}
			case FIELD: {
				final FieldInfo fieldInfo = usageLocation.asField();
				return fieldInfo.declaringClass().name().toString()
						+ "#"
						+ fieldInfo.name();
			}
			case METHOD: {
				final MethodInfo methodInfo = usageLocation.asMethod();
				return methodInfo.declaringClass().name().toString()
						+ "#"
						+ methodInfo.name()
						+ parameters( methodInfo );
			}
			default: {
				return null;
			}
		}
	}

	private static String parameters(MethodInfo methodInfo) {
		return methodInfo.parameters().stream()
				.map( ReportGenerator::parameterTypeToString )
				.collect( Collectors.joining( ",", "(", ")" ) );
	}

	private static String parameterTypeToString(MethodParameterInfo parameter) {
		switch ( parameter.type().kind() ) {
			case CLASS:
			case PRIMITIVE:
			case VOID:
			case TYPE_VARIABLE:
			case UNRESOLVED_TYPE_VARIABLE:
			case WILDCARD_TYPE:
			case TYPE_VARIABLE_REFERENCE:
			case PARAMETERIZED_TYPE:
				return parameter.type().name().toString();
			case ARRAY:
				return parameter.type().asArrayType().component().name().toString() + "[]";
			default:
				throw new AssertionError( "Unknown parameter type: " + parameter.type().kind() );
		}
	}
}
