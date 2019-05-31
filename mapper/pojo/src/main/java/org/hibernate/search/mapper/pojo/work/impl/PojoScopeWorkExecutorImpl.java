/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkExecutor;
import org.hibernate.search.engine.mapper.session.context.spi.DetachedSessionContextImplementor;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoIndexedTypeManager;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkExecutor;

public class PojoScopeWorkExecutorImpl implements PojoScopeWorkExecutor {

	private final List<IndexWorkExecutor> workExecutors = new ArrayList<>();

	public PojoScopeWorkExecutorImpl(Set<? extends PojoIndexedTypeManager<?, ?, ?>> targetedTypeManagers,
			DetachedSessionContextImplementor sessionContext) {
		for ( PojoIndexedTypeManager<?, ?, ?> targetedTypeManager : targetedTypeManagers ) {
			workExecutors.add( targetedTypeManager.createWorkExecutor( sessionContext ) );
		}
	}

	@Override
	public CompletableFuture<?> optimize() {
		return doOperationOnTypes( IndexWorkExecutor::optimize );
	}

	@Override
	public CompletableFuture<?> purge() {
		return doOperationOnTypes( IndexWorkExecutor::purge );
	}

	@Override
	public CompletableFuture<?> flush() {
		return doOperationOnTypes( IndexWorkExecutor::flush );
	}

	private CompletableFuture<?> doOperationOnTypes(Function<IndexWorkExecutor, CompletableFuture<?>> operation) {
		CompletableFuture<?>[] futures = new CompletableFuture<?>[workExecutors.size()];
		int typeCounter = 0;

		for ( IndexWorkExecutor workExecutor : workExecutors ) {
			futures[typeCounter++] = operation.apply( workExecutor );
		}

		// TODO HSEARCH-3110 use an ErrorHandler here?
		return CompletableFuture.allOf( futures );
	}

}
