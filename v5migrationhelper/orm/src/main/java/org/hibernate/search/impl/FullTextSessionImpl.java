/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.impl;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionDelegatorBaseImpl;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.FullTextSharedSessionBuilder;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.hcore.util.impl.ContextHelper;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.search.loading.dsl.SearchLoadingOptionsStep;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.massindexing.impl.V5MigrationMassIndexerAdapter;
import org.hibernate.search.query.engine.spi.V5MigrationSearchSession;
import org.hibernate.search.query.hibernate.impl.FullTextQueryImpl;
import org.hibernate.search.scope.impl.V5MigrationOrmSearchScopeAdapter;
import org.hibernate.search.scope.spi.V5MigrationSearchScope;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Lucene full text search aware session.
 *
 * @author Emmanuel Bernard
 * @author John Griffin
 * @author Hardy Ferentschik
 */
final class FullTextSessionImpl extends SessionDelegatorBaseImpl
		implements FullTextSession, SessionImplementor, V5MigrationSearchSession<SearchLoadingOptionsStep> {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private static SessionImplementor doUnwrap(Session session) {
		if ( session == null ) {
			throw log.getNullSessionPassedToFullTextSessionCreationException();
		}
		// Keeping both instanceofs in case the interface hierarchy changes.
		else if ( session instanceof SessionImplementor && session instanceof EventSource ) {
			// A session proxied with ThreadLocalSessionContext will implement all the interfaces we need,
			// but won't allow a call to .unwrap() outside of a transaction.
			// Thus we need to proceed with the cast.
			return (EventSource) session;
		}
		else {
			// Other proxies (such as Spring's after version 2.4),
			// only implement Session, not SessionImplementor,
			// but allow a call to .unwrap().
			return session.unwrap( SessionImplementor.class );
		}
	}


	private transient V5MigrationOrmSearchIntegratorAdapter searchIntegrator;
	private transient SearchFactory searchFactoryAPI;

	public FullTextSessionImpl(org.hibernate.Session session) {
		super( doUnwrap( session ) );
	}

	@Override
	public FullTextQuery createFullTextQuery(org.apache.lucene.search.Query luceneQuery, Class<?>... entities) {
		return new FullTextQueryImpl( luceneQuery, delegate, getSearchIntegrator(), this, entities );
	}

	@Override
	public <T> void purgeAll(Class<T> entityType) {
		searchSession().workspace( entityType ).purge();
	}

	@Override
	public void flushToIndexes() {
		searchSession().indexingPlan().execute();
	}

	@Override
	public <T> void purge(Class<T> entityType, Serializable id) {
		if ( entityType == null ) {
			return;
		}
		if ( id == null ) {
			// Search 5 behavior: if id is null, the method call is interpreted as purgeAll.
			purgeAll( entityType );
		}
		else {
			searchSession().indexingPlan().purge( entityType, id, null );
		}
	}

	@Override
	public <T> void index(T entity) {
		if ( entity == null ) {
			throw new IllegalArgumentException( "Entity to index should not be null" );
		}
		searchSession().indexingPlan().addOrUpdate( entity );
	}

	@Override
	public MassIndexer createIndexer(Class<?>... types) {
		return new V5MigrationMassIndexerAdapter( types == null || types.length == 0
				? searchSession().massIndexer( Object.class )
				: searchSession().massIndexer( types ) );
	}

	@Override
	public SearchFactory getSearchFactory() {
		if ( searchFactoryAPI == null ) {
			searchFactoryAPI = new SearchFactoryImpl( getSearchIntegrator() );
		}
		return searchFactoryAPI;
	}

	private V5MigrationOrmSearchIntegratorAdapter getSearchIntegrator() {
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

	@Override
	public SearchQuerySelectStep<?, ?, ?, SearchLoadingOptionsStep, ?, ?> search(V5MigrationSearchScope scope) {
		SearchSession searchSession = searchSession();
		return searchSession.search( ( (V5MigrationOrmSearchScopeAdapter) scope ).toSearchScope() );
	}

	private SearchSession searchSession() {
		return Search.session( delegate );
	}
}
