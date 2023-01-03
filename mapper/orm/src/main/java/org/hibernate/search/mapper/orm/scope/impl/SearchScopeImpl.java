/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.scope.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.hibernate.search.engine.backend.scope.IndexScopeExtension;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmSelectionLoadingContext;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.massindexing.impl.HibernateOrmMassIndexer;
import org.hibernate.search.mapper.orm.massindexing.impl.HibernateOrmMassIndexingContext;
import org.hibernate.search.mapper.orm.schema.management.SearchSchemaManager;
import org.hibernate.search.mapper.orm.schema.management.impl.SearchSchemaManagerImpl;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.search.loading.dsl.SearchLoadingOptionsStep;
import org.hibernate.search.mapper.orm.tenancy.spi.TenancyConfiguration;
import org.hibernate.search.mapper.orm.work.SearchWorkspace;
import org.hibernate.search.mapper.orm.work.impl.SearchWorkspaceImpl;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexer;
import org.hibernate.search.mapper.pojo.schema.management.spi.PojoScopeSchemaManager;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;

public class SearchScopeImpl<E> implements SearchScope<E> {

	private final HibernateOrmScopeMappingContext mappingContext;
	private final TenancyConfiguration tenancyConfiguration;
	private final PojoScopeDelegate<EntityReference, E, HibernateOrmScopeIndexedTypeContext<? extends E>> delegate;

	public SearchScopeImpl(HibernateOrmScopeMappingContext mappingContext,
			TenancyConfiguration tenancyConfiguration, PojoScopeDelegate<EntityReference, E, HibernateOrmScopeIndexedTypeContext<? extends E>> delegate) {
		this.mappingContext = mappingContext;
		this.tenancyConfiguration = tenancyConfiguration;
		this.delegate = delegate;
	}

	public SearchQuerySelectStep<?, EntityReference, E, SearchLoadingOptionsStep, ?, ?> search(
			HibernateOrmScopeSessionContext sessionContext, HibernateOrmSelectionLoadingContext.Builder loadingContextBuilder) {
		return delegate.search( sessionContext, sessionContext.documentReferenceConverter(),
				loadingContextBuilder );
	}

	@Override
	public SearchPredicateFactory predicate() {
		return delegate.predicate();
	}

	@Override
	public SearchSortFactory sort() {
		return delegate.sort();
	}

	@Override
	public SearchProjectionFactory<EntityReference, E> projection() {
		return delegate.projection();
	}

	@Override
	public SearchAggregationFactory aggregation() {
		return delegate.aggregation();
	}

	@Override
	public SearchSchemaManager schemaManager() {
		return new SearchSchemaManagerImpl( schemaManagerDelegate() );
	}

	@Override
	public SearchWorkspace workspace() {
		return workspace( (String) null );
	}

	@Override
	public SearchWorkspace workspace(String tenantId) {
		return new SearchWorkspaceImpl( delegate.workspace( tenantId ) );
	}

	@Override
	public MassIndexer massIndexer() {
		return massIndexer( Collections.<String>emptyList() );
	}

	@Override
	public MassIndexer massIndexer(String tenantId) {
		return massIndexer( Collections.singletonList( tenantId ) );
	}

	@Override
	public MassIndexer massIndexer(Collection<String> tenantIds) {
		if ( tenantIds.isEmpty() ) {
			// Let's see if we are in multi-tenant environment and try to get the tenant ids
			Set<String> configuredTenants = tenancyConfiguration.tenantIdsOrFail();
			if ( configuredTenants.isEmpty() ) {
				// if we didn't fail with exception - single tenant is used so just create an empty session:
				tenantIds = Collections.singletonList( null );
			}
			else {
				tenantIds = configuredTenants;
			}
		}

		HibernateOrmMassIndexingContext massIndexingContext = new HibernateOrmMassIndexingContext( mappingContext,
				mappingContext.typeContextProvider() );

		PojoMassIndexer massIndexerDelegate = delegate
				.massIndexer( massIndexingContext, tenantIds );

		return new HibernateOrmMassIndexer( massIndexerDelegate, massIndexingContext );
	}

	@Override
	public Set<? extends SearchIndexedEntity<? extends E>> includedTypes() {
		return delegate.includedIndexedTypes();
	}

	@Override
	public <T> T extension(IndexScopeExtension<T> extension) {
		return delegate.extension( extension );
	}

	public PojoScopeSchemaManager schemaManagerDelegate() {
		return delegate.schemaManager();
	}
}
