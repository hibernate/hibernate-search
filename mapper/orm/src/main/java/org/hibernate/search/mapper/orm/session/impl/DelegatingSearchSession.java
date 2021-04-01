/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.impl;

import java.util.Collection;
import java.util.function.Supplier;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.schema.management.SearchSchemaManager;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.search.loading.dsl.SearchLoadingOptionsStep;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.orm.work.SearchIndexingPlan;
import org.hibernate.search.mapper.orm.work.SearchWorkspace;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;

/**
 * A lazily initializing {@link SearchSession}.
 * <p>
 * This implementation allows to call {@link org.hibernate.search.mapper.orm.Search#session(Session)}
 * before Hibernate Search is fully initialized, which can be useful in CDI/Spring environments.
 */
public class DelegatingSearchSession implements SearchSession {

	private final Supplier<? extends HibernateOrmSearchSessionMappingContext> mappingContextProvider;
	private final Session session;

	public DelegatingSearchSession(Supplier<? extends HibernateOrmSearchSessionMappingContext> mappingContextProvider,
			Session session) {
		this.mappingContextProvider = mappingContextProvider;
		this.session = session;
	}

	@Override
	public <T> SearchQuerySelectStep<?, EntityReference, T, SearchLoadingOptionsStep, ?, ?> search(
			Collection<? extends Class<? extends T>> types) {
		return getDelegate().search( types );
	}

	@Override
	public <T> SearchQuerySelectStep<?, EntityReference, T, SearchLoadingOptionsStep, ?, ?> search(
			SearchScope<T> scope) {
		return getDelegate().search( scope );
	}

	@Override
	public SearchSchemaManager schemaManager(Collection<? extends Class<?>> types) {
		return getDelegate().schemaManager( types );
	}

	@Override
	public SearchWorkspace workspace(Collection<? extends Class<?>> types) {
		return getDelegate().workspace( types );
	}

	@Override
	public MassIndexer massIndexer(Collection<? extends Class<?>> types) {
		return getDelegate().massIndexer( types );
	}

	@Override
	public <T> SearchScope<T> scope(Collection<? extends Class<? extends T>> types) {
		return getDelegate().scope( types );
	}

	@Override
	public <T> SearchScope<T> scope(Class<T> expectedSuperType, Collection<String> entityNames) {
		return getDelegate().scope( expectedSuperType, entityNames );
	}

	@Override
	public EntityManager toEntityManager() {
		return session;
	}

	@Override
	public Session toOrmSession() {
		return session;
	}

	@Override
	public SearchIndexingPlan indexingPlan() {
		return getDelegate().indexingPlan();
	}

	@Override
	public void automaticIndexingSynchronizationStrategy(
			AutomaticIndexingSynchronizationStrategy synchronizationStrategy) {
		getDelegate().automaticIndexingSynchronizationStrategy( synchronizationStrategy );
	}

	private HibernateOrmSearchSession getDelegate() {
		// We cannot cache this session implementor, nor the resulting delegate,
		// because the session may be a proxy that returns a different session based
		// on the current thread (Spring, SessionFactory.getCurrentSession(), ...)
		// See https://hibernate.atlassian.net/browse/HSEARCH-4108
		SessionImplementor sessionImpl = HibernateOrmUtils.toSessionImplementor( session );
		return HibernateOrmSearchSession.get( mappingContextProvider.get(), sessionImpl );
	}
}
