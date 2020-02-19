/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.backend.elasticsearch.layout;


import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.encodeName;

import java.util.List;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.documentation.testsupport.ElasticsearchBackendConfiguration;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;
import org.hibernate.search.util.impl.test.JsonHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.skyscreamer.jsonassert.JSONCompareMode;

public class ElasticsearchCustomLayoutStrategyIT {
	private static final String BACKEND_NAME = "testBackend";

	@Rule
	public OrmSetupHelper setupHelper = OrmSetupHelper.withSingleBackend( BACKEND_NAME, new ElasticsearchBackendConfiguration() );

	@Rule
	public TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

	@Before
	public void setup() {
	}

	@Test
	public void smoke() {
		URLEncodedString primaryIndexName = encodeName( Book.NAME + "-20171106-191900-000000000" );
		elasticsearchClient.index( primaryIndexName, null, null )
				.ensureDoesNotExist().registerForCleanup();

		EntityManagerFactory entityManagerFactory = setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
						AutomaticIndexingSynchronizationStrategyNames.SYNC
				)
				.withBackendProperty(
						BACKEND_NAME,
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

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			Book book = new Book();
			book.setTitle( "The Robots Of Dawn" );
			entityManager.persist( book );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Book> result = searchSession.search( Book.class )
					.extension( ElasticsearchExtension.get() )
					.where( f -> f.match().field( "title" ).matching( "robot" ) )
					.fetchHits( 20 );

			assertThat( result ).hasSize( 1 );
		} );
	}

}
