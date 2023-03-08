/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.schema.management.impl;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

final class LuceneSchemaExporter {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final Path DEFAULT_BACKEND_PATH = FileSystems.getDefault().getPath( "backend" );

	private final Optional<String> backendName;

	public LuceneSchemaExporter(Optional<String> backendName) {
		this.backendName = backendName;
	}

	public void export(Path targetDirectory, String name) {
		targetDirectory = targetDirectory
				.resolve( backendName.map( bn -> FileSystems.getDefault().getPath( "backends", bn ) ).orElse( DEFAULT_BACKEND_PATH ) )
				.resolve( "indexes" )
				.resolve( name );
		checkOrCreateTargetDirectory( targetDirectory );

		try {
			writeString(
					targetDirectory.resolve( "index.txt" ),
					"The Lucene backend does not support exporting the schema."
			);
		}
		catch (IOException e) {
			throw log.unableToExportSchema( e.getMessage(), e );
		}
	}

	private void checkOrCreateTargetDirectory(Path targetDirectory) {
		try {
			if ( Files.exists( targetDirectory ) ) {
				if ( !Files.isDirectory( targetDirectory ) || isNotEmpty( targetDirectory ) ) {
					throw log.schemaExporterTargetIsNotEmptyDirectory( targetDirectory );
				}
			}
			else {
				Files.createDirectories( targetDirectory );
			}
		}
		catch (IOException e) {
			throw log.unableToExportSchema( e.getMessage(), e );
		}
	}

	private boolean isNotEmpty(Path targetDirectory) throws IOException {
		try ( Stream<Path> stream = Files.list( targetDirectory ) ) {
			return stream.findAny().isPresent();
		}
	}

	private void writeString(Path path, String content) throws IOException {
		try ( FileOutputStream stream = new FileOutputStream( path.toFile() ) ) {
			stream.write( content.getBytes( StandardCharsets.UTF_8 ) );
		}
	}
}
