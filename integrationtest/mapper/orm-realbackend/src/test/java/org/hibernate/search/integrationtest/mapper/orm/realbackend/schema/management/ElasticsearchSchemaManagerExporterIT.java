/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.realbackend.schema.management;

import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.integrationtest.mapper.orm.realbackend.testsupport.BackendConfigurations;
import org.hibernate.search.integrationtest.mapper.orm.realbackend.util.Article;
import org.hibernate.search.integrationtest.mapper.orm.realbackend.util.Book;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ElasticsearchSchemaManagerExporterIT {

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();
	@Rule
	public OrmSetupHelper setupHelper = OrmSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Test
	public void elasticsearch() throws IOException {
		entityManagerFactory = setupHelper.start()
				.withProperty( "hibernate.search.backend.type", "elasticsearch" )
				.withProperty( "hibernate.search.backend.version_check.enabled", false )
				.withProperty( "hibernate.search.backend.version", "8.6" )

				.withProperty( "hibernate.search.backends." + Article.BACKEND_NAME + ".type", "elasticsearch" )
				.withProperty( "hibernate.search.backends." + Article.BACKEND_NAME + ".version_check.enabled", false )
				.withProperty( "hibernate.search.backends." + Article.BACKEND_NAME + ".version", "8.6" )
				.setup( Book.class, Article.class );

		Path directory = temporaryFolder.newFolder().toPath();
		Search.mapping( entityManagerFactory ).scope( Object.class ).schemaManager().exportSchema( directory );

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
}
