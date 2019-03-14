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
import org.hibernate.search.mapper.orm.impl.HibernateSearchContextService;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.search.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
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
	public EntityManager toJpaEntityManager() {
		return getDelegate().toJpaEntityManager();
	}

	@Override
	public Session toHibernateOrmSession() {
		return getDelegate().toHibernateOrmSession();
	}

	@Override
	public <T> SearchScope<T> scope(Collection<? extends Class<? extends T>> types) {
		return getDelegate().scope( types );
	}

	@Override
	public MassIndexer createIndexer(Class<?>... types) {
		return getDelegate().createIndexer( types );
	}

	private SearchSessionImplementor getDelegate() {
		if ( delegate == null ) {
			HibernateSearchContextService contextService = sessionImplementor.getSessionFactory().getServiceRegistry()
					.getService( HibernateSearchContextService.class );
			delegate = contextService.getSearchSession( sessionImplementor );
		}
		return delegate;
	}
}
