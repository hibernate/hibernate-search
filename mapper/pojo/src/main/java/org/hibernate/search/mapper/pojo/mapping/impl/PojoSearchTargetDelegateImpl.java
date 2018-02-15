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
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSearchTargetDelegate;
import org.hibernate.search.mapper.pojo.model.spi.PojoIndexableTypeModel;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.search.ObjectLoader;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.util.AssertionFailure;

public class PojoSearchTargetDelegateImpl<T> implements PojoSearchTargetDelegate<T> {

	private final PojoTypeManagerContainer typeManagers;
	private final Set<PojoTypeManager<?, ? extends T, ?>> targetedTypeManagers;
	private final SessionContext sessionContext;
	private IndexSearchTarget indexSearchTarget;

	public PojoSearchTargetDelegateImpl(PojoTypeManagerContainer typeManagers,
			Set<PojoTypeManager<?, ? extends T, ?>> targetedTypeManagers,
			SessionContext sessionContext) {
		this.typeManagers = typeManagers;
		this.targetedTypeManagers = targetedTypeManagers;
		this.sessionContext = sessionContext;
	}

	@Override
	public Set<PojoIndexableTypeModel<? extends T>> getTargetedIndexedTypes() {
		return targetedTypeManagers.stream()
				.map( PojoTypeManager::getTypeModel )
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
	public SearchPredicateContainerContext<SearchPredicate> predicate() {
		return getIndexSearchTarget().predicate();
	}

	@Override
	public SearchSortContainerContext<SearchSort> sort() {
		return getIndexSearchTarget().sort();
	}

	private IndexSearchTarget getIndexSearchTarget() {
		if ( indexSearchTarget == null ) {
			Iterator<PojoTypeManager<?, ? extends T, ?>> iterator = targetedTypeManagers.iterator();
			IndexSearchTargetBuilder builder = iterator.next().createSearchTarget();
			while ( iterator.hasNext() ) {
				iterator.next().addToSearchTarget( builder );
			}
			indexSearchTarget = builder.build();
		}
		return indexSearchTarget;
	}

	private PojoReference toPojoReference(DocumentReference documentReference) {
		PojoTypeManager<?, ?, ?> typeManager = typeManagers.getByIndexName( documentReference.getIndexName() )
				.orElseThrow( () -> new AssertionFailure(
						"Document reference " + documentReference + " could not be converted to a PojoReference" ) );
		// TODO error handling if typeManager is null
		Object id = typeManager.getIdentifierMapping().fromDocumentIdentifier( documentReference.getId() );
		return new PojoReferenceImpl( typeManager.getTypeModel().getJavaClass(), id );
	}
}
