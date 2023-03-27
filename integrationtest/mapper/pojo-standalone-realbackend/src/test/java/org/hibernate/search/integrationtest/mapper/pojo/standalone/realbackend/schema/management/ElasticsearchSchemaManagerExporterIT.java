/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.standalone.realbackend.schema.management;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect.isActualVersion;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEqualsIgnoringUnknownFields;
import static org.junit.Assume.assumeFalse;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.search.integrationtest.mapper.pojo.standalone.realbackend.testsupport.BackendConfigurations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ElasticsearchSchemaManagerExporterIT {

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();
	@Rule
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withSingleBackend(
					MethodHandles.lookup(), BackendConfigurations.simple() );
	private SearchMapping mapping;

	@Before
	public void setUp() throws Exception {
		String version = ElasticsearchTestDialect.getActualVersion().toString();
		this.mapping = setupHelper.start()
				// so that we don't try to do anything with the schema and allow to run without ES being up:
				.withProperty( "hibernate.search.schema_management.strategy", "none" )

				.withProperty( "hibernate.search.backend.type", "elasticsearch" )
				.withProperty( "hibernate.search.backend.version_check.enabled", false )
				.withProperty( "hibernate.search.backend.version", version )

				.withProperty( "hibernate.search.backends." + Article.BACKEND_NAME + ".type", "elasticsearch" )
				.withProperty( "hibernate.search.backends." + Article.BACKEND_NAME + ".version_check.enabled", false )
				.withProperty( "hibernate.search.backends." + Article.BACKEND_NAME + ".version", version )
				.setup( Book.class, Article.class );
	}

	@Test
	public void elasticsearch() throws IOException {
		assumeFalse(
				"Older versions of Elasticsearch would not match the mappings",
				isActualVersion(
						esVersion -> esVersion.isLessThan( "7.0" ),
						osVersion -> false
				)
		);

		Path directory = temporaryFolder.newFolder().toPath();
		mapping.scope( Object.class ).schemaManager().exportExpectedSchema( directory );

		assertJsonEqualsIgnoringUnknownFields(
				"{" +
						"  \"aliases\": {" +
						"    \"book-write\": {" +
						"      \"is_write_index\": true" +
						"    }," +
						"    \"book-read\": {" +
						"      \"is_write_index\": false" +
						"    }" +
						"  }," +
						"  \"mappings\": {" +
						"    \"properties\": {" +
						"      \"_entity_type\": {" +
						"        \"type\": \"keyword\"," +
						"        \"index\": false," +
						"        \"doc_values\": true" +
						"      }" +
						"    }" +
						"  }," +
						"  \"settings\": {}" +
						"}",
				readString(
						directory.resolve( "backend" ) // as we are using the default backend
								.resolve( "indexes" )
								.resolve( Book.NAME )
								.resolve( "create-index.json" ) )
		);

		assertJsonEquals(
				"{}",
				readString(
						directory.resolve( "backend" ) // as we are using the default backend
								.resolve( "indexes" )
								.resolve( Book.NAME )
								.resolve( "create-index-query-params.json" ) )
		);

		assertJsonEqualsIgnoringUnknownFields(
				"{" +
						"  \"aliases\": {" +
						"    \"article-write\": {" +
						"      \"is_write_index\": true" +
						"    }," +
						"    \"article-read\": {" +
						"      \"is_write_index\": false" +
						"    }" +
						"  }," +
						"  \"mappings\": {" +
						"    \"properties\": {" +
						"      \"_entity_type\": {" +
						"        \"type\": \"keyword\"," +
						"        \"index\": false," +
						"        \"doc_values\": true" +
						"      }," +
						"      \"title\": {" +
						"        \"type\": \"text\"," +
						"        \"index\": true," +
						"        \"norms\": true," +
						"        \"analyzer\": \"default\"," +
						"        \"term_vector\": \"no\"" +
						"      }" +
						"    }" +
						"  }," +
						"  \"settings\": {}" +
						"}",
				readString(
						directory.resolve( "backends" ) // as we are not using the default backend
								.resolve( Article.BACKEND_NAME ) // name of a backend
								.resolve( "indexes" )
								.resolve( Article.NAME )
								.resolve( "create-index.json" ) )
		);

		assertJsonEquals(
				"{}",
				readString(
						directory.resolve( "backends" ) // as we are not using the default backend
								.resolve( Article.BACKEND_NAME ) // name of a backend
								.resolve( "indexes" )
								.resolve( Article.NAME )
								.resolve( "create-index-query-params.json" ) )
		);
	}

	@Test
	public void exportToExistingDirectory() throws IOException {
		Path directory = temporaryFolder.newFolder().toPath();
		Path path = Files.createDirectories( directory.resolve( "backend" )
				.resolve( "indexes" )
				.resolve( Book.NAME )
		);
		writeString(
				path
						.resolve( "not-an-index.json" ),
				"{}"
		);

		assertThatThrownBy( () ->
				mapping.scope( Object.class ).schemaManager().exportExpectedSchema( directory )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Target path",
						path.toString(),
						"already exists and is not an empty directory",
						"Use a path to an empty or non-existing directory"
				);
	}

	@Test
	public void exportToExistingFile() throws IOException {
		Path directory = temporaryFolder.newFolder().toPath();
		Path path = Files.createDirectories( directory.resolve( "backend" )
				.resolve( "indexes" )
		);
		writeString(
				path.resolve( Book.NAME ),
				"{}"
		);

		assertThatThrownBy( () ->
				mapping.scope( Object.class ).schemaManager().exportExpectedSchema( directory )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Target path",
						path.resolve( Book.NAME ).toString(),
						"already exists and is not an empty directory",
						"Use a path to an empty or non-existing directory"
				);
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

	private void writeString(Path path, String string) throws IOException {
		try ( OutputStream outputStream = Files.newOutputStream( path );
				OutputStreamWriter writer = new OutputStreamWriter( outputStream, StandardCharsets.UTF_8 ) ) {
			writer.write( string );
		}
	}
}
