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
import org.hibernate.search.mapper.orm.session.FullTextSession;
import org.hibernate.search.mapper.orm.session.spi.FullTextSessionImplementor;
import org.hibernate.search.mapper.orm.session.spi.FullTextSessionBuilder;
import org.hibernate.search.mapper.orm.mapping.context.impl.HibernateOrmMappingContextImpl;
import org.hibernate.search.mapper.orm.session.context.impl.HibernateOrmSessionContextImpl;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkPlan;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.session.spi.AbstractPojoSearchManager;
import org.hibernate.search.mapper.pojo.search.spi.PojoSearchScopeDelegate;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;
import org.hibernate.search.mapper.pojo.work.spi.PojoSessionWorkExecutor;

/**
 * The actual implementation of {@link FullTextSession}.
 */
public class HibernateOrmSearchManager extends AbstractPojoSearchManager
		implements FullTextSessionImplementor, FullTextSession {
	private final SessionImplementor sessionImplementor;

	private HibernateOrmSearchManager(HibernateOrmSearchManagerBuilder builder) {
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

	public static class HibernateOrmSearchManagerBuilder extends AbstractBuilder<HibernateOrmSearchManager>
			implements FullTextSessionBuilder {
		private final HibernateOrmMappingContextImpl mappingContext;
		private final SessionImplementor sessionImplementor;

		public HibernateOrmSearchManagerBuilder(PojoMappingDelegate mappingDelegate,
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
		public HibernateOrmSearchManager build() {
			return new HibernateOrmSearchManager( this );
		}
	}
}
