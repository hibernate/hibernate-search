/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.spi;

import org.hibernate.search.analyzer.spi.AnalyzerStrategy;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.elasticsearch.analyzer.impl.ElasticsearchAnalyzerStrategy;
import org.hibernate.search.elasticsearch.nulls.impl.ElasticsearchMissingValueStrategy;
import org.hibernate.search.engine.nulls.impl.MissingValueStrategy;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.indexes.spi.IndexManagerType;

public final class ElasticsearchIndexManagerType implements IndexManagerType {

	public static final ElasticsearchIndexManagerType INSTANCE = new ElasticsearchIndexManagerType();

	private ElasticsearchIndexManagerType() {
		//use the INSTANCE singleton
	}

	@Override
	public AnalyzerStrategy createAnalyzerStrategy(ServiceManager serviceManager, SearchConfiguration cfg) {
		return new ElasticsearchAnalyzerStrategy();
	}

	@Override
	public MissingValueStrategy getMissingValueStrategy() {
		return ElasticsearchMissingValueStrategy.INSTANCE;
	}
}
