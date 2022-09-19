/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.backend.elasticsearch.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.documentation.backend.elasticsearch.layout.Book;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;

import org.junit.Rule;
import org.junit.Test;

import com.google.gson.JsonParser;

public class ElasticsearchCustomIndexMappingIT {

	@Rule
	public TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend(
			BackendConfigurations.simple() );

	@Test
	public void smoke() throws Exception {
		EntityManagerFactory entityManagerFactory = setupHelper.start()
				.withBackendProperty(
						ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MAPPING_FILE,
						"custom/index-mapping.json"
				)
				.setup( Book.class );

		String mapping = elasticsearchClient.index( Book.NAME ).type().getMapping();
		assertJsonEquals( expectedMergedMapping(), mapping );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Book book = new Book();
			book.setTitle( "The Robots Of Dawn" );
			entityManager.persist( book );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Book> result = searchSession.search( Book.class )
					.where( f -> f.match().field( "title" ).matching( "robot" ) )
					.fetchHits( 20 );

			assertThat( result ).hasSize( 1 );
		} );
	}

	private String expectedMergedMapping() throws Exception {
		try ( InputStream in = ElasticsearchCustomIndexMappingIT.class.getClassLoader()
				.getResourceAsStream( "custom/index-mapping-merged.json" ) ) {

			Reader reader = new InputStreamReader( in, "UTF-8" );
			return JsonParser.parseReader( reader ).toString();
		}
	}
}
