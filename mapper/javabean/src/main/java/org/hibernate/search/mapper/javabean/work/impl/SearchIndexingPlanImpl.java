/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.work.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.mapper.javabean.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.util.common.impl.Throwables;

public class SearchIndexingPlanImpl implements SearchIndexingPlan {

	private final PojoIndexingPlan delegate;

	public SearchIndexingPlanImpl(PojoIndexingPlan delegate) {
		this.delegate = delegate;
	}

	@Override
	public void add(Object entity) {
		delegate.add( null, entity );
	}

	@Override
	public void add(Object providedId, Object entity) {
		delegate.add( providedId, entity );
	}

	@Override
	public void addOrUpdate(Object entity) {
		delegate.addOrUpdate( null, entity );
	}

	@Override
	public void addOrUpdate(Object providedId, Object entity) {
		delegate.addOrUpdate( providedId, entity );
	}

	@Override
	public void addOrUpdate(Object entity, String... dirtyPaths) {
		delegate.addOrUpdate( null, entity, dirtyPaths );
	}

	@Override
	public void addOrUpdate(Object providedId, Object entity, String... dirtyPaths) {
		delegate.addOrUpdate( providedId, entity, dirtyPaths );
	}

	@Override
	public void delete(Object entity) {
		delegate.delete( null, entity );
	}

	@Override
	public void delete(Object providedId, Object entity) {
		delegate.delete( providedId, entity );
	}

	public CompletableFuture<?> execute() {
		return delegate.executeAndReport().thenApply( report -> {
			report.getThrowable().ifPresent( t -> {
				throw Throwables.toRuntimeException( t );
			} );
			return null;
		} );
	}
}
