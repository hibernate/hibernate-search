/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.spi;

import org.hibernate.search.analyzer.spi.AnalyzerStrategy;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.elasticsearch.impl.ElasticsearchService;
import org.hibernate.search.engine.nulls.impl.MissingValueStrategy;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.ServiceReference;
import org.hibernate.search.indexes.spi.IndexManagerType;

public final class ElasticsearchIndexManagerType implements IndexManagerType {

	public static final ElasticsearchIndexManagerType INSTANCE = new ElasticsearchIndexManagerType();

	private ElasticsearchIndexManagerType() {
		//use the INSTANCE singleton
	}

	@Override
	public AnalyzerStrategy createAnalyzerStrategy(ServiceManager serviceManager, SearchConfiguration cfg) {
		try ( ServiceReference<ElasticsearchService> esService = serviceManager.requestReference( ElasticsearchService.class ) ) {
			return esService.get().getAnalyzerStrategyFactory().create( cfg );
		}
	}

	@Override
	public MissingValueStrategy createMissingValueStrategy(ServiceManager serviceManager, SearchConfiguration cfg) {
		try ( ServiceReference<ElasticsearchService> esService = serviceManager.requestReference( ElasticsearchService.class ) ) {
			return esService.get().getMissingValueStrategy();
		}
	}
}
