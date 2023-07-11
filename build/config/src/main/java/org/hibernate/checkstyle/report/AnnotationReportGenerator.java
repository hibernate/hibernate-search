/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.checkstyle.report;

import static org.hibernate.checkstyle.report.ReportGeneratorHelper.createIndex;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

public class AnnotationReportGenerator {

	public static void main(String[] args) throws IOException {
		generateReport(
				args[1],
				args[2],
				args[3],
				createIndex( args[0] ),
				new ReportGeneratorRules( 4, args )
		);
	}

	private static void generateReport(
			String outputPath,
			String reportName,
			String annotationName,
			Index index,
			ReportGeneratorRules ignoreRules)
			throws IOException {
		List<AnnotationInstance> incubating = index.getAnnotations( DotName.createSimple( annotationName ) );

		try ( Writer writer = new OutputStreamWriter(
				new FileOutputStream( Path.of( outputPath ).resolve( reportName + ".txt" ).toFile() ),
				StandardCharsets.UTF_8 );
				Writer writerInternal = new OutputStreamWriter(
						new FileOutputStream( Path.of( outputPath ).resolve( reportName + "-internal.txt" ).toFile() ),
						StandardCharsets.UTF_8
				) ) {
			writer.write( "@defaultMessage Do not use code marked with " + annotationName + " annotation\n" );

			for ( AnnotationInstance annotationInstance : incubating ) {
				AnnotationTarget target = annotationInstance.target();
				String path = ReportGeneratorHelper.determinePath( target );
				if ( path != null ) {
					ReportGeneratorHelper.writeReportLines( writer, path, ignoreRules.matchAnyPublicRule( path ) );
					ReportGeneratorHelper.writeReportLines( writerInternal, path, ignoreRules.matchAnyInternalRule( path ) );
				}
			}
		}
	}
}
