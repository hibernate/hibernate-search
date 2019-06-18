/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.lang.invoke.MethodHandles;
import javax.persistence.EntityManager;

import org.hibernate.BaseSessionEventListener;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.mapping.context.impl.HibernateOrmMappingContextImpl;
import org.hibernate.search.mapper.orm.mapping.spi.HibernateOrmMapping;
import org.hibernate.search.mapper.orm.massindexing.impl.HibernateOrmMassIndexingMappingContext;
import org.hibernate.search.mapper.orm.session.AutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSearchSession;
import org.hibernate.search.mapper.pojo.mapping.spi.AbstractPojoMappingImplementor;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.work.spi.PojoSessionWorkExecutor;
import org.hibernate.search.util.common.impl.TransientReference;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class HibernateOrmMappingImpl extends AbstractPojoMappingImplementor<HibernateOrmMappingImpl>
		implements HibernateOrmMapping, HibernateOrmMassIndexingMappingContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String SEARCH_SESSION_KEY =
			HibernateOrmMappingImpl.class.getName() + "#SEARCH_SESSION_KEY";

	private final HibernateOrmMappingContextImpl mappingContext;
	private final HibernateOrmTypeContextContainer typeContextContainer;
	private final AutomaticIndexingSynchronizationStrategy synchronizationStrategy;

	HibernateOrmMappingImpl(PojoMappingDelegate mappingDelegate,
			HibernateOrmTypeContextContainer typeContextContainer,
			SessionFactoryImplementor sessionFactoryImplementor,
			AutomaticIndexingSynchronizationStrategy synchronizationStrategy) {
		super( mappingDelegate );
		this.typeContextContainer = typeContextContainer;
		this.mappingContext = new HibernateOrmMappingContextImpl( sessionFactoryImplementor );
		this.synchronizationStrategy = synchronizationStrategy;
	}

	@Override
	public HibernateOrmMappingImpl toConcreteType() {
		return this;
	}

	/**
	 * @param sessionImplementor A Hibernate session
	 *
	 * @return The {@link HibernateOrmSearchSession} to use within the context of the given session.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public HibernateOrmSearchSession getSearchSession(SessionImplementor sessionImplementor) {
		TransientReference<HibernateOrmSearchSession> reference =
				(TransientReference<HibernateOrmSearchSession>) sessionImplementor.getProperties()
						.get( SEARCH_SESSION_KEY );
		@SuppressWarnings("resource") // The listener below handles closing
		HibernateOrmSearchSession searchSession = reference == null ? null : reference.get();
		if ( searchSession == null ) {
			searchSession = createSessionBuilder( sessionImplementor ).build();
			reference = new TransientReference<>( searchSession );
			sessionImplementor.setProperty( SEARCH_SESSION_KEY, reference );

			// Make sure we will ultimately close the query manager
			sessionImplementor.getEventListenerManager()
					.addListener( new SearchSessionClosingListener( sessionImplementor ) );
		}
		return searchSession;
	}

	@Override
	public PojoSessionWorkExecutor createSessionWorkExecutor(SessionImplementor sessionImplementor,
			DocumentCommitStrategy commitStrategy) {
		return getSearchSession( sessionImplementor ).createSessionWorkExecutor( commitStrategy );
	}

	<E> AbstractHibernateOrmTypeContext<E> getTypeContext(Class<E> type) {
		return typeContextContainer.getByExactClass( type );
	}

	private HibernateOrmSearchSession.HibernateOrmSearchSessionBuilder createSessionBuilder(EntityManager entityManager) {
		SessionImplementor sessionImplementor = null;
		try {
			sessionImplementor = entityManager.unwrap( SessionImplementor.class );
		}
		catch (IllegalStateException e) {
			throw log.hibernateSessionAccessError( e );
		}

		SessionFactory expectedSessionFactory = mappingContext.getSessionFactory();
		SessionFactory givenSessionFactory = sessionImplementor.getSessionFactory();

		if ( !givenSessionFactory.equals( expectedSessionFactory ) ) {
			throw log.usingDifferentSessionFactories( expectedSessionFactory, givenSessionFactory );
		}

		return new HibernateOrmSearchSession.HibernateOrmSearchSessionBuilder(
				getDelegate(), mappingContext, this, typeContextContainer,
				sessionImplementor,
				synchronizationStrategy
		);
	}

	private static class SearchSessionClosingListener extends BaseSessionEventListener {
		private final SessionImplementor sessionImplementor;

		private SearchSessionClosingListener(SessionImplementor sessionImplementor) {
			this.sessionImplementor = sessionImplementor;
		}

		@Override
		public void end() {
			@SuppressWarnings("unchecked") // This key "belongs" to us, we know what we put in there.
			TransientReference<HibernateOrmSearchSession> reference =
					(TransientReference<HibernateOrmSearchSession>) sessionImplementor.getProperties()
							.get( SEARCH_SESSION_KEY );
			HibernateOrmSearchSession searchSession = reference == null ? null : reference.get();
			if ( searchSession != null ) {
				searchSession.close();
			}
		}
	}
}
