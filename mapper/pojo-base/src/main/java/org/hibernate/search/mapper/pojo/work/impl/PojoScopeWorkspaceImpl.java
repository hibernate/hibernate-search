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

import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.backend.work.execution.spi.UnsupportedOperationBehavior;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeMappingContext;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkspace;
import org.hibernate.search.util.common.function.TriFunction;

public class PojoScopeWorkspaceImpl implements PojoScopeWorkspace {

	private final List<IndexWorkspace> delegates = new ArrayList<>();

	public PojoScopeWorkspaceImpl(PojoScopeMappingContext mappingContext,
			Set<? extends PojoWorkIndexedTypeContext<?, ?>> targetedTypeContexts,
			Set<String> tenantIds) {
		for ( PojoWorkIndexedTypeContext<?, ?> targetedTypeContext : targetedTypeContexts ) {
			delegates.add( targetedTypeContext.createWorkspace( mappingContext, tenantIds ) );
		}
	}

	@Override
	public CompletableFuture<?> mergeSegments(OperationSubmitter operationSubmitter,
			UnsupportedOperationBehavior unsupportedOperationBehavior) {
		return doOperationOnTypes( IndexWorkspace::mergeSegments, operationSubmitter, unsupportedOperationBehavior );
	}

	@Override
	public CompletableFuture<?> purge(Set<String> routingKeys, OperationSubmitter operationSubmitter,
			UnsupportedOperationBehavior unsupportedOperationBehavior) {
		return doOperationOnTypes(
				(indexWorkspace, submitter, unsupportedBehavior) -> indexWorkspace.purge( routingKeys, submitter,
						unsupportedBehavior ),
				operationSubmitter, unsupportedOperationBehavior );
	}

	@Override
	public CompletableFuture<?> flush(OperationSubmitter operationSubmitter,
			UnsupportedOperationBehavior unsupportedOperationBehavior) {
		return doOperationOnTypes( IndexWorkspace::flush, operationSubmitter, unsupportedOperationBehavior );
	}

	@Override
	public CompletableFuture<?> refresh(OperationSubmitter operationSubmitter,
			UnsupportedOperationBehavior unsupportedOperationBehavior) {
		return doOperationOnTypes( IndexWorkspace::refresh, operationSubmitter, unsupportedOperationBehavior );
	}

	private CompletableFuture<?> doOperationOnTypes(
			TriFunction<IndexWorkspace, OperationSubmitter, UnsupportedOperationBehavior, CompletableFuture<?>> operation,
			OperationSubmitter operationSubmitter,
			UnsupportedOperationBehavior unsupportedOperationBehavior) {
		CompletableFuture<?>[] futures = new CompletableFuture<?>[delegates.size()];
		int typeCounter = 0;

		for ( IndexWorkspace delegate : delegates ) {
			futures[typeCounter++] = operation.apply( delegate, operationSubmitter, unsupportedOperationBehavior );
		}

		// TODO HSEARCH-3110 use a FailureHandler here?
		return CompletableFuture.allOf( futures );
	}

}
