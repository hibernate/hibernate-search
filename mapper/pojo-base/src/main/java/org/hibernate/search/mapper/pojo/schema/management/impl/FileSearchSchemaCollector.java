/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.schema.management.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.search.engine.common.schema.management.SchemaExport;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.mapper.pojo.logging.impl.MappingLog;
import org.hibernate.search.mapper.pojo.schema.management.SearchSchemaCollector;

public class FileSearchSchemaCollector implements SearchSchemaCollector {

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
			throw MappingLog.INSTANCE.unableToExportSchema( e.getMessage(), e,
					EventContexts.fromBackendName( backendName.orElse( null ) )
							.append( EventContexts.fromIndexName( indexName ) )
			);
		}
	}

	private Path checkOrCreateTargetDirectory(Path targetDirectory) throws IOException {
		if ( Files.exists( targetDirectory )
				&& ( !Files.isDirectory( targetDirectory ) || isNotEmpty( targetDirectory ) ) ) {
			throw MappingLog.INSTANCE.schemaExporterTargetIsNotEmptyDirectory( targetDirectory );
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
