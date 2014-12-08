/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.impl;

import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.metadata.IndexedTypeDescriptor;
import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.stat.Statistics;


/**
 * Implementation of the public API: a simple delegate to the actual implementor.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2014 Red Hat Inc.
 * @since 5.0
 */
final class SearchFactoryImpl implements SearchFactory {

	private final SearchIntegrator searchIntegrator;

	public SearchFactoryImpl(SearchIntegrator searchIntegrator) {
		this.searchIntegrator = searchIntegrator;
	}

	@Override
	public void optimize() {
		searchIntegrator.optimize();
	}

	@Override
	public void optimize(Class<?> entityType) {
		searchIntegrator.optimize( entityType );
	}

	@Override
	public Analyzer getAnalyzer(String name) {
		return searchIntegrator.getAnalyzer( name );
	}

	@Override
	public Analyzer getAnalyzer(Class<?> clazz) {
		return searchIntegrator.getAnalyzer( clazz );
	}

	@Override
	public QueryContextBuilder buildQueryBuilder() {
		return searchIntegrator.buildQueryBuilder();
	}

	@Override
	public Statistics getStatistics() {
		return searchIntegrator.getStatistics();
	}

	@Override
	public IndexReaderAccessor getIndexReaderAccessor() {
		return searchIntegrator.getIndexReaderAccessor();
	}

	@Override
	public IndexedTypeDescriptor getIndexedTypeDescriptor(Class<?> entityType) {
		return searchIntegrator.getIndexedTypeDescriptor( entityType );
	}

	@Override
	public Set<Class<?>> getIndexedTypes() {
		return searchIntegrator.getIndexedTypes();
	}

	@Override
	public <T> T unwrap(Class<T> cls) {
		if ( SearchIntegrator.class.isAssignableFrom( cls ) ) {
			return (T) searchIntegrator;
		}
		else {
			return searchIntegrator.unwrap( cls );
		}
	}

}
