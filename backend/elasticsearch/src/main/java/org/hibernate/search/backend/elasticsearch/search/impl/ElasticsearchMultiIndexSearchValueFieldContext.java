/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonPrimitive;

public class ElasticsearchMultiIndexSearchValueFieldContext<F>
		extends AbstractElasticsearchMultiIndexSearchIndexSchemaElementContext<ElasticsearchSearchValueFieldContext<F>>
		implements ElasticsearchSearchValueFieldContext<F>, ElasticsearchSearchValueFieldTypeContext<F> {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public ElasticsearchMultiIndexSearchValueFieldContext(ElasticsearchSearchIndexScope scope, String absolutePath,
			List<ElasticsearchSearchValueFieldContext<F>> elementForEachIndex) {
		super( scope, absolutePath, elementForEachIndex );
	}

	@Override
	protected ElasticsearchSearchValueFieldContext<F> self() {
		return this;
	}

	@Override
	public boolean isComposite() {
		return false;
	}

	@Override
	public ElasticsearchSearchCompositeIndexSchemaElementContext toComposite() {
		throw log.invalidIndexElementTypeValueFieldIsNotObjectField( absolutePath );
	}

	@Override
	public String[] absolutePathComponents() {
		// The path is the same for all fields, so we just pick the first one.
		return elementForEachIndex.get( 0 ).absolutePathComponents();
	}

	@Override
	public boolean multiValuedInRoot() {
		for ( ElasticsearchSearchValueFieldContext<F> field : elementForEachIndex ) {
			if ( field.multiValuedInRoot() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public ElasticsearchSearchValueFieldTypeContext<F> type() {
		return this;
	}

	@Override
	protected String missingSupportHint(String queryElementName) {
		return log.missingSupportHintForValueField( queryElementName );
	}

	@Override
	protected String partialSupportHint() {
		return log.partialSupportHintForValueField();
	}

	@Override
	protected <T> ElasticsearchSearchQueryElementFactory<T, ElasticsearchSearchValueFieldContext<F>> queryElementFactory(
			ElasticsearchSearchValueFieldContext<F> indexElement, SearchQueryElementTypeKey<T> key) {
		return indexElement.type().queryElementFactory( key );
	}

	@Override
	public JsonPrimitive elasticsearchTypeAsJson() {
		return getFromTypeIfCompatible( ElasticsearchSearchValueFieldTypeContext::elasticsearchTypeAsJson, Object::equals,
				"elasticsearchType" );
	}

	@Override
	public DslConverter<?, F> dslConverter() {
		return getFromTypeIfCompatible( ElasticsearchSearchValueFieldTypeContext::dslConverter, DslConverter::isCompatibleWith,
				"dslConverter" );
	}

	@Override
	public DslConverter<F, F> rawDslConverter() {
		return getFromTypeIfCompatible( ElasticsearchSearchValueFieldTypeContext::rawDslConverter, DslConverter::isCompatibleWith,
				"rawDslConverter" );
	}

	@Override
	public ProjectionConverter<F, ?> projectionConverter() {
		return getFromTypeIfCompatible( ElasticsearchSearchValueFieldTypeContext::projectionConverter,
				ProjectionConverter::isCompatibleWith, "projectionConverter" );
	}

	@Override
	public ProjectionConverter<F, F> rawProjectionConverter() {
		return getFromTypeIfCompatible( ElasticsearchSearchValueFieldTypeContext::rawProjectionConverter,
				ProjectionConverter::isCompatibleWith, "rawProjectionConverter" );
	}

	@Override
	public Optional<String> searchAnalyzerName() {
		return getFromTypeIfCompatible( ElasticsearchSearchValueFieldTypeContext::searchAnalyzerName, Object::equals,
				"searchAnalyzer" );
	}

	@Override
	public Optional<String> normalizerName() {
		return getFromTypeIfCompatible( ElasticsearchSearchValueFieldTypeContext::normalizerName, Object::equals,
				"normalizer" );
	}

	@Override
	public boolean hasNormalizerOnAtLeastOneIndex() {
		for ( ElasticsearchSearchValueFieldContext<F> indexElement : elementForEachIndex ) {
			if ( indexElement.type().hasNormalizerOnAtLeastOneIndex() ) {
				return true;
			}
		}
		return false;
	}

	private <T> T getFromTypeIfCompatible(Function<ElasticsearchSearchValueFieldTypeContext<F>, T> getter,
			BiPredicate<T, T> compatiblityChecker, String attributeName) {
		T attribute = null;
		for ( ElasticsearchSearchValueFieldContext<F> indexElement : elementForEachIndex ) {
			ElasticsearchSearchValueFieldTypeContext<F> fieldType = indexElement.type();
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
