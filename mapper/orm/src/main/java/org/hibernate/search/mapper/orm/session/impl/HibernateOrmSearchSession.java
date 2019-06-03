/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collection;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.search.SearchScope;
import org.hibernate.search.mapper.orm.scope.impl.SearchScopeImpl;
import org.hibernate.search.mapper.orm.session.AutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.orm.session.spi.SearchSessionImplementor;
import org.hibernate.search.mapper.orm.session.spi.SearchSessionBuilder;
import org.hibernate.search.mapper.orm.mapping.context.impl.HibernateOrmMappingContextImpl;
import org.hibernate.search.mapper.orm.session.context.impl.HibernateOrmSessionContextImpl;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkPlan;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.session.spi.AbstractPojoSearchSession;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;
import org.hibernate.search.mapper.pojo.work.spi.PojoSessionWorkExecutor;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * The actual implementation of {@link SearchSession}.
 */
public class HibernateOrmSearchSession extends AbstractPojoSearchSession
		implements SearchSessionImplementor, SearchSession {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final HibernateOrmSessionContextImpl sessionContext;
	private AutomaticIndexingSynchronizationStrategy synchronizationStrategy;

	private HibernateOrmSearchSession(HibernateOrmSearchSessionBuilder builder) {
		this( builder, builder.buildSessionContext() );
	}

	private HibernateOrmSearchSession(HibernateOrmSearchSessionBuilder builder,
			HibernateOrmSessionContextImpl sessionContext) {
		super( builder, sessionContext );
		this.sessionContext = sessionContext;
		this.synchronizationStrategy = builder.synchronizationStrategy;
	}

	@Override
	public void close() {
		// Nothing to do
	}

	@Override
	public EntityManager toEntityManager() {
		return sessionContext.getSession();
	}

	@Override
	public Session toOrmSession() {
		return sessionContext.getSession();
	}

	@Override
	public <T> SearchScope<T> scope(Collection<? extends Class<? extends T>> types) {
		checkOrmSessionIsOpen();

		PojoScopeDelegate<T, T> scopeDelegate = getDelegate().createPojoScope( types );
		return new SearchScopeImpl<>( scopeDelegate, sessionContext );
	}

	@Override
	public PojoWorkPlan createWorkPlan(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return getDelegate().createWorkPlan( commitStrategy, refreshStrategy );
	}

	@Override
	public PojoSessionWorkExecutor createSessionWorkExecutor(DocumentCommitStrategy commitStrategy) {
		return getDelegate().createSessionWorkExecutor( commitStrategy );
	}

	@Override
	public void setAutomaticIndexingSynchronizationStrategy(
			AutomaticIndexingSynchronizationStrategy synchronizationStrategy) {
		this.synchronizationStrategy = synchronizationStrategy;
	}

	@Override
	public AutomaticIndexingSynchronizationStrategy getAutomaticIndexingSynchronizationStrategy() {
		return synchronizationStrategy;
	}

	private void checkOrmSessionIsOpen() {
		try {
			sessionContext.getSession().checkOpen();
		}
		catch (IllegalStateException e) {
			throw log.hibernateSessionIsClosed( e );
		}
	}

	public static class HibernateOrmSearchSessionBuilder extends AbstractBuilder<HibernateOrmSearchSession>
			implements SearchSessionBuilder {
		private final HibernateOrmMappingContextImpl mappingContext;
		private final SessionImplementor sessionImplementor;
		private final AutomaticIndexingSynchronizationStrategy synchronizationStrategy;

		public HibernateOrmSearchSessionBuilder(PojoMappingDelegate mappingDelegate,
				HibernateOrmMappingContextImpl mappingContext,
				SessionImplementor sessionImplementor,
				AutomaticIndexingSynchronizationStrategy synchronizationStrategy) {
			super( mappingDelegate );
			this.mappingContext = mappingContext;
			this.sessionImplementor = sessionImplementor;
			this.synchronizationStrategy = synchronizationStrategy;
		}

		private HibernateOrmSessionContextImpl buildSessionContext() {
			return new HibernateOrmSessionContextImpl( mappingContext, sessionImplementor );
		}

		@Override
		public HibernateOrmSearchSession build() {
			return new HibernateOrmSearchSession( this );
		}
	}
}
