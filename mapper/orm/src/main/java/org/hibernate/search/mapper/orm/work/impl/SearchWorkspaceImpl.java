/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.work.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.mapper.orm.work.SearchWorkspace;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkExecutor;
import org.hibernate.search.util.common.impl.Futures;

public class SearchWorkspaceImpl implements SearchWorkspace {
	private final PojoScopeWorkExecutor scopeWorkExecutor;

	public SearchWorkspaceImpl(PojoScopeWorkExecutor scopeWorkExecutor) {
		this.scopeWorkExecutor = scopeWorkExecutor;
	}

	@Override
	public void optimize() {
		Futures.unwrappedExceptionJoin( optimizeAsync() );
	}

	@Override
	public CompletableFuture<?> optimizeAsync() {
		return scopeWorkExecutor.optimize();
	}

	@Override
	public void purge() {
		Futures.unwrappedExceptionJoin( purgeAsync() );
	}

	@Override
	public CompletableFuture<?> purgeAsync() {
		return scopeWorkExecutor.purge();
	}

	@Override
	public void flush() {
		Futures.unwrappedExceptionJoin( flushAsync() );
	}

	@Override
	public CompletableFuture<?> flushAsync() {
		return scopeWorkExecutor.flush();
	}
}
