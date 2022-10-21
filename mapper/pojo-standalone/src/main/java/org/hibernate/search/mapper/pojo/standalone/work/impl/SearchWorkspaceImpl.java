/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.work.impl;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.impl.OperationSubmitterType;
import org.hibernate.search.engine.backend.work.execution.spi.OperationSubmitter;
import org.hibernate.search.mapper.pojo.standalone.work.SearchWorkspace;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkspace;
import org.hibernate.search.util.common.impl.Futures;

public class SearchWorkspaceImpl implements SearchWorkspace {
	private final PojoScopeWorkspace delegate;

	public SearchWorkspaceImpl(PojoScopeWorkspace delegate) {
		this.delegate = delegate;
	}

	@Override
	public void mergeSegments() {
		Futures.unwrappedExceptionJoin( delegate.mergeSegments( OperationSubmitterType.BLOCKING ) );
	}

	@Override
	public CompletableFuture<?> mergeSegmentsAsync() {
		return delegate.mergeSegments( OperationSubmitterType.REJECTING_EXECUTION_EXCEPTION );
	}

	@Override
	public void purge() {
		purge( Collections.emptySet() );
	}

	@Override
	public CompletableFuture<?> purgeAsync() {
		return purgeAsync( Collections.emptySet() );
	}

	@Override
	public void purge(Set<String> routingKeys) {
		Futures.unwrappedExceptionJoin( delegate.purge( routingKeys, OperationSubmitterType.BLOCKING ) );
	}

	@Override
	public CompletableFuture<?> purgeAsync(Set<String> routingKeys) {
		return delegate.purge( routingKeys, OperationSubmitterType.REJECTING_EXECUTION_EXCEPTION );
	}

	@Override
	public void flush() {
		Futures.unwrappedExceptionJoin( delegate.flush( OperationSubmitterType.BLOCKING ) );
	}

	@Override
	public CompletableFuture<?> flushAsync() {
		return delegate.flush( OperationSubmitterType.REJECTING_EXECUTION_EXCEPTION );
	}

	@Override
	public void refresh() {
		Futures.unwrappedExceptionJoin( delegate.refresh( OperationSubmitterType.BLOCKING ) );
	}

	@Override
	public CompletableFuture<?> refreshAsync() {
		return delegate.refresh( OperationSubmitterType.REJECTING_EXECUTION_EXCEPTION );
	}
}
