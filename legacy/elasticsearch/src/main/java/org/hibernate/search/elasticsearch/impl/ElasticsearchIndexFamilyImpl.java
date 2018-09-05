/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import org.hibernate.search.analyzer.spi.AnalyzerStrategy;
import org.hibernate.search.elasticsearch.indexes.ElasticsearchIndexFamily;
import org.hibernate.search.engine.nulls.impl.MissingValueStrategy;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.spi.IndexFamilyImplementor;

public class ElasticsearchIndexFamilyImpl implements IndexFamilyImplementor, ElasticsearchIndexFamily {

	private final ServiceManager serviceManager;
	private final ElasticsearchService elasticsearchService;

	public ElasticsearchIndexFamilyImpl(ServiceManager serviceManager) {
		this.serviceManager = serviceManager;
		this.elasticsearchService = serviceManager.requestService( ElasticsearchService.class );
	}

	@Override
	public void close() {
		serviceManager.releaseService( ElasticsearchService.class );
	}

	@Override
	public <T> T unwrap(Class<T> unwrappedClass) {
		if ( unwrappedClass.isAssignableFrom( ElasticsearchIndexFamily.class ) ) {
			return (T) this;
		}
		else {
			throw new SearchException( "Cannot unwrap a '" + getClass().getName() + "' into a '" + unwrappedClass.getName() + "'" );
		}
	}

	@Override
	public <T> T getClient(Class<T> clientClass) {
		return elasticsearchService.getClient( clientClass );
	}

	@Override
	public AnalyzerStrategy createAnalyzerStrategy() {
		return elasticsearchService.getAnalyzerStrategyFactory().create();
	}

	@Override
	public MissingValueStrategy getMissingValueStrategy() {
		return elasticsearchService.getMissingValueStrategy();
	}
}
