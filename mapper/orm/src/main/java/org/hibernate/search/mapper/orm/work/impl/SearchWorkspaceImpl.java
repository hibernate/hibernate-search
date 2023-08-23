/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.work.impl;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.UnsupportedOperationBehavior;
import org.hibernate.search.mapper.orm.work.SearchWorkspace;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkspace;
import org.hibernate.search.util.common.impl.Futures;

public class SearchWorkspaceImpl implements SearchWorkspace {
	private final PojoScopeWorkspace delegate;

	public SearchWorkspaceImpl(PojoScopeWorkspace delegate) {
		this.delegate = delegate;
	}

	@Override
	public void mergeSegments() {
		Futures.unwrappedExceptionJoin(
				delegate.mergeSegments( OperationSubmitter.blocking(), UnsupportedOperationBehavior.FAIL ) );
	}

	@Override
	public CompletableFuture<?> mergeSegmentsAsync() {
		return delegate.mergeSegments( OperationSubmitter.rejecting(), UnsupportedOperationBehavior.FAIL );
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
		Futures.unwrappedExceptionJoin(
				delegate.purge( routingKeys, OperationSubmitter.blocking(), UnsupportedOperationBehavior.FAIL ) );
	}

	@Override
	public CompletableFuture<?> purgeAsync(Set<String> routingKeys) {
		return delegate.purge( routingKeys, OperationSubmitter.rejecting(), UnsupportedOperationBehavior.FAIL );
	}

	@Override
	public void flush() {
		Futures.unwrappedExceptionJoin( delegate.flush( OperationSubmitter.blocking(), UnsupportedOperationBehavior.FAIL ) );
	}

	@Override
	public CompletableFuture<?> flushAsync() {
		return delegate.flush( OperationSubmitter.rejecting(), UnsupportedOperationBehavior.FAIL );
	}

	@Override
	public void refresh() {
		Futures.unwrappedExceptionJoin( delegate.refresh( OperationSubmitter.blocking(), UnsupportedOperationBehavior.FAIL ) );
	}

	@Override
	public CompletableFuture<?> refreshAsync() {
		return delegate.refresh( OperationSubmitter.rejecting(), UnsupportedOperationBehavior.FAIL );
	}
}
