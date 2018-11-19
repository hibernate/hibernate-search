/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.work.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.mapper.javabean.work.JavaBeanWorkPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkPlan;

public class JavaBeanWorkPlanImpl implements JavaBeanWorkPlan {

	private final PojoWorkPlan delegate;

	public JavaBeanWorkPlanImpl(PojoWorkPlan delegate) {
		this.delegate = delegate;
	}

	@Override
	public void add(Object entity) {
		delegate.add( entity );
	}

	@Override
	public void add(Object id, Object entity) {
		delegate.add( id, entity );
	}

	@Override
	public void update(Object entity) {
		delegate.update( entity );
	}

	@Override
	public void update(Object id, Object entity) {
		delegate.update( id, entity );
	}

	@Override
	public void update(Object entity, String... dirtyPaths) {
		delegate.update( entity, dirtyPaths );
	}

	@Override
	public void update(Object id, Object entity, String... dirtyPaths) {
		delegate.update( id, entity, dirtyPaths );
	}

	@Override
	public void delete(Object entity) {
		delegate.delete( entity );
	}

	@Override
	public void delete(Object id, Object entity) {
		delegate.delete( id, entity );
	}

	public void prepare() {
		delegate.prepare();
	}

	public CompletableFuture<?> execute() {
		return delegate.execute();
	}
}
