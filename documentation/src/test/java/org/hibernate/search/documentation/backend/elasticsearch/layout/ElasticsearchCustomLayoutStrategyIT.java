/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.backend.elasticsearch.layout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.encodeName;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.test.JsonHelper;

import org.junit.Rule;
import org.junit.Test;

import org.skyscreamer.jsonassert.JSONCompareMode;

public class ElasticsearchCustomLayoutStrategyIT {
	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	@Rule
	public TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

	@Test
	public void smoke() {
		URLEncodedString primaryIndexName = encodeName( Book.NAME + "-20171106-191900-000000000" );
		elasticsearchClient.index( primaryIndexName, null, null )
				.ensureDoesNotExist();

		EntityManagerFactory entityManagerFactory = setupHelper.start()
				.withBackendProperty(
						ElasticsearchBackendSettings.LAYOUT_STRATEGY,
						CustomLayoutStrategy.class.getName()
				)
				.setup( Book.class );

		JsonHelper.assertJsonEquals(
				"{'book-write': {}, 'book': {}}",
				elasticsearchClient.index( primaryIndexName, null, null )
						.aliases().get(),
				JSONCompareMode.LENIENT
		);

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Book book = new Book();
			book.setTitle( "The Robots Of Dawn" );
			entityManager.persist( book );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Book> result = searchSession.search( Book.class )
					.extension( ElasticsearchExtension.get() )
					.where( f -> f.match().field( "title" ).matching( "robot" ) )
					.fetchHits( 20 );

			assertThat( result ).hasSize( 1 );
		} );
	}

}
