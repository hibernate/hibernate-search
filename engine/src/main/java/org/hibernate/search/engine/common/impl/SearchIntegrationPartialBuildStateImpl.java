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
import org.hibernate.search.engine.common.spi.SearchIntegrationPartialBuildState;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.environment.bean.spi.BeanResolver;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.mapper.mapping.spi.MappingKey;
import org.hibernate.search.engine.mapper.mapping.spi.MappingPartialBuildState;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Closer;

class SearchIntegrationPartialBuildStateImpl implements SearchIntegrationPartialBuildState {

	private final BeanResolver beanResolver;

	private final Map<MappingKey<?, ?>, MappingPartialBuildState> partiallyBuiltMappings;
	private final Map<MappingKey<?, ?>, MappingImplementor<?>> fullyBuiltMappings = new LinkedHashMap<>();
	private final Map<String, BackendImplementor<?>> backends;
	private final Map<String, IndexManagerImplementor<?>> indexManagers;

	SearchIntegrationPartialBuildStateImpl(
			BeanResolver beanResolver,
			Map<MappingKey<?, ?>, MappingPartialBuildState> partiallyBuiltMappings,
			Map<String, BackendImplementor<?>> backends,
			Map<String, IndexManagerImplementor<?>> indexManagers) {
		this.beanResolver = beanResolver;
		this.partiallyBuiltMappings = partiallyBuiltMappings;
		this.backends = backends;
		this.indexManagers = indexManagers;
	}

	@Override
	public void closeOnFailure() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( MappingPartialBuildState::closeOnFailure, partiallyBuiltMappings.values() );
			closer.pushAll( MappingImplementor::close, fullyBuiltMappings.values() );
			closer.pushAll( IndexManagerImplementor::close, indexManagers.values() );
			closer.pushAll( BackendImplementor::close, backends.values() );
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

		return mapping.toAPI();
	}

	@Override
	public SearchIntegration finalizeIntegration() {
		if ( !partiallyBuiltMappings.isEmpty() ) {
			throw new AssertionFailure(
				"Some mappings were not fully built; there is probably a bug in Hibernate Search."
					+ " Partially built mappings: " + partiallyBuiltMappings
			);
		}

		return new SearchIntegrationImpl(
				beanResolver,
				fullyBuiltMappings,
				backends,
				indexManagers
		);
	}
}
