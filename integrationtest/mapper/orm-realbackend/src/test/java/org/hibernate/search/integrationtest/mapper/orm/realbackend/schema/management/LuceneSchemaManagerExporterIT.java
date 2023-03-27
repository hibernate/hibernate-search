/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.realbackend.schema.management;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.integrationtest.mapper.orm.realbackend.testsupport.BackendConfigurations;
import org.hibernate.search.integrationtest.mapper.orm.realbackend.util.Article;
import org.hibernate.search.integrationtest.mapper.orm.realbackend.util.Book;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LuceneSchemaManagerExporterIT {

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();
	@Rule
	public OrmSetupHelper setupHelper = OrmSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Test
	public void lucene() throws IOException {
		entityManagerFactory = setupHelper.start()
				.withProperty( "hibernate.search.backend.type", "lucene" )

				.withProperty( "hibernate.search.backends." + Article.BACKEND_NAME + ".type", "lucene" )
				.setup( Book.class, Article.class );

		Path directory = temporaryFolder.newFolder().toPath();
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
