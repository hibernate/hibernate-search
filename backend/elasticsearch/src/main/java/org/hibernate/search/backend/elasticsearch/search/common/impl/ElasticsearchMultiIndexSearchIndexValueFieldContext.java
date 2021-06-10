/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.common.impl;

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

public class ElasticsearchMultiIndexSearchIndexValueFieldContext<F>
		extends AbstractElasticsearchMultiIndexSearchIndexNodeContext<ElasticsearchSearchIndexValueFieldContext<F>>
		implements ElasticsearchSearchIndexValueFieldContext<F>, ElasticsearchSearchIndexValueFieldTypeContext<F> {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public ElasticsearchMultiIndexSearchIndexValueFieldContext(ElasticsearchSearchIndexScope scope, String absolutePath,
			List<ElasticsearchSearchIndexValueFieldContext<F>> elementForEachIndex) {
		super( scope, absolutePath, elementForEachIndex );
	}

	@Override
	protected ElasticsearchSearchIndexValueFieldContext<F> self() {
		return this;
	}

	@Override
	public boolean isComposite() {
		return false;
	}

	@Override
	public ElasticsearchSearchIndexCompositeNodeContext toComposite() {
		throw log.invalidIndexElementTypeValueFieldIsNotObjectField( absolutePath );
	}

	@Override
	public String[] absolutePathComponents() {
		// The path is the same for all fields, so we just pick the first one.
		return elementForEachIndex.get( 0 ).absolutePathComponents();
	}

	@Override
	public boolean multiValuedInRoot() {
		for ( ElasticsearchSearchIndexValueFieldContext<F> field : elementForEachIndex ) {
			if ( field.multiValuedInRoot() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public ElasticsearchSearchIndexValueFieldTypeContext<F> type() {
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
	protected <T> ElasticsearchSearchQueryElementFactory<T, ElasticsearchSearchIndexValueFieldContext<F>> queryElementFactory(
			ElasticsearchSearchIndexValueFieldContext<F> indexElement, SearchQueryElementTypeKey<T> key) {
		return indexElement.type().queryElementFactory( key );
	}

	@Override
	public JsonPrimitive elasticsearchTypeAsJson() {
		return getFromTypeIfCompatible( ElasticsearchSearchIndexValueFieldTypeContext::elasticsearchTypeAsJson, Object::equals,
				"elasticsearchType" );
	}

	@Override
	public DslConverter<?, F> dslConverter() {
		return getFromTypeIfCompatible( ElasticsearchSearchIndexValueFieldTypeContext::dslConverter, DslConverter::isCompatibleWith,
				"dslConverter" );
	}

	@Override
	public DslConverter<F, F> rawDslConverter() {
		return getFromTypeIfCompatible( ElasticsearchSearchIndexValueFieldTypeContext::rawDslConverter, DslConverter::isCompatibleWith,
				"rawDslConverter" );
	}

	@Override
	public ProjectionConverter<F, ?> projectionConverter() {
		return getFromTypeIfCompatible( ElasticsearchSearchIndexValueFieldTypeContext::projectionConverter,
				ProjectionConverter::isCompatibleWith, "projectionConverter" );
	}

	@Override
	public ProjectionConverter<F, F> rawProjectionConverter() {
		return getFromTypeIfCompatible( ElasticsearchSearchIndexValueFieldTypeContext::rawProjectionConverter,
				ProjectionConverter::isCompatibleWith, "rawProjectionConverter" );
	}

	@Override
	public Optional<String> searchAnalyzerName() {
		return getFromTypeIfCompatible( ElasticsearchSearchIndexValueFieldTypeContext::searchAnalyzerName, Object::equals,
				"searchAnalyzer" );
	}

	@Override
	public Optional<String> normalizerName() {
		return getFromTypeIfCompatible( ElasticsearchSearchIndexValueFieldTypeContext::normalizerName, Object::equals,
				"normalizer" );
	}

	@Override
	public boolean hasNormalizerOnAtLeastOneIndex() {
		for ( ElasticsearchSearchIndexValueFieldContext<F> indexElement : elementForEachIndex ) {
			if ( indexElement.type().hasNormalizerOnAtLeastOneIndex() ) {
				return true;
			}
		}
		return false;
	}

	private <T> T getFromTypeIfCompatible(Function<ElasticsearchSearchIndexValueFieldTypeContext<F>, T> getter,
			BiPredicate<T, T> compatiblityChecker, String attributeName) {
		T attribute = null;
		for ( ElasticsearchSearchIndexValueFieldContext<F> indexElement : elementForEachIndex ) {
			ElasticsearchSearchIndexValueFieldTypeContext<F> fieldType = indexElement.type();
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
