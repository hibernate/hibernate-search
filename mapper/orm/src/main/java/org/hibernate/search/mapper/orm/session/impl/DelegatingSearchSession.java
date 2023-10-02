/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.session.impl;

import java.util.Collection;
import java.util.function.Supplier;

import jakarta.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.schema.management.SearchSchemaManager;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.search.loading.dsl.SearchLoadingOptionsStep;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.orm.work.SearchIndexingPlan;
import org.hibernate.search.mapper.orm.work.SearchWorkspace;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.SearchIndexingPlanFilter;

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
	@SuppressWarnings("deprecation")
	public <T> SearchQuerySelectStep<?,
			org.hibernate.search.mapper.orm.common.EntityReference,
			T,
			SearchLoadingOptionsStep,
			?,
			?> search(
					Collection<? extends Class<? extends T>> classes) {
		return getDelegate().search( classes );
	}

	@Override
	@SuppressWarnings("deprecation")
	public <T> SearchQuerySelectStep<?,
			org.hibernate.search.mapper.orm.common.EntityReference,
			T,
			SearchLoadingOptionsStep,
			?,
			?> search(
					SearchScope<T> scope) {
		return getDelegate().search( scope );
	}

	@Override
	public SearchSchemaManager schemaManager(Collection<? extends Class<?>> classes) {
		return getDelegate().schemaManager( classes );
	}

	@Override
	public SearchWorkspace workspace(Collection<? extends Class<?>> classes) {
		return getDelegate().workspace( classes );
	}

	@Override
	public MassIndexer massIndexer(Collection<? extends Class<?>> classes) {
		return getDelegate().massIndexer( classes );
	}

	@Override
	public <T> SearchScope<T> scope(Collection<? extends Class<? extends T>> classes) {
		return getDelegate().scope( classes );
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
	@SuppressWarnings("deprecation")
	public void automaticIndexingSynchronizationStrategy(
			org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy synchronizationStrategy) {
		getDelegate().automaticIndexingSynchronizationStrategy( synchronizationStrategy );
	}

	@Override
	public void indexingPlanSynchronizationStrategy(IndexingPlanSynchronizationStrategy synchronizationStrategy) {
		getDelegate().indexingPlanSynchronizationStrategy( synchronizationStrategy );
	}

	@Override
	public void indexingPlanFilter(SearchIndexingPlanFilter filter) {
		getDelegate().indexingPlanFilter( filter );
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
