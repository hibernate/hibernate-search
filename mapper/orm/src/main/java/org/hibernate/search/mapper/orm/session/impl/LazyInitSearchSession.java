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
import org.hibernate.search.mapper.orm.mapping.impl.HibernateSearchContextService;
import org.hibernate.search.mapper.orm.search.SearchScope;
import org.hibernate.search.mapper.orm.session.AutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.orm.session.SearchSessionWritePlan;
import org.hibernate.search.mapper.orm.session.spi.SearchSessionImplementor;

/**
 * A lazily initializing {@link SearchSession}.
 * <p>
 * This implementation allows to call {@link org.hibernate.search.mapper.orm.Search#getSearchSession(Session)}
 * before Hibernate Search is fully initialized, which can be useful in CDI/Spring environments.
 */
public class LazyInitSearchSession implements SearchSession {

	private final SessionImplementor sessionImplementor;
	private SearchSessionImplementor delegate;

	public LazyInitSearchSession(SessionImplementor sessionImplementor) {
		this.sessionImplementor = sessionImplementor;
	}

	@Override
	public EntityManager toEntityManager() {
		return getDelegate().toEntityManager();
	}

	@Override
	public Session toOrmSession() {
		return getDelegate().toOrmSession();
	}

	@Override
	public <T> SearchScope<T> scope(Collection<? extends Class<? extends T>> types) {
		return getDelegate().scope( types );
	}

	@Override
	public SearchSessionWritePlan writePlan() {
		return getDelegate().writePlan();
	}

	@Override
	public void setAutomaticIndexingSynchronizationStrategy(
			AutomaticIndexingSynchronizationStrategy synchronizationStrategy) {
		getDelegate().setAutomaticIndexingSynchronizationStrategy( synchronizationStrategy );
	}

	private SearchSessionImplementor getDelegate() {
		if ( delegate == null ) {
			HibernateSearchContextService contextService =
					HibernateSearchContextService.get( sessionImplementor.getSessionFactory() );
			delegate = contextService.getSearchSession( sessionImplementor );
		}
		return delegate;
	}
}
