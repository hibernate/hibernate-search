/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.standalone.realbackend.schema.management;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.search.integrationtest.mapper.pojo.standalone.realbackend.testsupport.BackendConfigurations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LuceneSchemaManagerExporterIT {

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();
	@Rule
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withSingleBackend(
					MethodHandles.lookup(), BackendConfigurations.simple() );

	@Test
	public void lucene() throws IOException {
		SearchMapping mapping = setupHelper.start()
				.withProperty( "hibernate.search.backend.type", "lucene" )

				.withProperty( "hibernate.search.backends." + Article.BACKEND_NAME + ".type", "lucene" )
				.setup( Book.class, Article.class );

		Path directory = temporaryFolder.newFolder().toPath();
		mapping.scope( Object.class ).schemaManager().exportExpectedSchema( directory );

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

	@Indexed(index = Book.NAME)
	public static class Book {
		static final String NAME = "book";
		@DocumentId
		private Integer id;

		private String title;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	}

	@Indexed(index = Article.NAME, backend = Article.BACKEND_NAME)
	public class Article {
		public static final String BACKEND_NAME = "custom-backend";
		static final String NAME = "article";
		@DocumentId
		private Integer id;

		@FullTextField
		private String title;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	}

	private String readString(Path path) throws IOException {
		try ( Stream<String> lines = Files.lines( path ) ) {
			return lines.collect( Collectors.joining( "\n" ) );
		}
	}
}
