/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.common.spi.SearchIndexIdentifierContext;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;

public class StubSearchProjectionBuilderFactory implements SearchProjectionBuilderFactory {

	private final StubSearchIndexScope scope;

	public StubSearchProjectionBuilderFactory(StubSearchIndexScope scope) {
		this.scope = scope;
	}

	@Override
	public SearchProjection<DocumentReference> documentReference() {
		return StubDefaultProjection.get();
	}

	@Override
	public <E> SearchProjection<E> entityLoading() {
		return StubEntityLoadingProjection.get();
	}

	@Override
	public <R> SearchProjection<R> entityReference() {
		return StubReferenceProjection.get();
	}

	@Override
	public <I> SearchProjection<I> id(Class<I> identifierType) {
		SearchIndexIdentifierContext identifier = scope.identifier();
		return new StubIdProjection<>(
				identifier.projectionConverter().withConvertedType( identifierType, identifier ) );
	}

	@Override
	public SearchProjection<Float> score() {
		return StubDefaultProjection.get();
	}

	@Override
	public CompositeProjectionBuilder composite() {
		return new StubCompositeProjection.Builder();
	}

	@Override
	public <T> SearchProjection<T> constant(T value) {
		return new StubConstantProjection<>( value );
	}

	@Override
	public <T> SearchProjection<T> entityComposite(SearchProjection<T> delegate) {
		return new StubEntityCompositeProjection<>( StubSearchProjection.from( delegate ) );
	}

	@Override
	public <T> SearchProjection<T> throwing(Supplier<SearchException> exceptionSupplier) {
		return new StubThrowingProjection<>( exceptionSupplier );
	}

	@Override
	public <T> SearchProjection<T> byTypeName(Map<String, ? extends SearchProjection<? extends T>> inners) {
		Map<String, StubSearchProjection<? extends T>> stubInners = new HashMap<>();
		for ( Map.Entry<String, ? extends SearchProjection<? extends T>> entry : inners.entrySet() ) {
			stubInners.put( entry.getKey(), StubSearchProjection.from( entry.getValue() ) );
		}
		return new StubByMappedTypeProjection<>( stubInners );
	}

}
