/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.impl;

import java.util.Collection;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.massindexing.impl.MassIndexerImpl;
import org.hibernate.search.mapper.orm.search.SearchScope;
import org.hibernate.search.mapper.orm.search.impl.SearchScopeImpl;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.orm.session.spi.SearchSessionImplementor;
import org.hibernate.search.mapper.orm.session.spi.SearchSessionBuilder;
import org.hibernate.search.mapper.orm.mapping.context.impl.HibernateOrmMappingContextImpl;
import org.hibernate.search.mapper.orm.session.context.impl.HibernateOrmSessionContextImpl;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkPlan;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.session.spi.AbstractPojoSearchSession;
import org.hibernate.search.mapper.pojo.search.spi.PojoSearchScopeDelegate;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;
import org.hibernate.search.mapper.pojo.work.spi.PojoSessionWorkExecutor;

/**
 * The actual implementation of {@link SearchSession}.
 */
public class HibernateOrmSearchSession extends AbstractPojoSearchSession
		implements SearchSessionImplementor, SearchSession {
	private final SessionImplementor sessionImplementor;

	private HibernateOrmSearchSession(HibernateOrmSearchSessionBuilder builder) {
		super( builder );
		this.sessionImplementor = builder.sessionImplementor;
	}

	@Override
	public void close() {
		// Nothing to do
	}

	@Override
	public EntityManager toJpaEntityManager() {
		return sessionImplementor;
	}

	@Override
	public Session toHibernateOrmSession() {
		return sessionImplementor;
	}

	@Override
	public <T> SearchScope<T> scope(Collection<? extends Class<? extends T>> types) {
		PojoSearchScopeDelegate<T, T> searchScopeDelegate = getDelegate().createPojoSearchScope( types );
		return new SearchScopeImpl<>( searchScopeDelegate, sessionImplementor );
	}

	@Override
	public MassIndexer createIndexer(Class<?>... types) {
		if ( types.length == 0 ) {
			// by default reindex all entities
			types = new Class<?>[] { Object.class };
		}

		return new MassIndexerImpl( sessionImplementor.getFactory(), sessionImplementor.getTenantIdentifier(), types );
	}

	@Override
	public PojoWorkPlan createWorkPlan() {
		return getDelegate().createWorkPlan();
	}

	@Override
	public PojoSessionWorkExecutor createSessionWorkExecutor() {
		return getDelegate().createSessionWorkExecutor();
	}

	public static class HibernateOrmSearchSessionBuilder extends AbstractBuilder<HibernateOrmSearchSession>
			implements SearchSessionBuilder {
		private final HibernateOrmMappingContextImpl mappingContext;
		private final SessionImplementor sessionImplementor;

		public HibernateOrmSearchSessionBuilder(PojoMappingDelegate mappingDelegate,
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
		public HibernateOrmSearchSession build() {
			return new HibernateOrmSearchSession( this );
		}
	}
}
