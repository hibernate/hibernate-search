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
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneFieldPredicateBuilderFactory;
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
	public <T> T queryElement(SearchQueryElementTypeKey<T> key, LuceneSearchContext searchContext) {
		LuceneSearchFieldQueryElementFactory<T, F> factory = type().queryElementFactory( key );
		if ( factory == null ) {
			throw log.cannotUseQueryElementForField( absolutePath(), key.toString(), eventContext() );
		}
		return factory.create( searchContext, this );
	}

	@Override
	public DslConverter<?, F> dslConverter() {
		return getFromTypeIfCompatible( LuceneSearchFieldTypeContext::dslConverter, DslConverter::isCompatibleWith,
				"dslConverter" );
	}

	@Override
	public DslConverter<F, F> rawDslConverter() {
		return getFromTypeIfCompatible( LuceneSearchFieldTypeContext::rawDslConverter, DslConverter::isCompatibleWith,
				"rawDslConverter" );
	}

	@Override
	public ProjectionConverter<F, ?> projectionConverter() {
		return getFromTypeIfCompatible( LuceneSearchFieldTypeContext::projectionConverter,
				ProjectionConverter::isCompatibleWith, "projectionConverter" );
	}

	@Override
	public ProjectionConverter<F, F> rawProjectionConverter() {
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
	public <T> LuceneSearchFieldQueryElementFactory<T, F> queryElementFactory(SearchQueryElementTypeKey<T> key) {
		LuceneSearchFieldQueryElementFactory<T, F> factory = null;
		for ( LuceneSearchFieldContext<F> fieldContext : fieldForEachIndex ) {
			LuceneSearchFieldTypeContext<F> fieldType = fieldContext.type();
			LuceneSearchFieldQueryElementFactory<T, F> factoryForFieldContext =
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
	public LuceneFieldPredicateBuilderFactory<F> predicateBuilderFactory() {
		return getFromTypeIfCompatible( LuceneSearchFieldTypeContext::predicateBuilderFactory,
				LuceneFieldPredicateBuilderFactory::isCompatibleWith, "predicateFactory" );
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
