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
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.metadata.IndexedTypeDescriptor;
import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.stat.Statistics;


/**
 * Implementation of the public API: a simple delegate to the actual implementor.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2014 Red Hat Inc.
 * @since 5.0
 */
public final class SearchFactoryImpl implements SearchFactory {

	private final SearchFactoryImplementor searchFactoryImplementor;

	public SearchFactoryImpl(SearchFactoryImplementor searchFactoryImplementor) {
		this.searchFactoryImplementor = searchFactoryImplementor;
	}

	@Override
	public void optimize() {
		searchFactoryImplementor.optimize();
	}

	@Override
	public void optimize(Class<?> entityType) {
		searchFactoryImplementor.optimize( entityType );
	}

	@Override
	public Analyzer getAnalyzer(String name) {
		return searchFactoryImplementor.getAnalyzer( name );
	}

	@Override
	public Analyzer getAnalyzer(Class<?> clazz) {
		return searchFactoryImplementor.getAnalyzer( clazz );
	}

	@Override
	public QueryContextBuilder buildQueryBuilder() {
		return searchFactoryImplementor.buildQueryBuilder();
	}

	@Override
	public Statistics getStatistics() {
		return searchFactoryImplementor.getStatistics();
	}

	@Override
	public IndexReaderAccessor getIndexReaderAccessor() {
		return searchFactoryImplementor.getIndexReaderAccessor();
	}

	@Override
	public IndexedTypeDescriptor getIndexedTypeDescriptor(Class<?> entityType) {
		return searchFactoryImplementor.getIndexedTypeDescriptor( entityType );
	}

	@Override
	public Set<Class<?>> getIndexedTypes() {
		return searchFactoryImplementor.getIndexedTypes();
	}

	@Override
	public <T> T unwrap(Class<T> cls) {
		if ( SearchFactoryImplementor.class.isAssignableFrom( cls ) ) {
			return (T) searchFactoryImplementor;
		}
		else {
			return searchFactoryImplementor.unwrap( cls );
		}
	}

}
