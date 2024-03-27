/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient;
import org.hibernate.search.util.impl.test.JsonHelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.skyscreamer.jsonassert.JSONCompareMode;

class ElasticsearchCustomLayoutStrategyIT {
	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	@RegisterExtension
	public TestElasticsearchClient elasticsearchClient = TestElasticsearchClient.create();

	@Test
	void smoke() {
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
