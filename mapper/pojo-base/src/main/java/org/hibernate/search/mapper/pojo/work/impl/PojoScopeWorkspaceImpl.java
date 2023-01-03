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
import java.util.function.BiFunction;

import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeMappingContext;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkspace;

public class PojoScopeWorkspaceImpl implements PojoScopeWorkspace {

	private final List<IndexWorkspace> delegates = new ArrayList<>();

	public PojoScopeWorkspaceImpl(PojoScopeMappingContext mappingContext,
			Set<? extends PojoWorkIndexedTypeContext<?, ?>> targetedTypeContexts,
			String tenantId) {
		for ( PojoWorkIndexedTypeContext<?, ?> targetedTypeContext : targetedTypeContexts ) {
			delegates.add( targetedTypeContext.createWorkspace( mappingContext, tenantId ) );
		}
	}

	@Override
	public CompletableFuture<?> mergeSegments(OperationSubmitter operationSubmitter) {
		return doOperationOnTypes( IndexWorkspace::mergeSegments, operationSubmitter );
	}

	@Override
	public CompletableFuture<?> purge(Set<String> routingKeys, OperationSubmitter operationSubmitter) {
		return doOperationOnTypes( (indexWorkspace, submitter) -> indexWorkspace.purge( routingKeys, submitter ), operationSubmitter );
	}

	@Override
	public CompletableFuture<?> flush(OperationSubmitter operationSubmitter) {
		return doOperationOnTypes( IndexWorkspace::flush, operationSubmitter );
	}

	@Override
	public CompletableFuture<?> refresh(OperationSubmitter operationSubmitter) {
		return doOperationOnTypes( IndexWorkspace::refresh, operationSubmitter );
	}

	private CompletableFuture<?> doOperationOnTypes(
			BiFunction<IndexWorkspace, OperationSubmitter,
			CompletableFuture<?>> operation,
			OperationSubmitter operationSubmitter) {
		CompletableFuture<?>[] futures = new CompletableFuture<?>[delegates.size()];
		int typeCounter = 0;

		for ( IndexWorkspace delegate : delegates ) {
			futures[typeCounter++] = operation.apply( delegate, operationSubmitter );
		}

		// TODO HSEARCH-3110 use a FailureHandler here?
		return CompletableFuture.allOf( futures );
	}

}
