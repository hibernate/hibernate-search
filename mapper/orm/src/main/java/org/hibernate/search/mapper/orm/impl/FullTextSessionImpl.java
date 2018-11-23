/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.impl;

import java.util.Collection;

import org.hibernate.engine.spi.SessionDelegatorBaseImpl;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.massindexing.MassIndexerWithTenant;
import org.hibernate.search.mapper.orm.hibernate.FullTextSearchTarget;
import org.hibernate.search.mapper.orm.hibernate.FullTextSession;
import org.hibernate.search.mapper.orm.jpa.FullTextEntityManager;
import org.hibernate.search.mapper.orm.massindexing.impl.MassIndexerImpl;
import org.hibernate.search.mapper.orm.session.spi.HibernateOrmSearchManager;

public class FullTextSessionImpl extends SessionDelegatorBaseImpl implements FullTextSession {

	private transient HibernateOrmSearchManager searchManager = null;

	public FullTextSessionImpl(SessionImplementor delegate) {
		super( delegate );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> type) {
		if ( type.equals( FullTextEntityManager.class ) ) {
			return (T) this;
		}
		else if ( type.equals( FullTextSession.class ) ) {
			return (T) this;
		}
		else {
			return super.unwrap( type );
		}
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

		MassIndexer massIndexer = new MassIndexerImpl( getFactory(), types );
		if ( massIndexer instanceof MassIndexerWithTenant ) {
			( (MassIndexerWithTenant) massIndexer ).tenantIdentifier( getTenantIdentifier() );
		}
		return massIndexer;
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
