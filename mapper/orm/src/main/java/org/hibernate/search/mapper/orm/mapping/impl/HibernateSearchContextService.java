/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.BaseSessionEventListener;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.mapping.spi.HibernateOrmMapping;
import org.hibernate.search.mapper.orm.session.spi.SearchSessionImplementor;
import org.hibernate.search.util.common.impl.TransientReference;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.service.Service;

public final class HibernateSearchContextService implements Service, AutoCloseable {
	private static final String SEARCH_SESSION_KEY =
			HibernateSearchContextService.class.getName() + "#SEARCH_SESSION_KEY";

	public static HibernateSearchContextService get(SessionFactoryImplementor sessionFactory) {
		return sessionFactory.getServiceRegistry().getService( HibernateSearchContextService.class );
	}

	private volatile SearchIntegration integration;
	private volatile HibernateOrmMapping mapping;

	@Override
	public void close() {
		if ( integration != null ) {
			integration.close();
		}
	}

	public void initialize(SearchIntegration integration, HibernateOrmMapping mapping) {
		this.integration = integration;
		this.mapping = mapping;
	}

	public SearchIntegration getIntegration() {
		if ( integration != null ) {
			return integration;
		}
		else {
			throw LoggerFactory.make( Log.class, MethodHandles.lookup() ).hibernateSearchNotInitialized();
		}
	}

	public HibernateOrmMapping getMapping() {
		if ( mapping != null ) {
			return mapping;
		}
		else {
			throw LoggerFactory.make( Log.class, MethodHandles.lookup() ).hibernateSearchNotInitialized();
		}
	}

	/**
	 * @param sessionImplementor A Hibernate session
	 *
	 * @return The {@link SearchSessionImplementor} to use within the context of the given session.
	 */
	@SuppressWarnings("unchecked")
	public SearchSessionImplementor getSearchSession(SessionImplementor sessionImplementor) {
		TransientReference<SearchSessionImplementor> reference =
				(TransientReference<SearchSessionImplementor>) sessionImplementor.getProperties().get(
						SEARCH_SESSION_KEY );
		@SuppressWarnings("resource") // The listener below handles closing
				SearchSessionImplementor searchSession = reference == null ? null : reference.get();
		if ( searchSession == null ) {
			searchSession = getMapping().createSession( sessionImplementor );
			reference = new TransientReference<>( searchSession );
			sessionImplementor.setProperty( SEARCH_SESSION_KEY, reference );

			// Make sure we will ultimately close the query manager
			sessionImplementor.getEventListenerManager().addListener( new SearchSessionClosingListener( sessionImplementor ) );
		}
		return searchSession;
	}

	private static class SearchSessionClosingListener extends BaseSessionEventListener {
		private final SessionImplementor sessionImplementor;

		private SearchSessionClosingListener(SessionImplementor sessionImplementor) {
			this.sessionImplementor = sessionImplementor;
		}

		@Override
		public void end() {
			@SuppressWarnings("unchecked") // This key "belongs" to us, we know what we put in there.
					TransientReference<SearchSessionImplementor> reference =
					(TransientReference<SearchSessionImplementor>) sessionImplementor.getProperties().get(
							SEARCH_SESSION_KEY );
			SearchSessionImplementor searchSession = reference == null ? null : reference.get();
			if ( searchSession != null ) {
				searchSession.close();
			}
		}
	}
}
