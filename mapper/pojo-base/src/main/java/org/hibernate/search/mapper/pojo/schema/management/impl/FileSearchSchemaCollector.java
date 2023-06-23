/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.schema.management.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.search.engine.common.schema.management.SchemaExport;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.schema.management.SearchSchemaCollector;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class FileSearchSchemaCollector implements SearchSchemaCollector {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final Path DEFAULT_BACKEND_PATH = Paths.get( "backend" );

	private final Path targetDirectory;

	public FileSearchSchemaCollector(Path targetDirectory) {
		this.targetDirectory = targetDirectory;
	}

	@Override
	public void indexSchema(Optional<String> backendName, String indexName, SchemaExport export) {
		Path target = targetDirectory
				.resolve( backendName
						.map( bn -> Paths.get( "backends", bn ) )
						.orElse( DEFAULT_BACKEND_PATH ) )
				.resolve( "indexes" )
				.resolve( indexName );

		try {
			export.toFiles( checkOrCreateTargetDirectory( target ) );
		}
		catch (IOException | RuntimeException e) {
			throw log.unableToExportSchema( e.getMessage(), e,
					EventContexts.fromBackendName( backendName.orElse( null ) )
							.append( EventContexts.fromIndexName( indexName ) )
			);
		}
	}

	private Path checkOrCreateTargetDirectory(Path targetDirectory) throws IOException {
		if ( Files.exists( targetDirectory )
				&& ( !Files.isDirectory( targetDirectory ) || isNotEmpty( targetDirectory ) ) ) {
			throw log.schemaExporterTargetIsNotEmptyDirectory( targetDirectory );
		}
		else {
			return Files.createDirectories( targetDirectory );
		}
	}

	private boolean isNotEmpty(Path targetDirectory) throws IOException {
		try ( Stream<Path> stream = Files.list( targetDirectory ) ) {
			return stream.findAny().isPresent();
		}
	}
}
