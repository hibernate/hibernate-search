/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.impl;

import java.io.Serializable;

import org.hibernate.engine.spi.SessionDelegatorBaseImpl;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.FullTextSharedSessionBuilder;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.hcore.util.impl.ContextHelper;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.hibernate.impl.FullTextQueryImpl;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

/**
 * Lucene full text search aware session.
 *
 * @author Emmanuel Bernard
 * @author John Griffin
 * @author Hardy Ferentschik
 */
final class FullTextSessionImpl extends SessionDelegatorBaseImpl implements FullTextSession, SessionImplementor {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private transient SearchIntegrator searchIntegrator;
	private transient SearchFactory searchFactoryAPI;

	public FullTextSessionImpl(org.hibernate.Session session) {
		super( (SessionImplementor) session );
	}

	@Override
	public FullTextQuery createFullTextQuery(org.apache.lucene.search.Query luceneQuery, Class<?>... entities) {
		// TODO pass the session and loading options contributor here
		HSQuery hsQuery = getSearchIntegrator().createHSQuery( luceneQuery, null, null, entities );
		return createFullTextQuery( hsQuery );
	}

	private FullTextQuery createFullTextQuery(HSQuery hsQuery) {
		return new FullTextQueryImpl( hsQuery, delegate );
	}

	@Override
	public <T> void purgeAll(Class<T> entityType) {
		purge( entityType, null );
	}

	@Override
	public void flushToIndexes() {
		throw new UnsupportedOperationException( "To be implemented by delegating to Search 6 APIs." );
	}

	@Override
	public <T> void purge(Class<T> entityType, Serializable id) {
		if ( entityType == null ) {
			return;
		}
		throw new UnsupportedOperationException( "To be implemented by delegating to Search 6 APIs." );
	}

	@Override
	public <T> void index(T entity) {
		if ( entity == null ) {
			throw new IllegalArgumentException( "Entity to index should not be null" );
		}

		throw new UnsupportedOperationException( "To be implemented by delegating to Search 6 APIs." );
	}

	@Override
	public MassIndexer createIndexer(Class<?>... types) {
		throw new UnsupportedOperationException( "To be implemented by delegating to Search 6 APIs." );
	}

	@Override
	public SearchFactory getSearchFactory() {
		if ( searchFactoryAPI == null ) {
			searchFactoryAPI = new SearchFactoryImpl( getSearchIntegrator() );
		}
		return searchFactoryAPI;
	}

	private SearchIntegrator getSearchIntegrator() {
		if ( searchIntegrator == null ) {
			searchIntegrator = ContextHelper.getSearchIntegrator( delegate );
		}
		return searchIntegrator;
	}

	@Override
	public FullTextSharedSessionBuilder sessionWithOptions() {
		return new FullTextSharedSessionBuilderDelegator( super.sessionWithOptions() );
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

}
