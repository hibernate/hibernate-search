/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.work.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.mapper.javabean.work.SearchWorkPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;

public class SearchWorkPlanImpl implements SearchWorkPlan {

	private final PojoIndexingPlan delegate;

	public SearchWorkPlanImpl(PojoIndexingPlan delegate) {
		this.delegate = delegate;
	}

	@Override
	public void add(Object entity) {
		delegate.add( entity );
	}

	@Override
	public void add(Object providedId, Object entity) {
		delegate.add( providedId, entity );
	}

	@Override
	public void update(Object entity) {
		delegate.update( entity );
	}

	@Override
	public void update(Object providedId, Object entity) {
		delegate.update( providedId, entity );
	}

	@Override
	public void update(Object entity, String... dirtyPaths) {
		delegate.update( entity, dirtyPaths );
	}

	@Override
	public void update(Object providedId, Object entity, String... dirtyPaths) {
		delegate.update( providedId, entity, dirtyPaths );
	}

	@Override
	public void delete(Object entity) {
		delegate.delete( entity );
	}

	@Override
	public void delete(Object providedId, Object entity) {
		delegate.delete( providedId, entity );
	}

	public void prepare() {
		delegate.prepare();
	}

	public CompletableFuture<?> execute() {
		return delegate.execute();
	}
}
