/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.util.Map;

import org.hibernate.search.analyzer.spi.AnalyzerStrategy;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.integration.impl.SearchIntegration;
import org.hibernate.search.engine.nulls.impl.MissingValueStrategy;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.indexes.spi.IndexManagerType;

/**
 * @author Yoann Rodiere
 */
public final class SearchIntegrationConfigContext {

	private final MissingValueStrategy missingValueStrategy;

	private final MutableAnalyzerRegistry analyzerRegistry;

	public SearchIntegrationConfigContext(IndexManagerType type,
			ServiceManager serviceManager, SearchConfiguration searchConfiguration) {
		this( type, serviceManager, searchConfiguration, null );
	}

	public SearchIntegrationConfigContext(IndexManagerType type,
			ServiceManager serviceManager, SearchConfiguration searchConfiguration,
			SearchIntegration previousIntegrationState) {
		AnalyzerStrategy strategy = type.createAnalyzerStrategy( serviceManager, searchConfiguration );
		this.analyzerRegistry = new MutableAnalyzerRegistry(
				strategy, previousIntegrationState == null ? null : previousIntegrationState.getAnalyzerRegistry() );
		this.missingValueStrategy = type.createMissingValueStrategy( serviceManager, searchConfiguration );
	}

	public MissingValueStrategy getMissingValueStrategy() {
		return missingValueStrategy;
	}

	public MutableAnalyzerRegistry getAnalyzerRegistry() {
		return analyzerRegistry;
	}

	public ImmutableSearchIntegration initialize(Map<String, AnalyzerDef> mappingAnalyzerDefs) {
		analyzerRegistry.initialize( mappingAnalyzerDefs );
		return new ImmutableSearchIntegration( analyzerRegistry );
	}

}
