/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.scope.impl;

import static org.hibernate.search.util.common.impl.CollectionHelper.asSetIgnoreNull;

import java.util.Collections;
import java.util.Set;

import org.hibernate.search.engine.backend.scope.IndexScopeExtension;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.highlighter.dsl.SearchHighlighterFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
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

@SuppressWarnings("deprecation")
public class SearchScopeImpl<E> implements SearchScope<E> {

	private final HibernateOrmScopeMappingContext mappingContext;
	private final TenancyConfiguration tenancyConfiguration;
	private final PojoScopeDelegate<org.hibernate.search.mapper.orm.common.EntityReference, E, HibernateOrmScopeIndexedTypeContext<? extends E>> delegate;

	public SearchScopeImpl(HibernateOrmScopeMappingContext mappingContext,
			TenancyConfiguration tenancyConfiguration,
			PojoScopeDelegate<org.hibernate.search.mapper.orm.common.EntityReference, E, HibernateOrmScopeIndexedTypeContext<? extends E>> delegate) {
		this.mappingContext = mappingContext;
		this.tenancyConfiguration = tenancyConfiguration;
		this.delegate = delegate;
	}

	public SearchQuerySelectStep<?, org.hibernate.search.mapper.orm.common.EntityReference, E, SearchLoadingOptionsStep, ?, ?> search(
			HibernateOrmScopeSessionContext sessionContext, HibernateOrmSelectionLoadingContext.Builder loadingContextBuilder) {
		return delegate.search( sessionContext, loadingContextBuilder );
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
	public SearchProjectionFactory<org.hibernate.search.mapper.orm.common.EntityReference, E> projection() {
		return delegate.projection();
	}

	@Override
	public SearchAggregationFactory aggregation() {
		return delegate.aggregation();
	}

	@Override
	public SearchHighlighterFactory highlighter() {
		return delegate.highlighter();
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
		return massIndexer( Collections.<String>emptySet() );
	}

	@Override
	public MassIndexer massIndexer(String tenantId) {
		return massIndexer( asSetIgnoreNull( tenantId ) );
	}

	@Override
	public MassIndexer massIndexer(Set<String> tenantIds) {
		if ( tenantIds.isEmpty() ) {
			// Let's see if we are in multi-tenant environment and try to get the tenant ids
			tenantIds = tenancyConfiguration.tenantIdsOrFail();
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
