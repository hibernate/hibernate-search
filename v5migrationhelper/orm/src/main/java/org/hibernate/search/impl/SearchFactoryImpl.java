/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.backend.lucene.LuceneBackend;
import org.hibernate.search.backend.lucene.index.LuceneIndexManager;
import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;

/**
 * Implementation of the public API: a simple delegate to the actual implementor.
 *
 * @author Sanne Grinovero (C) 2014 Red Hat Inc.
 * @since 5.0
 */
final class SearchFactoryImpl implements SearchFactory {

	public static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final V5MigrationOrmSearchIntegratorAdapter searchIntegrator;

	public SearchFactoryImpl(V5MigrationOrmSearchIntegratorAdapter searchIntegrator) {
		this.searchIntegrator = searchIntegrator;
	}

	@Override
	public void optimize() {
		optimize( Object.class );
	}

	@Override
	public void optimize(Class<?> clazz) {
		searchIntegrator.toSearchMapping().scope( clazz ).workspace().mergeSegments();
	}

	@Override
	public Analyzer getAnalyzer(String name) {
		return searchIntegrator.toSearchMapping().backend().unwrap( LuceneBackend.class ).analyzer( name )
				.orElseThrow( () -> log.unknownAnalyzer( name ) );
	}

	@Override
	public Analyzer getAnalyzer(Class<?> clazz) {
		return searchIntegrator.toSearchMapping().indexedEntity( clazz )
				.indexManager().unwrap( LuceneIndexManager.class )
				.indexingAnalyzer();
	}

	@Override
	public QueryContextBuilder buildQueryBuilder() {
		return searchIntegrator.buildQueryBuilder();
	}

	@Override
	public Set<Class<?>> getIndexedTypes() {
		return searchIntegrator.toSearchMapping().allIndexedEntities()
				.stream().map( SearchIndexedEntity::javaClass )
				.collect( Collectors.toSet() );
	}

	@Override
	public <T> T unwrap(Class<T> cls) {
		if ( SearchIntegrator.class.isAssignableFrom( cls ) ) {
			return (T) searchIntegrator;
		}
		else if ( SearchMapping.class.isAssignableFrom( cls ) ) {
			return (T) searchIntegrator.toSearchMapping();
		}
		else {
			throw log.cannotUnwrapSearchFactory( cls );
		}
	}

}
