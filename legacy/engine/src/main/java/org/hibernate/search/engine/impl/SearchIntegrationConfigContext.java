/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.util.List;
import java.util.Map;

import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.analyzer.spi.AnalyzerStrategy;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.NormalizerDef;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.integration.impl.SearchIntegration;
import org.hibernate.search.engine.nulls.impl.MissingValueStrategy;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.indexes.spi.IndexFamilyImplementor;
import org.hibernate.search.indexes.spi.IndexManagerType;

/**
 * @author Yoann Rodiere
 */
public final class SearchIntegrationConfigContext {

	private final AnalyzerStrategy analyzerStrategy;

	private final MutableAnalyzerRegistry analyzerRegistry;

	private final MutableNormalizerRegistry normalizerRegistry;

	private final IndexFamilyImplementor indexFamily;

	public SearchIntegrationConfigContext(IndexManagerType type,
			ServiceManager serviceManager, SearchConfiguration searchConfiguration) {
		this( type, serviceManager, searchConfiguration, null );
	}

	public SearchIntegrationConfigContext(IndexManagerType type,
			ServiceManager serviceManager, SearchConfiguration searchConfiguration,
			SearchIntegration previousIntegrationState) {
		this.indexFamily = previousIntegrationState == null ? type.createIndexFamily( serviceManager, searchConfiguration )
				: previousIntegrationState.getIndexFamily();
		/*
		 * Analyzer strategies are re-created on each SearchFactory increment,
		 * so that the new analyzer definitions can be added between two SearchFactory increments.
		 */
		this.analyzerStrategy = indexFamily.createAnalyzerStrategy();
		this.analyzerRegistry = new MutableAnalyzerRegistry(
				analyzerStrategy, previousIntegrationState == null ? null : previousIntegrationState.getAnalyzerRegistry() );
		this.normalizerRegistry = new MutableNormalizerRegistry(
				analyzerStrategy, previousIntegrationState == null ? null : previousIntegrationState.getNormalizerRegistry() );
	}

	public MissingValueStrategy getMissingValueStrategy() {
		return indexFamily.getMissingValueStrategy();
	}

	public MutableAnalyzerRegistry getAnalyzerRegistry() {
		return analyzerRegistry;
	}

	public MutableNormalizerRegistry getNormalizerRegistry() {
		return normalizerRegistry;
	}

	public ImmutableSearchIntegration initialize(Map<String, AnalyzerDef> mappingAnalyzerDefs,
			Map<String, NormalizerDef> mappingNormalizerDefs) {
		List<AnalyzerReference> analyzerReferences = analyzerRegistry.getAllReferences();
		List<AnalyzerReference> normalizerReferences = normalizerRegistry.getAllReferences();
		analyzerStrategy.initializeReferences(
				analyzerReferences, mappingAnalyzerDefs, normalizerReferences, mappingNormalizerDefs );

		return new ImmutableSearchIntegration( indexFamily, analyzerRegistry, normalizerRegistry );
	}

}
