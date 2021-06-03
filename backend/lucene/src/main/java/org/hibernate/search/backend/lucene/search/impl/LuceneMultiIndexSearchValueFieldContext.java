/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;

public class LuceneMultiIndexSearchValueFieldContext<F>
		extends AbstractLuceneMultiIndexSearchIndexSchemaElementContext<LuceneSearchValueFieldContext<F>>
		implements LuceneSearchValueFieldContext<F>, LuceneSearchValueFieldTypeContext<F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public LuceneMultiIndexSearchValueFieldContext(LuceneSearchIndexScope scope, String absolutePath,
			List<LuceneSearchValueFieldContext<F>> elementForEachIndex) {
		super( scope, absolutePath, elementForEachIndex );
	}

	@Override
	protected LuceneSearchValueFieldContext<F> self() {
		return this;
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
	public String nestedDocumentPath() {
		return getFromElementIfCompatible( LuceneSearchValueFieldContext::nestedDocumentPath, Object::equals,
				"nestedDocumentPath" );
	}

	@Override
	public boolean multiValuedInRoot() {
		for ( LuceneSearchValueFieldContext<F> field : elementForEachIndex ) {
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
	protected String missingSupportHint(String queryElementName) {
		return log.missingSupportHintForValueField( queryElementName );
	}

	@Override
	protected String partialSupportHint() {
		return log.partialSupportHintForValueField();
	}

	@Override
	protected <T> LuceneSearchQueryElementFactory<T, LuceneSearchValueFieldContext<F>> queryElementFactory(
			LuceneSearchValueFieldContext<F> indexElement, SearchQueryElementTypeKey<T> key) {
		return indexElement.type().queryElementFactory( key );
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

	private <T> T getFromTypeIfCompatible(Function<LuceneSearchValueFieldTypeContext<F>, T> getter,
			BiPredicate<T, T> compatibilityChecker, String attributeName) {
		T attribute = null;
		for ( LuceneSearchValueFieldContext<F> indexElement : elementForEachIndex ) {
			LuceneSearchValueFieldTypeContext<F> fieldType = indexElement.type();
			T attributeForIndexElement = getter.apply( fieldType );
			if ( attribute == null ) {
				attribute = attributeForIndexElement;
			}
			else {
				checkAttributeCompatibility( compatibilityChecker, attributeName, attribute, attributeForIndexElement );
			}
		}
		return attribute;
	}

}
