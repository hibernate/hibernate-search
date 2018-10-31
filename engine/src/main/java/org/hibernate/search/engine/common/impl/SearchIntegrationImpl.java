/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.environment.bean.spi.BeanResolver;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.mapper.mapping.spi.MappingKey;
import org.hibernate.search.util.impl.common.Closer;
import org.hibernate.search.util.impl.common.LoggerFactory;

public class SearchIntegrationImpl implements SearchIntegration {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BeanResolver beanResolver;

	private final Map<MappingKey<?>, MappingImplementor<?>> mappings;
	private final Map<String, BackendImplementor<?>> backends;
	private final Map<String, IndexManagerImplementor<?>> indexManagers;

	SearchIntegrationImpl(BeanResolver beanResolver,
			Map<MappingKey<?>, MappingImplementor<?>> mappings,
			Map<String, BackendImplementor<?>> backends,
			Map<String, IndexManagerImplementor<?>> indexManagers) {
		this.beanResolver = beanResolver;
		this.mappings = mappings;
		this.backends = backends;
		this.indexManagers = indexManagers;
	}

	@Override
	public <M> M getMapping(MappingKey<M> mappingKey) {
		// See SearchIntegrationBuilderImpl: we are sure that, if there is a mapping, it implements MappingImplementor<M>
		@SuppressWarnings("unchecked")
		MappingImplementor<M> mappingImplementor = (MappingImplementor<M>) mappings.get( mappingKey );
		if ( mappingImplementor == null ) {
			throw log.noMappingRegistered( mappingKey );
		}
		return mappingImplementor.toAPI();
	}

	@Override
	public Backend getBackend(String backendName) {
		BackendImplementor<?> backend = backends.get( backendName );
		if ( backend == null ) {
			throw log.noBackendRegistered( backendName );
		}
		return backend.toAPI();
	}

	@Override
	public IndexManager getIndexManager(String indexManagerName) {
		IndexManagerImplementor<?> indexManager = indexManagers.get( indexManagerName );
		if ( indexManager == null ) {
			throw log.noIndexManagerRegistered( indexManagerName );
		}
		return indexManager.toAPI();
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( MappingImplementor::close, mappings.values() );
			closer.pushAll( IndexManagerImplementor::close, indexManagers.values() );
			closer.pushAll( BackendImplementor::close, backends.values() );
			closer.pushAll( BeanResolver::close, beanResolver );
		}
	}
}
