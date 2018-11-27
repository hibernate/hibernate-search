/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.impl;

import java.util.Collection;
import java.util.Collections;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.mapper.orm.search.impl.HibernateOrmSearchTargetImpl;
import org.hibernate.search.mapper.orm.search.spi.HibernateOrmSearchTarget;
import org.hibernate.search.mapper.orm.session.spi.HibernateOrmSearchManager;
import org.hibernate.search.mapper.orm.session.spi.HibernateOrmSearchManagerBuilder;
import org.hibernate.search.mapper.orm.mapping.context.impl.HibernateOrmMappingContextImpl;
import org.hibernate.search.mapper.orm.session.context.impl.HibernateOrmSessionContextImpl;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkPlan;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.session.spi.AbstractPojoSearchManager;
import org.hibernate.search.mapper.pojo.search.spi.PojoSearchTargetDelegate;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;

public class HibernateOrmSearchManagerImpl extends AbstractPojoSearchManager
		implements HibernateOrmSearchManager {
	private final SessionImplementor sessionImplementor;

	private HibernateOrmSearchManagerImpl(HibernateOrmSearchManagerBuilderImpl builder) {
		super( builder );
		this.sessionImplementor = builder.sessionImplementor;
	}

	@Override
	public void close() {
		// Nothing to do
	}

	@Override
	public <T> HibernateOrmSearchTarget<T> search(Class<T> targetedType) {
		return search( Collections.singleton( targetedType ) );
	}

	@Override
	public <T> HibernateOrmSearchTarget<T> search(Collection<? extends Class<? extends T>> targetedTypes) {
		PojoSearchTargetDelegate<T, T> searchTargetDelegate = getDelegate().createPojoSearchTarget( targetedTypes );
		return new HibernateOrmSearchTargetImpl<>( searchTargetDelegate, sessionImplementor );
	}

	@Override
	public PojoWorkPlan createWorkPlan() {
		return getDelegate().createWorkPlan();
	}

	public static class HibernateOrmSearchManagerBuilderImpl extends AbstractBuilder<HibernateOrmSearchManagerImpl>
			implements HibernateOrmSearchManagerBuilder {
		private final HibernateOrmMappingContextImpl mappingContext;
		private final SessionImplementor sessionImplementor;

		public HibernateOrmSearchManagerBuilderImpl(PojoMappingDelegate mappingDelegate,
				HibernateOrmMappingContextImpl mappingContext,
				SessionImplementor sessionImplementor) {
			super( mappingDelegate );
			this.mappingContext = mappingContext;
			this.sessionImplementor = sessionImplementor;
		}

		@Override
		protected AbstractPojoSessionContextImplementor buildSessionContext() {
			return new HibernateOrmSessionContextImpl( mappingContext, sessionImplementor );
		}

		@Override
		public HibernateOrmSearchManagerImpl build() {
			return new HibernateOrmSearchManagerImpl( this );
		}
	}
}
