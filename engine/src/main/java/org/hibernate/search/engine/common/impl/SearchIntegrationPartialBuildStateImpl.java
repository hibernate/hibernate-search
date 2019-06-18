/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.common.spi.SearchIntegrationPartialBuildState;
import org.hibernate.search.engine.environment.bean.spi.BeanResolver;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.mapper.mapping.spi.MappingKey;
import org.hibernate.search.engine.mapper.mapping.spi.MappingPartialBuildState;
import org.hibernate.search.engine.reporting.impl.RootFailureCollector;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Closer;

class SearchIntegrationPartialBuildStateImpl implements SearchIntegrationPartialBuildState {

	private static final int FAILURE_LIMIT = 100;

	private final BeanResolver beanResolver;

	private final Map<MappingKey<?, ?>, MappingPartialBuildState> partiallyBuiltMappings;
	private final Map<MappingKey<?, ?>, MappingImplementor<?>> fullyBuiltMappings = new LinkedHashMap<>();
	private final Map<String, BackendPartialBuildState> partiallyBuiltBackends;
	private final Map<String, BackendImplementor<?>> fullyBuiltBackends = new LinkedHashMap<>();
	private final Map<String, IndexManagerPartialBuildState> partiallyBuiltIndexManagers;
	private final Map<String, IndexManagerImplementor<?>> fullyBuiltIndexManagers = new LinkedHashMap<>();

	SearchIntegrationPartialBuildStateImpl(
			BeanResolver beanResolver,
			Map<MappingKey<?, ?>, MappingPartialBuildState> partiallyBuiltMappings,
			Map<String, BackendPartialBuildState> partiallyBuiltBackends,
			Map<String, IndexManagerPartialBuildState> partiallyBuiltIndexManagers) {
		this.beanResolver = beanResolver;
		this.partiallyBuiltMappings = partiallyBuiltMappings;
		this.partiallyBuiltBackends = partiallyBuiltBackends;
		this.partiallyBuiltIndexManagers = partiallyBuiltIndexManagers;
	}

	@Override
	public void closeOnFailure() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( MappingPartialBuildState::closeOnFailure, partiallyBuiltMappings.values() );
			closer.pushAll( MappingImplementor::close, fullyBuiltMappings.values() );
			closer.pushAll( IndexManagerPartialBuildState::closeOnFailure, partiallyBuiltIndexManagers.values() );
			closer.pushAll( IndexManagerImplementor::close, fullyBuiltIndexManagers.values() );
			closer.pushAll( BackendPartialBuildState::closeOnFailure, partiallyBuiltBackends.values() );
			closer.pushAll( BackendImplementor::close, fullyBuiltBackends.values() );
			closer.pushAll( BeanResolver::close, beanResolver );
		}
	}

	@Override
	public <PBM, M> M finalizeMapping(MappingKey<PBM, M> mappingKey,
			Function<PBM, MappingImplementor<M>> director) {
		// We know this cast will work because of how
		@SuppressWarnings("unchecked")
		PBM partiallyBuiltMapping = (PBM) partiallyBuiltMappings.get( mappingKey );
		if ( partiallyBuiltMapping == null ) {
			throw new AssertionFailure(
					"Some partially built mapping could not be found during bootstrap; there is probably a bug in Hibernate Search."
					+ " Key: " + mappingKey
			);
		}

		MappingImplementor<M> mapping = director.apply( partiallyBuiltMapping );
		fullyBuiltMappings.put( mappingKey, mapping );
		partiallyBuiltMappings.remove( mappingKey );

		return mapping.toConcreteType();
	}

	@Override
	public SearchIntegration finalizeIntegration(ConfigurationPropertySource configurationPropertySource) {
		if ( !partiallyBuiltMappings.isEmpty() ) {
			throw new AssertionFailure(
					"Some mappings were not fully built; there is probably a bug in Hibernate Search."
							+ " Partially built mappings: " + partiallyBuiltMappings
			);
		}

		RootFailureCollector failureCollector = new RootFailureCollector( FAILURE_LIMIT );

		// Start backends
		for ( Map.Entry<String, BackendPartialBuildState> entry : partiallyBuiltBackends.entrySet() ) {
			// TODO HSEARCH-3084 perform backend initialization in parallel for all backends?
			fullyBuiltBackends.put(
					entry.getKey(),
					entry.getValue().finalizeBuild( failureCollector, configurationPropertySource )
			);
		}
		failureCollector.checkNoFailure();

		// Start indexes
		for ( Map.Entry<String, IndexManagerPartialBuildState> entry : partiallyBuiltIndexManagers.entrySet() ) {
			// TODO HSEARCH-3084 perform index initialization in parallel for all indexes?
			fullyBuiltIndexManagers.put(
					entry.getKey(),
					entry.getValue().finalizeBuild( failureCollector, configurationPropertySource )
			);
		}
		failureCollector.checkNoFailure();

		return new SearchIntegrationImpl(
				beanResolver,
				fullyBuiltMappings,
				fullyBuiltBackends,
				fullyBuiltIndexManagers
		);
	}
}
