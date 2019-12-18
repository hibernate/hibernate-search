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

import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkspace;

public class PojoScopeWorkspaceImpl implements PojoScopeWorkspace {

	private final List<IndexWorkspace> delegates = new ArrayList<>();

	public PojoScopeWorkspaceImpl(Set<? extends PojoWorkIndexedTypeContext<?, ?, ?>> targetedTypeContexts,
			DetachedBackendSessionContext sessionContext) {
		for ( PojoWorkIndexedTypeContext<?, ?, ?> targetedTypeContext : targetedTypeContexts ) {
			delegates.add( targetedTypeContext.createWorkspace( sessionContext ) );
		}
	}

	@Override
	public CompletableFuture<?> mergeSegments() {
		return doOperationOnTypes( IndexWorkspace::mergeSegments );
	}

	@Override
	public CompletableFuture<?> purge() {
		return doOperationOnTypes( IndexWorkspace::purge );
	}

	@Override
	public CompletableFuture<?> flush() {
		return doOperationOnTypes( IndexWorkspace::flush );
	}

	private CompletableFuture<?> doOperationOnTypes(Function<IndexWorkspace, CompletableFuture<?>> operation) {
		CompletableFuture<?>[] futures = new CompletableFuture<?>[delegates.size()];
		int typeCounter = 0;

		for ( IndexWorkspace delegate : delegates ) {
			futures[typeCounter++] = operation.apply( delegate );
		}

		// TODO HSEARCH-3110 use a FailureHandler here?
		return CompletableFuture.allOf( futures );
	}

}
