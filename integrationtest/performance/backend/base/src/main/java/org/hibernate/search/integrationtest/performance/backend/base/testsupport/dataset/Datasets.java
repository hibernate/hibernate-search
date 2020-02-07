/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.performance.backend.base.testsupport.dataset;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public final class Datasets {

	public static final String CONSTANT_TEXT = "constant-text";

	public static final String HIBERNATE_DEV_ML_2016_01 = "hibernate-dev-ml-2016-01";

	private static final String DATASET_CACHE_DIRECTORY = "dataset";

	private static final URI HIBERNATE_DEV_MAILING_LIST_URI =
			URI.create( "http://lists.jboss.org/pipermail/hibernate-dev/2016-January.txt" );

	private Datasets() {
	}

	public static Dataset createDataset(String name, Path cacheDirectory)
			throws IOException {
		switch ( name ) {
			case CONSTANT_TEXT:
				return new ConstantDataset();
			case HIBERNATE_DEV_ML_2016_01:
				Path path = fetch( name, HIBERNATE_DEV_MAILING_LIST_URI, cacheDirectory );
				return new SampleDataset( parseMailingListDigest( path ) );
			default:
				throw new IllegalArgumentException( "Unknown dataset: " + name );
		}
	}

	private static Path fetch(String name, URI uri, Path cacheDirectory) throws IOException {
		Path datasetCacheDirectory = cacheDirectory.resolve( DATASET_CACHE_DIRECTORY );
		if ( !Files.exists( datasetCacheDirectory ) ) {
			Files.createDirectory( datasetCacheDirectory );
		}

		Path outputPath = datasetCacheDirectory.resolve( name );
		if ( Files.exists( outputPath ) ) {
			return outputPath;
		}

		try ( InputStream input = uri.toURL().openStream();
				ReadableByteChannel inputChannel = Channels.newChannel( input );
				FileChannel outputChannel = FileChannel.open(
						outputPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW ) ) {
			outputChannel.transferFrom( inputChannel, 0, Long.MAX_VALUE );
		}
		return outputPath;
	}

	private static List<SampleDataset.DataSample> parseMailingListDigest(Path path) throws IOException {
		try ( BufferedReader reader = Files.newBufferedReader( path ) ) {
			return new MailingListDigestParser( reader ).parse();
		}
	}

}
