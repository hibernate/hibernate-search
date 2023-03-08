/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.standalone.realbackend.schema.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;

import org.hibernate.search.integrationtest.mapper.pojo.standalone.realbackend.testsupport.BackendConfigurations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SchemaManagerExporterIT {

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();
	@Rule
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withSingleBackend(
					MethodHandles.lookup(), BackendConfigurations.simple() );

	@Test
	public void elasticsearch() throws IOException {
		SearchMapping mapping = setupHelper.start()
				.withProperty( "hibernate.search.backend.type", "elasticsearch" )
				.withProperty( "hibernate.search.backend.version_check.enabled", false )
				.withProperty( "hibernate.search.backend.version", "8.6" )

				.withProperty( "hibernate.search.backends." + Article.BACKEND_NAME + ".type", "elasticsearch" )
				.withProperty( "hibernate.search.backends." + Article.BACKEND_NAME + ".version_check.enabled", false )
				.withProperty( "hibernate.search.backends." + Article.BACKEND_NAME + ".version", "8.6" )
				.setup( Book.class, Article.class );

		Path directory = temporaryFolder.newFolder().toPath();
		try ( SearchSession session = mapping.createSession() ) {
			session.schemaManager().exportSchema( directory );
		}

		String bookIndex = Files.readString(
				directory.resolve( "backend" ) // as we are using the default backend
						.resolve( "indexes" )
						.resolve( Book.class.getName() ) // we use FQN as who knows maybe someone will decide to have same class names in different packages
						.resolve( "index.json" ) );
		assertJsonEquals(
				"{" +
						"  \"aliases\": {" +
						"    \"book-write\": {" +
						"      \"is_write_index\": true" +
						"    }," +
						"    \"book-read\": {" +
						"      \"is_write_index\": false" +
						"    }" +
						"  }," +
						"  \"mapping\": {" +
						"    \"properties\": {" +
						"      \"_entity_type\": {" +
						"        \"type\": \"keyword\"," +
						"        \"index\": false," +
						"        \"doc_values\": true" +
						"      }" +
						"    }," +
						"    \"dynamic\": \"strict\"" +
						"  }," +
						"  \"settings\": {}" +
						"}",
				bookIndex
		);

		String bookInfo = Files.readString(
				directory.resolve( "backend" ) // as we are using the default backend
						.resolve( "indexes" )
						.resolve( Book.class.getName() ) // we use FQN as who knows maybe someone will decide to have same class names in different packages
						.resolve( "additional-information.json" ) );

		assertJsonEquals(
				"{" +
						"  \"primaryIndexName\": {" +
						"    \"encoded\": \"book-000001\"," +
						"    \"original\": \"book-000001\"" +
						"  }" +
						"}",
				bookInfo
		);

		String articleIndex = Files.readString(
				directory.resolve( "backends" ) // as we are not using the default backend
						.resolve( Article.BACKEND_NAME ) // name of a backend
						.resolve( "indexes" )
						.resolve( Article.class.getName() ) // we use FQN as who knows maybe someone will decide to have same class names in different packages
						.resolve( "index.json" ) );
		assertJsonEquals(
				"{" +
						"  \"aliases\": {" +
						"    \"article-write\": {" +
						"      \"is_write_index\": true" +
						"    }," +
						"    \"article-read\": {" +
						"      \"is_write_index\": false" +
						"    }" +
						"  }," +
						"  \"mapping\": {" +
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
						"    }," +
						"    \"dynamic\": \"strict\"" +
						"  }," +
						"  \"settings\": {}" +
						"}",
				articleIndex
		);

		String articleInfo = Files.readString(
				directory.resolve( "backends" ) // as we are not using the default backend
						.resolve( Article.BACKEND_NAME ) // name of a backend
						.resolve( "indexes" )
						.resolve( Article.class.getName() ) // we use FQN as who knows maybe someone will decide to have same class names in different packages
						.resolve( "additional-information.json" ) );

		assertJsonEquals(
				"{" +
						"  \"primaryIndexName\": {" +
						"    \"encoded\": \"article-000001\"," +
						"    \"original\": \"article-000001\"" +
						"  }" +
						"}",
				articleInfo
		);
	}

	@Test
	public void lucene() throws IOException {
		SearchMapping mapping = setupHelper.start()
				.withProperty( "hibernate.search.backend.type", "lucene" )

				.withProperty( "hibernate.search.backends." + Article.BACKEND_NAME + ".type", "lucene" )
				.setup( Book.class, Article.class );

		Path directory = temporaryFolder.newFolder().toPath();
		try ( SearchSession session = mapping.createSession() ) {
			session.schemaManager().exportSchema( directory );
		}

		String bookIndex = Files.readString(
				directory.resolve( "backend" ) // as we are using the default backend
						.resolve( "indexes" )
						.resolve( Book.class.getName() ) // we use FQN as who knows maybe someone will decide to have same class names in different packages
						.resolve( "index.txt" ) );
		assertThat( bookIndex ).isEqualTo( "The Lucene backend does not support exporting the schema." );

		String articleIndex = Files.readString(
				directory.resolve( "backends" ) // as we are not using the default backend
						.resolve( Article.BACKEND_NAME ) // name of a backend
						.resolve( "indexes" )
						.resolve( Article.class.getName() ) // we use FQN as who knows maybe someone will decide to have same class names in different packages
						.resolve( "index.txt" ) );
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

	@Indexed(index = Article.NAME, backend = Article.BACKEND_NAME )
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
}
