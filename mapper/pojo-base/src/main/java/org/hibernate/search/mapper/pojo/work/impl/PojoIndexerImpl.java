/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoBackendSessionContext;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class PojoIndexerImpl implements PojoIndexer {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoWorkIndexedTypeContextProvider indexedTypeContextProvider;
	private final AbstractPojoBackendSessionContext sessionContext;
	private final PojoRuntimeIntrospector introspector;
	private final DocumentCommitStrategy commitStrategy;

	private final Map<PojoRawTypeIdentifier<?>, PojoTypeIndexer<?, ?, ?>> typeExecutors = new HashMap<>();

	public PojoIndexerImpl(PojoWorkIndexedTypeContextProvider indexedTypeContextProvider,
			AbstractPojoBackendSessionContext sessionContext,
			DocumentCommitStrategy commitStrategy) {
		this.indexedTypeContextProvider = indexedTypeContextProvider;
		this.sessionContext = sessionContext;
		this.introspector = sessionContext.getRuntimeIntrospector();
		this.commitStrategy = commitStrategy;
	}

	@Override
	public CompletableFuture<?> add(Object providedId, Object entity) {
		PojoRawTypeIdentifier<?> typeIdentifier = introspector.getTypeIdentifier( entity );
		PojoTypeIndexer<?, ?, ?> typeExecutor = this.typeExecutors.get( typeIdentifier );
		if ( typeExecutor == null ) {
			typeExecutor = createTypeIndexer( typeIdentifier );
			typeExecutors.put( typeIdentifier, typeExecutor );
		}

		return typeExecutor.add( providedId, entity );
	}

	private PojoTypeIndexer<?, ?, ?> createTypeIndexer(PojoRawTypeIdentifier<?> typeIdentifier) {
		Optional<? extends PojoWorkIndexedTypeContext<?, ?, ?>> typeContext =
				indexedTypeContextProvider.getByExactType( typeIdentifier );
		if ( !typeContext.isPresent() ) {
			throw log.notDirectlyIndexedType( typeIdentifier );
		}

		return typeContext.get().createIndexer( sessionContext, commitStrategy );
	}
}
