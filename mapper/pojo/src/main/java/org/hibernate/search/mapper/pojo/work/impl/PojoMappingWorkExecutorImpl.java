/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoIndexedTypeManager;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoIndexedTypeManagerContainer;
import org.hibernate.search.mapper.pojo.work.spi.PojoMappingWorkExecutor;
import org.hibernate.search.util.impl.common.LoggerFactory;

public class PojoMappingWorkExecutorImpl implements PojoMappingWorkExecutor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoIndexedTypeManagerContainer indexedTypeManagers;

	public PojoMappingWorkExecutorImpl(PojoIndexedTypeManagerContainer indexedTypeManagers) {
		this.indexedTypeManagers = indexedTypeManagers;
	}

	@Override
	public CompletableFuture<?> optimize(Collection<Class<?>> types) {
		return doOperationOnTypes( PojoIndexedTypeManager::optimize, types );
	}

	@Override
	public CompletableFuture<?> purge(Collection<Class<?>> types) {
		return doOperationOnTypes( PojoIndexedTypeManager::purge, types );
	}

	@Override
	public CompletableFuture<?> flush(Collection<Class<?>> types) {
		return doOperationOnTypes( PojoIndexedTypeManager::flush, types );
	}

	private CompletableFuture<?> doOperationOnTypes(Function<PojoIndexedTypeManager, CompletableFuture<?>> operation, Collection<Class<?>> types) {
		CompletableFuture<?>[] futures = new CompletableFuture<?>[types.size()];
		int typeCounter = 0;

		for ( Class<?> type : types ) {
			futures[typeCounter++] = operation.apply( getTypeManager( type ) );
		}

		// TODO use an << errorHandler >> here
		return CompletableFuture.allOf( futures );
	}

	private PojoIndexedTypeManager<?, ?, ?> getTypeManager(Class<?> clazz) {
		Optional<? extends PojoIndexedTypeManager<?, ?, ?>> exactClass = indexedTypeManagers.getByExactClass( clazz );
		if ( !exactClass.isPresent() ) {
			throw log.notDirectlyIndexedType( clazz );
		}

		return exactClass.get();
	}
}
