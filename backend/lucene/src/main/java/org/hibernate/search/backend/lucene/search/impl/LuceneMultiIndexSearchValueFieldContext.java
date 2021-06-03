/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.analysis.Analyzer;

public class LuceneMultiIndexSearchValueFieldContext<F>
		implements LuceneSearchValueFieldContext<F>, LuceneSearchValueFieldTypeContext<F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Set<String> indexNames;
	private final String absolutePath;
	private final List<LuceneSearchValueFieldContext<F>> fieldForEachIndex;

	public LuceneMultiIndexSearchValueFieldContext(Set<String> indexNames, String absolutePath,
			List<LuceneSearchValueFieldContext<F>> fieldForEachIndex) {
		this.indexNames = indexNames;
		this.absolutePath = absolutePath;
		this.fieldForEachIndex = fieldForEachIndex;
	}

	@Override
	public boolean isComposite() {
		return false;
	}

	@Override
	public LuceneSearchCompositeIndexSchemaElementContext toComposite() {
		throw log.invalidIndexElementTypeValueFieldIsNotObjectField( absolutePath );
	}

	@Override
	public String absolutePath() {
		return absolutePath;
	}

	@Override
	public String nestedDocumentPath() {
		return getFromFieldIfCompatible( LuceneSearchValueFieldContext::nestedDocumentPath, Object::equals,
				"nestedDocumentPath" );
	}

	@Override
	public List<String> nestedPathHierarchy() {
		return getFromFieldIfCompatible( LuceneSearchValueFieldContext::nestedPathHierarchy, Object::equals,
				"nestedPathHierarchy" );
	}

	@Override
	public boolean multiValuedInRoot() {
		for ( LuceneSearchValueFieldContext<F> field : fieldForEachIndex ) {
			if ( field.multiValuedInRoot() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public LuceneSearchValueFieldTypeContext<F> type() {
		return this;
	}

	@Override
	public EventContext eventContext() {
		return indexesEventContext().append( relativeEventContext() );
	}

	private EventContext indexesEventContext() {
		return EventContexts.fromIndexNames( indexNames );
	}

	private EventContext relativeEventContext() {
		return EventContexts.fromIndexFieldAbsolutePath( absolutePath );
	}

	@Override
	public <T> T queryElement(SearchQueryElementTypeKey<T> key, LuceneSearchIndexScope scope) {
		AbstractLuceneSearchValueFieldQueryElementFactory<T, F> factory = type().queryElementFactory( key );
		if ( factory == null ) {
			throw log.cannotUseQueryElementForField( absolutePath(), key.toString(), eventContext() );
		}
		return factory.create( scope, this );
	}

	@Override
	public DslConverter<?, F> dslConverter() {
		return getFromTypeIfCompatible( LuceneSearchValueFieldTypeContext::dslConverter, DslConverter::isCompatibleWith,
				"dslConverter" );
	}

	@Override
	public DslConverter<F, F> rawDslConverter() {
		return getFromTypeIfCompatible( LuceneSearchValueFieldTypeContext::rawDslConverter, DslConverter::isCompatibleWith,
				"rawDslConverter" );
	}

	@Override
	public ProjectionConverter<F, ?> projectionConverter() {
		return getFromTypeIfCompatible( LuceneSearchValueFieldTypeContext::projectionConverter,
				ProjectionConverter::isCompatibleWith, "projectionConverter" );
	}

	@Override
	public ProjectionConverter<F, F> rawProjectionConverter() {
		return getFromTypeIfCompatible( LuceneSearchValueFieldTypeContext::rawProjectionConverter,
				ProjectionConverter::isCompatibleWith, "rawProjectionConverter" );
	}

	@Override
	public Analyzer searchAnalyzerOrNormalizer() {
		return getFromTypeIfCompatible( LuceneSearchValueFieldTypeContext::searchAnalyzerOrNormalizer, Object::equals,
				"searchAnalyzerOrNormalizer" );
	}

	@Override
	public <T> AbstractLuceneSearchValueFieldQueryElementFactory<T, F> queryElementFactory(SearchQueryElementTypeKey<T> key) {
		AbstractLuceneSearchValueFieldQueryElementFactory<T, F> factory = null;
		for ( LuceneSearchValueFieldContext<F> fieldContext : fieldForEachIndex ) {
			LuceneSearchValueFieldTypeContext<F> fieldType = fieldContext.type();
			AbstractLuceneSearchValueFieldQueryElementFactory<T, F> factoryForFieldContext =
					fieldType.queryElementFactory( key );
			if ( factory == null ) {
				factory = factoryForFieldContext;
			}
			else {
				checkFactoryCompatibility( key, factory, factoryForFieldContext );
			}
		}
		return factory;
	}

	private <T> T getFromFieldIfCompatible(Function<LuceneSearchValueFieldContext<F>, T> getter,
			BiPredicate<T, T> compatibilityChecker, String attributeName) {
		T attribute = null;
		for ( LuceneSearchValueFieldContext<F> fieldContext : fieldForEachIndex ) {
			T attributeForFieldContext = getter.apply( fieldContext );
			if ( attribute == null ) {
				attribute = attributeForFieldContext;
			}
			else {
				checkAttributeCompatibility( compatibilityChecker, attributeName, attribute, attributeForFieldContext );
			}
		}
		return attribute;
	}

	private <T> T getFromTypeIfCompatible(Function<LuceneSearchValueFieldTypeContext<F>, T> getter,
			BiPredicate<T, T> compatibilityChecker, String attributeName) {
		T attribute = null;
		for ( LuceneSearchValueFieldContext<F> fieldContext : fieldForEachIndex ) {
			LuceneSearchValueFieldTypeContext<F> fieldType = fieldContext.type();
			T attributeForFieldContext = getter.apply( fieldType );
			if ( attribute == null ) {
				attribute = attributeForFieldContext;
			}
			else {
				checkAttributeCompatibility( compatibilityChecker, attributeName, attribute, attributeForFieldContext );
			}
		}
		return attribute;
	}

	private <T> void checkFactoryCompatibility(SearchQueryElementTypeKey<T> key,
			AbstractLuceneSearchValueFieldQueryElementFactory<T, F> factory1, AbstractLuceneSearchValueFieldQueryElementFactory<T, F> factory2) {
		if ( factory1 == null && factory2 == null ) {
			return;
		}
		try {
			try {
				if ( factory1 == null || factory2 == null ) {
					throw log.partialSupportForQueryElement( key.toString() );
				}

				factory1.checkCompatibleWith( factory2 );
			}
			catch (SearchException e) {
				throw log.inconsistentSupportForQueryElement( key.toString(), e.getMessage(), e );
			}
		}
		catch (SearchException e) {
			throw log.inconsistentConfigurationForIndexElementForSearch( relativeEventContext(), e.getMessage(),
					indexesEventContext(), e );
		}
	}

	private <T> void checkAttributeCompatibility(BiPredicate<T, T> compatibilityChecker, String attributeName,
			T attribute1, T attribute2) {
		try {
			if ( !compatibilityChecker.test( attribute1, attribute2 ) ) {
				throw log.differentIndexElementAttribute( attributeName, attribute1, attribute2 );
			}
		}
		catch (SearchException e) {
			throw log.inconsistentConfigurationForIndexElementForSearch( relativeEventContext(), e.getMessage(),
					indexesEventContext(), e );
		}
	}
}
