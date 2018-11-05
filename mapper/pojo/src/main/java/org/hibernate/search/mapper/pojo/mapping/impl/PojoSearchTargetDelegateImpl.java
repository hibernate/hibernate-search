/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexSearchTargetBuilder;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.mapper.pojo.session.context.spi.PojoSessionContextImplementor;
import org.hibernate.search.mapper.pojo.mapping.context.spi.PojoMappingContextImplementor;
import org.hibernate.search.mapper.pojo.search.spi.PojoSearchTargetDelegate;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexSearchTarget;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.util.AssertionFailure;

class PojoSearchTargetDelegateImpl<E> implements PojoSearchTargetDelegate<E> {

	private final PojoIndexedTypeManagerContainer typeManagers;
	private final Set<PojoIndexedTypeManager<?, ? extends E, ?>> targetedTypeManagers;
	private final PojoSessionContextImplementor sessionContext;
	private MappedIndexSearchTarget indexSearchTarget;

	PojoSearchTargetDelegateImpl(PojoIndexedTypeManagerContainer typeManagers,
			Set<PojoIndexedTypeManager<?, ? extends E, ?>> targetedTypeManagers,
			PojoSessionContextImplementor sessionContext) {
		this.typeManagers = typeManagers;
		this.targetedTypeManagers = targetedTypeManagers;
		this.sessionContext = sessionContext;
	}

	@Override
	public Set<Class<? extends E>> getTargetedIndexedTypes() {
		return targetedTypeManagers.stream()
				.map( PojoIndexedTypeManager::getIndexedJavaClass )
				.collect( Collectors.toCollection( LinkedHashSet::new ) );
	}

	@Override
	public <O, Q> SearchQueryResultContext<Q> queryAsLoadedObjects(ObjectLoader<PojoReference, O> objectLoader,
			Function<SearchQuery<O>, Q> searchQueryWrapperFactory) {
		return getIndexSearchTarget().queryAsLoadedObjects(
				sessionContext, this::toPojoReference, objectLoader, searchQueryWrapperFactory
		);
	}

	@Override
	public <T, Q> SearchQueryResultContext<Q> queryAsReferences(Function<PojoReference, T> hitTransformer,
			Function<SearchQuery<T>, Q> searchQueryWrapperFactory) {
		return getIndexSearchTarget().queryAsReferences(
				sessionContext, this::toPojoReference, hitTransformer, searchQueryWrapperFactory
		);
	}

	@Override
	public <O, T, Q> SearchQueryResultContext<Q> queryAsProjections(ObjectLoader<PojoReference, O> objectLoader,
			Function<List<?>, T> hitTransformer, Function<SearchQuery<T>, Q> searchQueryWrapperFactory,
			SearchProjection<?>... projections) {
		return getIndexSearchTarget().queryAsProjections(
				sessionContext, this::toPojoReference, objectLoader, hitTransformer, searchQueryWrapperFactory,
				projections
		);
	}

	@Override
	public SearchPredicateFactoryContext predicate() {
		return getIndexSearchTarget().predicate();
	}

	@Override
	public SearchSortContainerContext sort() {
		return getIndexSearchTarget().sort();
	}

	@Override
	public SearchProjectionFactoryContext projection() {
		return getIndexSearchTarget().projection();
	}

	private MappedIndexSearchTarget getIndexSearchTarget() {
		PojoMappingContextImplementor mappingContext = sessionContext.getMappingContext();
		if ( indexSearchTarget == null ) {
			Iterator<PojoIndexedTypeManager<?, ? extends E, ?>> iterator = targetedTypeManagers.iterator();
			MappedIndexSearchTargetBuilder builder = iterator.next().createSearchTargetBuilder( mappingContext );
			while ( iterator.hasNext() ) {
				iterator.next().addToSearchTarget( builder );
			}
			indexSearchTarget = builder.build();
		}
		return indexSearchTarget;
	}

	private PojoReference toPojoReference(DocumentReference documentReference) {
		PojoIndexedTypeManager<?, ?, ?> typeManager = typeManagers.getByIndexName( documentReference.getIndexName() )
				.orElseThrow( () -> new AssertionFailure(
						"Document reference " + documentReference + " could not be converted to a PojoReference" ) );
		// TODO error handling if typeManager is null
		Object id = typeManager.getIdentifierMapping().fromDocumentIdentifier( documentReference.getId(), sessionContext );
		return new PojoReferenceImpl( typeManager.getIndexedJavaClass(), id );
	}
}
