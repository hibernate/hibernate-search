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

import org.hibernate.search.engine.backend.index.spi.IndexSearchTargetBuilder;
import org.hibernate.search.mapper.pojo.session.context.spi.PojoSessionContextImplementor;
import org.hibernate.search.mapper.pojo.mapping.context.spi.PojoMappingContextImplementor;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSearchTargetDelegate;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.util.AssertionFailure;

public class PojoSearchTargetDelegateImpl<T> implements PojoSearchTargetDelegate<T> {

	private final PojoIndexedTypeManagerContainer typeManagers;
	private final Set<PojoIndexedTypeManager<?, ? extends T, ?>> targetedTypeManagers;
	private final PojoSessionContextImplementor sessionContext;
	private IndexSearchTarget indexSearchTarget;

	public PojoSearchTargetDelegateImpl(PojoIndexedTypeManagerContainer typeManagers,
			Set<PojoIndexedTypeManager<?, ? extends T, ?>> targetedTypeManagers,
			PojoSessionContextImplementor sessionContext) {
		this.typeManagers = typeManagers;
		this.targetedTypeManagers = targetedTypeManagers;
		this.sessionContext = sessionContext;
	}

	@Override
	public Set<Class<? extends T>> getTargetedIndexedTypes() {
		return targetedTypeManagers.stream()
				.map( PojoIndexedTypeManager::getIndexedJavaClass )
				.collect( Collectors.toCollection( LinkedHashSet::new ) );
	}

	@Override
	public SearchQueryResultDefinitionContext<PojoReference, PojoReference> query() {
		return query( ObjectLoader.identity() );
	}

	@Override
	public <O> SearchQueryResultDefinitionContext<PojoReference, O> query(ObjectLoader<PojoReference, O> objectLoader) {
		return getIndexSearchTarget().query( sessionContext, this::toPojoReference, objectLoader );
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

	private IndexSearchTarget getIndexSearchTarget() {
		PojoMappingContextImplementor mappingContext = sessionContext.getMappingContext();
		if ( indexSearchTarget == null ) {
			Iterator<PojoIndexedTypeManager<?, ? extends T, ?>> iterator = targetedTypeManagers.iterator();
			IndexSearchTargetBuilder builder = iterator.next().createSearchTargetBuilder( mappingContext );
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
