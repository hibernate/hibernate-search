/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl;

import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;

public class StubMultiIndexSearchIndexValueFieldContext<F>
		extends AbstractStubMultiIndexSearchIndexNodeContext<StubSearchIndexValueFieldContext<F>>
		implements StubSearchIndexValueFieldContext<F>, StubSearchValueFieldTypeContext<F> {

	public StubMultiIndexSearchIndexValueFieldContext(StubSearchIndexScope scope, String absolutePath,
			List<StubSearchIndexValueFieldContext<F>> elementForEachIndex) {
		super( scope, absolutePath, elementForEachIndex );
	}

	@Override
	public boolean isValueField() {
		return true;
	}

	@Override
	public StubSearchIndexValueFieldContext<?> toValueField() {
		return this;
	}

	@Override
	public StubSearchValueFieldTypeContext<F> type() {
		return this;
	}

	@Override
	public <T> T queryElement(SearchQueryElementTypeKey<T> key, StubSearchIndexScope scope) {
		return queryElementFactory( key ).create( scope, this );
	}

	@Override
	protected <T> AbstractStubSearchQueryElementFactory<T> queryElementFactory(
			StubSearchIndexValueFieldContext<F> indexElement, SearchQueryElementTypeKey<T> key) {
		return indexElement.type().queryElementFactory( key );
	}

	@Override
	public Class<F> valueClass() {
		return getFromTypeIfCompatible( StubSearchValueFieldTypeContext::valueClass, Objects::equals,
				"valueClass" );
	}

	@Override
	public DslConverter<?, F> dslConverter() {
		return getFromTypeIfCompatible( StubSearchValueFieldTypeContext::dslConverter, DslConverter::isCompatibleWith,
				"dslConverter" );
	}

	@Override
	public DslConverter<F, F> rawDslConverter() {
		return getFromTypeIfCompatible( StubSearchValueFieldTypeContext::rawDslConverter, DslConverter::isCompatibleWith,
				"rawDslConverter" );
	}

	@Override
	public ProjectionConverter<F, ?> projectionConverter() {
		return getFromTypeIfCompatible( StubSearchValueFieldTypeContext::projectionConverter,
				ProjectionConverter::isCompatibleWith, "projectionConverter" );
	}

	@Override
	public ProjectionConverter<F, F> rawProjectionConverter() {
		return getFromTypeIfCompatible( StubSearchValueFieldTypeContext::rawProjectionConverter,
				ProjectionConverter::isCompatibleWith, "rawProjectionConverter" );
	}

	private <T> T getFromTypeIfCompatible(Function<StubSearchValueFieldTypeContext<F>, T> getter,
			BiPredicate<T, T> compatiblityChecker, String attributeName) {
		T attribute = null;
		for ( StubSearchIndexValueFieldContext<F> indexElement : elementForEachIndex ) {
			StubSearchValueFieldTypeContext<F> fieldType = indexElement.type();
			T attributeForIndexElement = getter.apply( fieldType );
			if ( attribute == null ) {
				attribute = attributeForIndexElement;
			}
			else {
				checkAttributeCompatibility( compatiblityChecker, attributeName, attribute, attributeForIndexElement );
			}
		}
		return attribute;
	}
}
