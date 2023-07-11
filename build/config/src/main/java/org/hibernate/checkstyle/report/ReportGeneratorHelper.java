/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.checkstyle.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;

final class ReportGeneratorHelper {

	private static final String INDEX_FILE_NAME = "hibernate-search-report-index.idx";

	private ReportGeneratorHelper() {
	}

	static Index createIndex(String sourcesPath) throws IOException {
		Path indexPath = Path.of( sourcesPath ).resolve( INDEX_FILE_NAME );
		if ( Files.exists( indexPath ) ) {
			try ( InputStream input = new FileInputStream( indexPath.toFile() ) ) {
				return new IndexReader( input ).read();
			}
		}
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

		Index index = Index.of( classFiles.toArray( File[]::new ) );
		try ( OutputStream output = new FileOutputStream( indexPath.toFile() ) ) {
			new IndexWriter( output ).write( index );
		}
		return index;
	}

	static String determinePath(AnnotationTarget usageLocation) {
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
				.map( ReportGeneratorHelper::parameterTypeToString )
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
				return parameter.type().asArrayType().constituent().name().toString() + "[]";
			default:
				throw new AssertionError( "Unknown parameter type: " + parameter.type().kind() );
		}
	}

	static void writeReportLines(Writer writer, String path, Optional<Pattern> rule) throws IOException {
		rule.ifPresent( r -> {
			try {
				writer.write( "# Ignoring the following line because of the `" + r.pattern() + "` rule:\n# " );
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
