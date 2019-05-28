/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.hibernate.search.engine.backend.index.spi.IndexWorkExecutor;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoIndexedTypeManager;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoIndexedTypeManagerContainer;
import org.hibernate.search.mapper.pojo.work.spi.PojoMappingWorkExecutor;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class PojoMappingWorkExecutorImpl implements PojoMappingWorkExecutor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoIndexedTypeManagerContainer indexedTypeManagers;

	private final Map<Class<?>, IndexWorkExecutor> workExecutors = new HashMap<>();

	public PojoMappingWorkExecutorImpl(PojoIndexedTypeManagerContainer indexedTypeManagers) {
		this.indexedTypeManagers = indexedTypeManagers;
	}

	@Override
	public CompletableFuture<?> optimize(Collection<Class<?>> types) {
		return doOperationOnTypes( IndexWorkExecutor::optimize, types );
	}

	@Override
	public CompletableFuture<?> purge(Collection<Class<?>> types, String tenantId) {
		return doOperationOnTypes( workExecutor -> workExecutor.purge( tenantId ), types );
	}

	@Override
	public CompletableFuture<?> flush(Collection<Class<?>> types) {
		return doOperationOnTypes( IndexWorkExecutor::flush, types );
	}

	private CompletableFuture<?> doOperationOnTypes(Function<IndexWorkExecutor, CompletableFuture<?>> operation, Collection<Class<?>> types) {
		CompletableFuture<?>[] futures = new CompletableFuture<?>[types.size()];
		int typeCounter = 0;

		for ( Class<?> type : types ) {
			// TODO Should we support concurrent accesses here?
			if ( !workExecutors.containsKey( type ) ) {
				workExecutors.put( type, createWorkExecutor( type ) );
			}
			futures[typeCounter++] = operation.apply( workExecutors.get( type ) );
		}

		// TODO HSEARCH-3110 use an ErrorHandler here?
		return CompletableFuture.allOf( futures );
	}

	private IndexWorkExecutor createWorkExecutor(Class<?> clazz) {
		Optional<? extends PojoIndexedTypeManager<?, ?, ?>> exactClass = indexedTypeManagers.getByExactClass( clazz );
		if ( !exactClass.isPresent() ) {
			throw log.notDirectlyIndexedType( clazz );
		}

		return exactClass.get().createWorkExecutor();
	}
}
