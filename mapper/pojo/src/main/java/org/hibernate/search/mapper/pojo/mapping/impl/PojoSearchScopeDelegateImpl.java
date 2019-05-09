/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexSearchScope;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexSearchScopeBuilder;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.mapper.pojo.search.spi.PojoSearchScopeDelegate;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;
import org.hibernate.search.mapper.pojo.mapping.context.spi.AbstractPojoMappingContextImplementor;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.util.common.AssertionFailure;

class PojoSearchScopeDelegateImpl<E, E2> implements PojoSearchScopeDelegate<E, E2> {

	private final PojoIndexedTypeManagerContainer typeManagers;
	private final Set<PojoIndexedTypeManager<?, ? extends E, ?>> targetedTypeManagers;
	private final AbstractPojoSessionContextImplementor sessionContext;
	private MappedIndexSearchScope<PojoReference, E2> delegate;

	PojoSearchScopeDelegateImpl(PojoIndexedTypeManagerContainer typeManagers,
			Set<PojoIndexedTypeManager<?, ? extends E, ?>> targetedTypeManagers,
			AbstractPojoSessionContextImplementor sessionContext) {
		this.typeManagers = typeManagers;
		this.targetedTypeManagers = targetedTypeManagers;
		this.sessionContext = sessionContext;
	}

	@Override
	public Set<Class<? extends E>> getIncludedIndexedTypes() {
		return targetedTypeManagers.stream()
				.map( PojoIndexedTypeManager::getIndexedJavaClass )
				.collect( Collectors.toCollection( LinkedHashSet::new ) );
	}

	@Override
	public PojoReference toPojoReference(DocumentReference documentReference) {
		PojoIndexedTypeManager<?, ?, ?> typeManager = typeManagers.getByIndexName( documentReference.getIndexName() )
				.orElseThrow( () -> new AssertionFailure(
						"Document reference " + documentReference + " could not be converted to a PojoReference" ) );
		// TODO error handling if typeManager is null
		Object id = typeManager.getIdentifierMapping().fromDocumentIdentifier( documentReference.getId(), sessionContext );
		return new PojoReferenceImpl( typeManager.getIndexedJavaClass(), id );
	}

	@Override
	public SearchQueryResultDefinitionContext<PojoReference, E2, SearchProjectionFactoryContext<PojoReference, E2>> search(
			LoadingContextBuilder<PojoReference, E2> loadingContextBuilder) {
		return getDelegate().search( sessionContext, loadingContextBuilder );
	}

	@Override
	public SearchPredicateFactoryContext predicate() {
		return getDelegate().predicate();
	}

	@Override
	public SearchSortContainerContext sort() {
		return getDelegate().sort();
	}

	@Override
	public SearchProjectionFactoryContext<PojoReference, E2> projection() {
		return getDelegate().projection();
	}

	private MappedIndexSearchScope<PojoReference, E2> getDelegate() {
		AbstractPojoMappingContextImplementor mappingContext = sessionContext.getMappingContext();
		if ( delegate == null ) {
			Iterator<PojoIndexedTypeManager<?, ? extends E, ?>> iterator = targetedTypeManagers.iterator();
			MappedIndexSearchScopeBuilder<PojoReference, E2> builder = iterator.next().createSearchScopeBuilder(
					mappingContext
			);
			while ( iterator.hasNext() ) {
				iterator.next().addTo( builder );
			}
			delegate = builder.build();
		}
		return delegate;
	}
}
