/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneFieldAggregationBuilderFactory;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.analysis.Analyzer;

public class LuceneMultiIndexSearchFieldContext<F>
		implements LuceneSearchFieldContext<F>, LuceneSearchFieldTypeContext<F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Set<String> indexNames;
	private final String absolutePath;
	private final List<LuceneSearchFieldContext<F>> fieldForEachIndex;

	public LuceneMultiIndexSearchFieldContext(Set<String> indexNames, String absolutePath,
			List<LuceneSearchFieldContext<F>> fieldForEachIndex) {
		this.indexNames = indexNames;
		this.absolutePath = absolutePath;
		this.fieldForEachIndex = fieldForEachIndex;
	}

	@Override
	public String absolutePath() {
		return absolutePath;
	}

	@Override
	public String nestedDocumentPath() {
		return getFromFieldIfCompatible( LuceneSearchFieldContext::nestedDocumentPath, Object::equals,
				"nestedDocumentPath" );
	}

	@Override
	public List<String> nestedPathHierarchy() {
		return getFromFieldIfCompatible( LuceneSearchFieldContext::nestedPathHierarchy, Object::equals,
				"nestedPathHierarchy" );
	}

	@Override
	public boolean multiValuedInRoot() {
		for ( LuceneSearchFieldContext<F> field : fieldForEachIndex ) {
			if ( field.multiValuedInRoot() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public LuceneSearchFieldTypeContext<F> type() {
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
	public DslConverter<?, ? extends F> dslConverter() {
		return getFromTypeIfCompatible( LuceneSearchFieldTypeContext::dslConverter, DslConverter::isCompatibleWith,
				"dslConverter" );
	}

	@Override
	public DslConverter<F, ? extends F> rawDslConverter() {
		return getFromTypeIfCompatible( LuceneSearchFieldTypeContext::rawDslConverter, DslConverter::isCompatibleWith,
				"rawDslConverter" );
	}

	@Override
	public ProjectionConverter<? super F, ?> projectionConverter() {
		return getFromTypeIfCompatible( LuceneSearchFieldTypeContext::projectionConverter,
				ProjectionConverter::isCompatibleWith, "projectionConverter" );
	}

	@Override
	public ProjectionConverter<? super F, F> rawProjectionConverter() {
		return getFromTypeIfCompatible( LuceneSearchFieldTypeContext::rawProjectionConverter,
				ProjectionConverter::isCompatibleWith, "rawProjectionConverter" );
	}

	@Override
	public Optional<String> searchAnalyzerName() {
		return getFromTypeIfCompatible( LuceneSearchFieldTypeContext::searchAnalyzerName, Object::equals,
				"searchAnalyzer" );
	}

	@Override
	public Analyzer searchAnalyzerOrNormalizer() {
		return getFromTypeIfCompatible( LuceneSearchFieldTypeContext::searchAnalyzerOrNormalizer, Object::equals,
				"searchAnalyzerOrNormalizer" );
	}

	@Override
	public LuceneFieldPredicateBuilderFactory<F> predicateBuilderFactory() {
		return getFromTypeIfCompatible( LuceneSearchFieldTypeContext::predicateBuilderFactory,
				LuceneFieldPredicateBuilderFactory::isCompatibleWith, "predicateFactory" );
	}

	@Override
	public LuceneFieldSortBuilderFactory<F> sortBuilderFactory() {
		return getFromTypeIfCompatible( LuceneSearchFieldTypeContext::sortBuilderFactory,
				LuceneFieldSortBuilderFactory::isCompatibleWith, "sortFactory" );
	}

	@Override
	public LuceneFieldProjectionBuilderFactory<F> projectionBuilderFactory() {
		return getFromTypeIfCompatible( LuceneSearchFieldTypeContext::projectionBuilderFactory,
				LuceneFieldProjectionBuilderFactory::isCompatibleWith, "projectionFactory" );
	}

	@Override
	public LuceneFieldAggregationBuilderFactory<F> aggregationBuilderFactory() {
		return getFromTypeIfCompatible( LuceneSearchFieldTypeContext::aggregationBuilderFactory,
				LuceneFieldAggregationBuilderFactory::isCompatibleWith, "aggregationFactory" );
	}

	private <T> T getFromFieldIfCompatible(Function<LuceneSearchFieldContext<F>, T> getter,
			BiPredicate<T, T> compatiblityChecker, String attributeName) {
		T attribute = null;
		for ( LuceneSearchFieldContext<F> fieldContext : fieldForEachIndex ) {
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

	private <T> T getFromTypeIfCompatible(Function<LuceneSearchFieldTypeContext<F>, T> getter,
			BiPredicate<T, T> compatiblityChecker, String attributeName) {
		T attribute = null;
		for ( LuceneSearchFieldContext<F> fieldContext : fieldForEachIndex ) {
			LuceneSearchFieldTypeContext<F> fieldType = fieldContext.type();
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
