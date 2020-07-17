/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class PojoIndexerImpl implements PojoIndexer {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoWorkIndexedTypeContextProvider indexedTypeContextProvider;
	private final PojoWorkSessionContext<?> sessionContext;

	private final Map<PojoRawTypeIdentifier<?>, PojoTypeIndexer<?, ?>> delegates = new ConcurrentHashMap<>();

	public PojoIndexerImpl(PojoWorkIndexedTypeContextProvider indexedTypeContextProvider,
			PojoWorkSessionContext<?> sessionContext) {
		this.indexedTypeContextProvider = indexedTypeContextProvider;
		this.sessionContext = sessionContext;
	}

	@Override
	public CompletableFuture<?> add(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId, String providedRoutingKey, Object entity,
			DocumentCommitStrategy commitStrategy,
			DocumentRefreshStrategy refreshStrategy) {
		return getDelegate( typeIdentifier ).add( providedId, providedRoutingKey, entity, commitStrategy, refreshStrategy );
	}

	@Override
	public CompletableFuture<?> addOrUpdate(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId, String providedRoutingKey, Object entity,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return getDelegate( typeIdentifier ).addOrUpdate( providedId, providedRoutingKey, entity, commitStrategy, refreshStrategy );
	}

	@Override
	public CompletableFuture<?> delete(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId, Object entity,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return getDelegate( typeIdentifier ).delete( providedId, entity, commitStrategy, refreshStrategy );
	}

	@Override
	public CompletableFuture<?> purge(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId,
			String providedRoutingKey,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return getDelegate( typeIdentifier ).purge( providedId, providedRoutingKey, commitStrategy, refreshStrategy );
	}

	private PojoTypeIndexer<?, ?> getDelegate(PojoRawTypeIdentifier<?> typeIdentifier) {
		// Call get() before resorting to computeIfAbsent,
		// because it's faster and will be enough in the vast majority of cases.
		PojoTypeIndexer<?, ?> delegate = this.delegates.get( typeIdentifier );
		if ( delegate == null ) {
			delegate = this.delegates.computeIfAbsent( typeIdentifier, this::createTypeIndexer );
		}
		return delegate;
	}

	private PojoTypeIndexer<?, ?> createTypeIndexer(PojoRawTypeIdentifier<?> typeIdentifier) {
		Optional<? extends PojoWorkIndexedTypeContext<?, ?>> typeContext =
				indexedTypeContextProvider.getByExactType( typeIdentifier );
		if ( !typeContext.isPresent() ) {
			throw log.notDirectlyIndexedType( typeIdentifier );
		}

		return typeContext.get().createIndexer( sessionContext );
	}
}
