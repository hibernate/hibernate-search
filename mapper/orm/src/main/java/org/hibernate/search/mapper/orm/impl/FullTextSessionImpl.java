/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.impl;

import java.util.Collection;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.hibernate.FullTextSearchTarget;
import org.hibernate.search.mapper.orm.hibernate.FullTextSession;
import org.hibernate.search.mapper.orm.massindexing.impl.MassIndexerImpl;
import org.hibernate.search.mapper.orm.session.spi.HibernateOrmSearchManager;

public class FullTextSessionImpl implements FullTextSession {

	private final SessionImplementor delegate;

	private HibernateOrmSearchManager searchManager = null;

	public FullTextSessionImpl(SessionImplementor delegate) {
		this.delegate = delegate;
	}

	@Override
	public EntityManager toJpaEntityManager() {
		return delegate;
	}

	@Override
	public Session toHibernateOrmSession() {
		return delegate;
	}

	@Override
	public final <T> FullTextSearchTarget<T> search(Class<T> type) {
		return new FullTextSearchTargetImpl<>( getSearchManager().search( type ) );
	}

	@Override
	public final <T> FullTextSearchTarget<T> search(Collection<? extends Class<? extends T>> types) {
		return new FullTextSearchTargetImpl<>( getSearchManager().search( types ) );
	}

	@Override
	public MassIndexer createIndexer(Class<?>... types) {
		if ( types.length == 0 ) {
			// by default reindex all entities
			types = new Class<?>[] { Object.class };
		}

		return new MassIndexerImpl( delegate.getFactory(), delegate.getTenantIdentifier(), types );
	}

	private HibernateOrmSearchManager getSearchManager() {
		if ( searchManager == null ) {
			HibernateSearchContextService contextService = delegate.getSessionFactory().getServiceRegistry()
					.getService( HibernateSearchContextService.class );
			searchManager = contextService.getSearchManager( delegate );
		}
		return searchManager;
	}
}
