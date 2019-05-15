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
import org.hibernate.search.engine.backend.index.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.index.DocumentRefreshStrategy;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.massindexing.impl.MassIndexerImpl;
import org.hibernate.search.mapper.orm.search.SearchScope;
import org.hibernate.search.mapper.orm.search.impl.SearchScopeImpl;
import org.hibernate.search.mapper.orm.session.AutomaticIndexingSynchronizationStrategy;
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
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * The actual implementation of {@link SearchSession}.
 */
public class HibernateOrmSearchSession extends AbstractPojoSearchSession
		implements SearchSessionImplementor, SearchSession {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SessionImplementor sessionImplementor;
	private AutomaticIndexingSynchronizationStrategy synchronizationStrategy;

	private HibernateOrmSearchSession(HibernateOrmSearchSessionBuilder builder) {
		super( builder );
		this.sessionImplementor = builder.sessionImplementor;
		this.synchronizationStrategy = builder.synchronizationStrategy;
	}

	@Override
	public void close() {
		// Nothing to do
	}

	@Override
	public EntityManager toEntityManager() {
		return sessionImplementor;
	}

	@Override
	public Session toOrmSession() {
		return sessionImplementor;
	}

	@Override
	public <T> SearchScope<T> scope(Collection<? extends Class<? extends T>> types) {
		checkOrmSessionIsOpen();

		PojoSearchScopeDelegate<T, T> searchScopeDelegate = getDelegate().createPojoSearchScope( types );
		return new SearchScopeImpl<>( searchScopeDelegate, sessionImplementor );
	}

	@Override
	public MassIndexer createIndexer(Class<?>... types) {
		checkOrmSessionIsOpen();

		if ( types.length == 0 ) {
			// by default reindex all entities
			types = new Class<?>[] { Object.class };
		}

		return new MassIndexerImpl( sessionImplementor.getFactory(), sessionImplementor.getTenantIdentifier(), types );
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
			sessionImplementor.checkOpen();
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
