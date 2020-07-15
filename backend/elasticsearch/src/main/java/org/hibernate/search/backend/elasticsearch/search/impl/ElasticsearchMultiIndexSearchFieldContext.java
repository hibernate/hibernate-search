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
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.types.aggregation.impl.ElasticsearchFieldAggregationBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchFieldProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

public class ElasticsearchMultiIndexSearchFieldContext<F>
		implements ElasticsearchSearchFieldContext<F>, ElasticsearchSearchFieldTypeContext<F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Set<String> indexNames;
	private final String absolutePath;
	private final List<ElasticsearchSearchFieldContext<F>> fieldForEachIndex;

	public ElasticsearchMultiIndexSearchFieldContext(Set<String> indexNames, String absolutePath,
			List<ElasticsearchSearchFieldContext<F>> fieldForEachIndex) {
		this.indexNames = indexNames;
		this.absolutePath = absolutePath;
		this.fieldForEachIndex = fieldForEachIndex;
	}

	@Override
	public String absolutePath() {
		return absolutePath;
	}

	@Override
	public String[] absolutePathComponents() {
		// The path is the same for all fields, so we just pick the first one.
		return fieldForEachIndex.get( 0 ).absolutePathComponents();
	}

	@Override
	public List<String> nestedPathHierarchy() {
		return getFromFieldIfCompatible( ElasticsearchSearchFieldContext::nestedPathHierarchy, Object::equals,
				"nestedPathHierarchy" );
	}

	@Override
	public boolean multiValuedInRoot() {
		for ( ElasticsearchSearchFieldContext<F> field : fieldForEachIndex ) {
			if ( field.multiValuedInRoot() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public ElasticsearchSearchFieldTypeContext<F> type() {
		return this;
	}

	@Override
	public EventContext eventContext() {
		return indexesEventContext()
				.append( EventContexts.fromIndexFieldAbsolutePath( absolutePath ) );
	}

	private EventContext indexesEventContext() {
		return EventContexts.fromIndexNames( indexNames );
	}

	@Override
	public <T> T queryElement(SearchQueryElementTypeKey<T> key, ElasticsearchSearchContext searchContext) {
		ElasticsearchSearchFieldQueryElementFactory<T, F> factory = type().queryElementFactory( key );
		if ( factory == null ) {
			throw log.cannotUseQueryElementForField( absolutePath(), key.toString(), eventContext() );
		}
		return factory.create( searchContext, this );
	}

	@Override
	public DslConverter<?, F> dslConverter() {
		return getFromTypeIfCompatible( ElasticsearchSearchFieldTypeContext::dslConverter, DslConverter::isCompatibleWith,
				"dslConverter" );
	}

	@Override
	public DslConverter<F, F> rawDslConverter() {
		return getFromTypeIfCompatible( ElasticsearchSearchFieldTypeContext::rawDslConverter, DslConverter::isCompatibleWith,
				"rawDslConverter" );
	}

	@Override
	public ProjectionConverter<F, ?> projectionConverter() {
		return getFromTypeIfCompatible( ElasticsearchSearchFieldTypeContext::projectionConverter,
				ProjectionConverter::isCompatibleWith, "projectionConverter" );
	}

	@Override
	public ProjectionConverter<F, F> rawProjectionConverter() {
		return getFromTypeIfCompatible( ElasticsearchSearchFieldTypeContext::rawProjectionConverter,
				ProjectionConverter::isCompatibleWith, "rawProjectionConverter" );
	}

	@Override
	public Optional<String> searchAnalyzerName() {
		return getFromTypeIfCompatible( ElasticsearchSearchFieldTypeContext::searchAnalyzerName, Object::equals,
				"searchAnalyzer" );
	}

	@Override
	public Optional<String> normalizerName() {
		return getFromTypeIfCompatible( ElasticsearchSearchFieldTypeContext::normalizerName, Object::equals,
				"normalizer" );
	}

	@Override
	public <T> ElasticsearchSearchFieldQueryElementFactory<T, F> queryElementFactory(SearchQueryElementTypeKey<T> key) {
		ElasticsearchSearchFieldQueryElementFactory<T, F> factory = null;
		for ( ElasticsearchSearchFieldContext<F> fieldContext : fieldForEachIndex ) {
			ElasticsearchSearchFieldTypeContext<F> fieldType = fieldContext.type();
			ElasticsearchSearchFieldQueryElementFactory<T, F> factoryForFieldContext =
					fieldType.queryElementFactory( key );
			if ( factoryForFieldContext == null ) {
				// The query element can't be created for at least one of the indexes.
				return null;
			}
			if ( factory == null ) {
				factory = factoryForFieldContext;
			}
			else if ( !factory.isCompatibleWith( factoryForFieldContext ) ) {
				throw log.conflictingQueryElementForFieldOnMultipleIndexes( absolutePath, key.toString(),
						factory, factoryForFieldContext, indexesEventContext() );
			}
		}
		return factory;
	}

	@Override
	public ElasticsearchFieldPredicateBuilderFactory<F> predicateBuilderFactory() {
		return getFromTypeIfCompatible( ElasticsearchSearchFieldTypeContext::predicateBuilderFactory,
				ElasticsearchFieldPredicateBuilderFactory::isCompatibleWith, "predicateFactory" );
	}

	@Override
	public ElasticsearchFieldSortBuilderFactory<F> sortBuilderFactory() {
		return getFromTypeIfCompatible( ElasticsearchSearchFieldTypeContext::sortBuilderFactory,
				ElasticsearchFieldSortBuilderFactory::isCompatibleWith, "sortFactory" );
	}

	@Override
	public ElasticsearchFieldProjectionBuilderFactory<F> projectionBuilderFactory() {
		return getFromTypeIfCompatible( ElasticsearchSearchFieldTypeContext::projectionBuilderFactory,
				ElasticsearchFieldProjectionBuilderFactory::isCompatibleWith, "projectionFactory" );
	}

	@Override
	public ElasticsearchFieldAggregationBuilderFactory<F> aggregationBuilderFactory() {
		return getFromTypeIfCompatible( ElasticsearchSearchFieldTypeContext::aggregationBuilderFactory,
				ElasticsearchFieldAggregationBuilderFactory::isCompatibleWith, "aggregationFactory" );
	}

	private <T> T getFromFieldIfCompatible(Function<ElasticsearchSearchFieldContext<F>, T> getter,
			BiPredicate<T, T> compatiblityChecker, String attributeName) {
		T attribute = null;
		for ( ElasticsearchSearchFieldContext<F> fieldContext : fieldForEachIndex ) {
			T attributeForFieldContext = getter.apply( fieldContext );
			if ( attribute == null ) {
				attribute = attributeForFieldContext;
			}
			else if ( !compatiblityChecker.test( attribute, attributeForFieldContext ) ) {
				throw log.conflictingFieldTypesForSearch( absolutePath, attributeName,
						attribute, attributeForFieldContext, indexesEventContext() );
			}
		}
		return attribute;
	}

	private <T> T getFromTypeIfCompatible(Function<ElasticsearchSearchFieldTypeContext<F>, T> getter,
			BiPredicate<T, T> compatiblityChecker, String attributeName) {
		T attribute = null;
		for ( ElasticsearchSearchFieldContext<F> fieldContext : fieldForEachIndex ) {
			ElasticsearchSearchFieldTypeContext<F> fieldType = fieldContext.type();
			T attributeForFieldContext = getter.apply( fieldType );
			if ( attribute == null ) {
				attribute = attributeForFieldContext;
			}
			else if ( !compatiblityChecker.test( attribute, attributeForFieldContext ) ) {
				throw log.conflictingFieldTypesForSearch( absolutePath, attributeName,
						attribute, attributeForFieldContext, indexesEventContext() );
			}
		}
		return attribute;
	}
}
