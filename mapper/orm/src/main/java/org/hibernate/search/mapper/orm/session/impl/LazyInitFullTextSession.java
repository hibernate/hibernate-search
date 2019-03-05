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
import org.hibernate.search.mapper.orm.search.FullTextSearchTarget;
import org.hibernate.search.mapper.orm.session.FullTextSession;
import org.hibernate.search.mapper.orm.session.spi.FullTextSessionImplementor;

/**
 * A lazily initializing {@link FullTextSession}.
 * <p>
 * This implementation allows to call {@link org.hibernate.search.mapper.orm.Search#getFullTextSession(Session)}
 * before Hibernate Search is fully initialized, which can be useful in CDI/Spring environments.
 */
public class LazyInitFullTextSession implements FullTextSession {

	private final SessionImplementor sessionImplementor;
	private FullTextSessionImplementor delegate;

	public LazyInitFullTextSession(SessionImplementor sessionImplementor) {
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
	public <T> FullTextSearchTarget<T> target(Collection<? extends Class<? extends T>> types) {
		return getDelegate().target( types );
	}

	@Override
	public MassIndexer createIndexer(Class<?>... types) {
		return getDelegate().createIndexer( types );
	}

	private FullTextSessionImplementor getDelegate() {
		if ( delegate == null ) {
			HibernateSearchContextService contextService = sessionImplementor.getSessionFactory().getServiceRegistry()
					.getService( HibernateSearchContextService.class );
			delegate = contextService.getFullTextSession( sessionImplementor );
		}
		return delegate;
	}
}
