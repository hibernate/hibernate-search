/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.realbackend.schema.management;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.integrationtest.mapper.orm.realbackend.testsupport.BackendConfigurations;
import org.hibernate.search.integrationtest.mapper.orm.realbackend.util.Article;
import org.hibernate.search.integrationtest.mapper.orm.realbackend.util.Book;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

class LuceneSchemaManagerExporterIT {

	@TempDir
	public Path temporaryFolder;
	@RegisterExtension
	public OrmSetupHelper setupHelper = OrmSetupHelper.withMultipleBackends(
			BackendConfigurations.simple(),
			Map.of( Article.BACKEND_NAME, BackendConfigurations.simple() )
	);

	private EntityManagerFactory entityManagerFactory;

	@Test
	void lucene() throws IOException {
		entityManagerFactory = setupHelper.start().setup( Book.class, Article.class );

		Path directory = temporaryFolder;
		Search.mapping( entityManagerFactory ).scope( Object.class ).schemaManager().exportExpectedSchema( directory );

		String bookIndex = readString(
				directory.resolve( "backend" ) // as we are using the default backend
						.resolve( "indexes" )
						.resolve( Book.NAME )
						.resolve( "no-schema.txt" ) );
		assertThat( bookIndex ).isEqualTo( "The Lucene backend does not support exporting the schema." );

		String articleIndex = readString(
				directory.resolve( "backends" ) // as we are not using the default backend
						.resolve( Article.BACKEND_NAME ) // name of a backend
						.resolve( "indexes" )
						.resolve( Article.NAME )
						.resolve( "no-schema.txt" ) );
		assertThat( articleIndex ).isEqualTo( "The Lucene backend does not support exporting the schema." );
	}

	private String readString(Path path) throws IOException {
		try ( Stream<String> lines = Files.lines( path ) ) {
			return lines.collect( Collectors.joining( "\n" ) );
		}
	}
}
