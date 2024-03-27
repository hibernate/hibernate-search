/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.java.modules.pojo.standalone.lucene.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.java.modules.pojo.standalone.lucene.entity.Author;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotatedTypeSource;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;

public class AuthorService implements AutoCloseable {

	private static final AtomicInteger ID_PROVIDER = new AtomicInteger( 1 );
	private final CloseableSearchMapping mapping;

	public AuthorService() {
		mapping = createSearchMapping();
	}

	private CloseableSearchMapping createSearchMapping() {
		return SearchMapping.builder( AnnotatedTypeSource.fromClasses( AuthorService.class ) )
				.property( "hibernate.search.backend.directory.type", "local-heap" )
				.property(
						"hibernate.search.backend.analysis.configurer",
						"org.hibernate.search.integrationtest.java.modules.pojo.standalone.lucene.config.MyLuceneAnalysisConfigurer"
				)
				.build();
	}

	public void add(String name) {
		try ( SearchSession session = mapping.createSessionWithOptions()
				.indexingPlanSynchronizationStrategy( IndexingPlanSynchronizationStrategy.sync() )
				.build() ) {

			Author author = new Author( ID_PROVIDER.getAndIncrement(), name );
			session.indexingPlan().add( author );
			SimulatedDatastore.put( author );
		}
	}

	public List<Author> search(String term) {
		try ( SearchSession session = mapping.createSession() ) {
			SearchQuery<Author> query = session.search( Author.class )
					.where( f -> f.match().field( "name" ).matching( term ) )
					.toQuery();

			return query.fetchAllHits();
		}
	}

	@Override
	public void close() {
		SimulatedDatastore.clear();
		mapping.close();
	}
}
