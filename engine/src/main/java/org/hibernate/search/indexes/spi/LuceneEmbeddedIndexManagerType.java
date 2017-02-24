/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.spi;

import org.hibernate.search.analyzer.impl.LuceneEmbeddedAnalyzerStrategy;
import org.hibernate.search.analyzer.spi.AnalyzerStrategy;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.nulls.impl.LuceneMissingValueStrategy;
import org.hibernate.search.engine.nulls.impl.MissingValueStrategy;
import org.hibernate.search.engine.service.spi.ServiceManager;

public final class LuceneEmbeddedIndexManagerType implements IndexManagerType {

	public static final LuceneEmbeddedIndexManagerType INSTANCE = new LuceneEmbeddedIndexManagerType();

	private LuceneEmbeddedIndexManagerType() {
		//use the INSTANCE singleton
	}

	@Override
	public AnalyzerStrategy createAnalyzerStrategy(ServiceManager serviceManager, SearchConfiguration cfg) {
		return new LuceneEmbeddedAnalyzerStrategy( serviceManager, cfg );
	}

	@Override
	public MissingValueStrategy createMissingValueStrategy(ServiceManager serviceManager, SearchConfiguration cfg) {
		return LuceneMissingValueStrategy.INSTANCE;
	}
}
