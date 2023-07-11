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
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

public class InternalReportGenerator {

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
			String basePackageToScan,
			Index index,
			ReportGeneratorRules ignoreRules)
			throws IOException {

		Collection<DotName> packages = allPackages( index, basePackageToScan );
		try ( Writer writer = new OutputStreamWriter(
				new FileOutputStream( Path.of( outputPath ).resolve( reportName + ".txt" ).toFile() ),
				StandardCharsets.UTF_8 );
				Writer writerInternal = new OutputStreamWriter(
						new FileOutputStream( Path.of( outputPath ).resolve( reportName + "-internal.txt" ).toFile() ),
						StandardCharsets.UTF_8
				) ) {
			writer.write( "@defaultMessage Do not use code from internal packages\n" );

			for ( DotName pakcage : packages ) {
				String path = pakcage.toString();
				ReportGeneratorHelper.writeReportLines( writer, path, ignoreRules.matchAnyPublicRule( path ) );
				ReportGeneratorHelper.writeReportLines( writerInternal, path, ignoreRules.matchAnyInternalRule( path ) );
			}
		}
	}

	private static Collection<DotName> allPackages(Index index, String base) {
		Set<DotName> result = new TreeSet<>();

		doAllPackages( index, DotName.createSimple( base ), result );

		return result;
	}

	private static final Pattern INTERNAL_PACKAGE = Pattern.compile( "^.+\\.internal\\b.*$" );

	private static void doAllPackages(Index index, DotName current, Set<DotName> result) {
		if ( INTERNAL_PACKAGE.matcher( current.toString() ).matches() ) {
			result.add( current );
		}
		index.getSubpackages( current ).forEach( p -> doAllPackages( index, p, result ) );
	}

}
