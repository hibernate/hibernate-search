/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScope;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScopeBuilder;
import org.hibernate.search.engine.mapper.session.context.spi.DetachedSessionContextImplementor;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingTypeMetadata;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;
import org.hibernate.search.mapper.pojo.mapping.context.spi.AbstractPojoMappingContextImplementor;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactoryContext;
import org.hibernate.search.mapper.pojo.work.impl.PojoScopeWorkExecutorImpl;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkExecutor;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.StreamHelper;

class PojoScopeDelegateImpl<E, E2> implements PojoScopeDelegate<E, E2> {

	private final PojoIndexedTypeManagerContainer typeManagers;
	private final Set<PojoIndexedTypeManager<?, ? extends E, ?>> targetedTypeManagers;
	private final AbstractPojoSessionContextImplementor sessionContext;
	private MappedIndexScope<PojoReference, E2> delegate;
	private PojoScopeWorkExecutor executor;

	PojoScopeDelegateImpl(PojoIndexedTypeManagerContainer typeManagers,
			Set<PojoIndexedTypeManager<?, ? extends E, ?>> targetedTypeManagers,
			AbstractPojoSessionContextImplementor sessionContext) {
		this.typeManagers = typeManagers;
		this.targetedTypeManagers = targetedTypeManagers;
		this.sessionContext = sessionContext;
	}

	@Override
	public Map<Class<? extends E>, PojoMappingTypeMetadata> getIncludedIndexedTypes() {
		return targetedTypeManagers.stream()
				.collect( StreamHelper.toMap(
						PojoIndexedTypeManager::getJavaClass,
						PojoIndexedTypeManager::getMappingMetadata,
						LinkedHashMap::new
				) );
	}

	@Override
	public PojoReference toPojoReference(DocumentReference documentReference) {
		PojoIndexedTypeManager<?, ?, ?> typeManager = typeManagers.getByIndexName( documentReference.getIndexName() )
				.orElseThrow( () -> new AssertionFailure(
						"Document reference " + documentReference + " could not be converted to a PojoReference"
				) );
		Object id = typeManager.getIdentifierMapping().fromDocumentIdentifier( documentReference.getId(), sessionContext );
		return new PojoReferenceImpl( typeManager.getJavaClass(), id );
	}

	@Override
	public SearchQueryResultDefinitionContext<?, PojoReference, E2, SearchProjectionFactoryContext<PojoReference, E2>, ?> search(
			LoadingContextBuilder<PojoReference, E2> loadingContextBuilder) {
		return getIndexScope().search( sessionContext, loadingContextBuilder );
	}

	@Override
	public SearchPredicateFactoryContext predicate() {
		return getIndexScope().predicate();
	}

	@Override
	public SearchSortFactoryContext sort() {
		return getIndexScope().sort();
	}

	@Override
	public SearchProjectionFactoryContext<PojoReference, E2> projection() {
		return getIndexScope().projection();
	}

	@Override
	public PojoScopeWorkExecutor executor() {
		if ( executor == null ) {
			executor = new PojoScopeWorkExecutorImpl(
					targetedTypeManagers, DetachedSessionContextImplementor.of( sessionContext )
			);
		}
		return executor;
	}

	private MappedIndexScope<PojoReference, E2> getIndexScope() {
		AbstractPojoMappingContextImplementor mappingContext = sessionContext.getMappingContext();
		if ( delegate == null ) {
			Iterator<PojoIndexedTypeManager<?, ? extends E, ?>> iterator = targetedTypeManagers.iterator();
			MappedIndexScopeBuilder<PojoReference, E2> builder = iterator.next().createScopeBuilder(
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
