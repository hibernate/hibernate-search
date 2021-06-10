/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.common.impl;

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

public class LuceneMultiIndexSearchIndexValueFieldContext<F>
		extends AbstractLuceneMultiIndexSearchIndexNodeContext<LuceneSearchIndexValueFieldContext<F>>
		implements LuceneSearchIndexValueFieldContext<F>, LuceneSearchIndexValueFieldTypeContext<F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public LuceneMultiIndexSearchIndexValueFieldContext(LuceneSearchIndexScope scope, String absolutePath,
			List<LuceneSearchIndexValueFieldContext<F>> elementForEachIndex) {
		super( scope, absolutePath, elementForEachIndex );
	}

	@Override
	protected LuceneSearchIndexValueFieldContext<F> self() {
		return this;
	}

	@Override
	public boolean isComposite() {
		return false;
	}

	@Override
	public LuceneSearchIndexCompositeNodeContext toComposite() {
		throw log.invalidIndexElementTypeValueFieldIsNotObjectField( absolutePath );
	}

	@Override
	public String nestedDocumentPath() {
		return getFromElementIfCompatible( LuceneSearchIndexValueFieldContext::nestedDocumentPath, Object::equals,
				"nestedDocumentPath" );
	}

	@Override
	public boolean multiValuedInRoot() {
		for ( LuceneSearchIndexValueFieldContext<F> field : elementForEachIndex ) {
			if ( field.multiValuedInRoot() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public LuceneSearchIndexValueFieldTypeContext<F> type() {
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
	protected <T> LuceneSearchQueryElementFactory<T, LuceneSearchIndexValueFieldContext<F>> queryElementFactory(
			LuceneSearchIndexValueFieldContext<F> indexElement, SearchQueryElementTypeKey<T> key) {
		return indexElement.type().queryElementFactory( key );
	}

	@Override
	public DslConverter<?, F> dslConverter() {
		return getFromTypeIfCompatible( LuceneSearchIndexValueFieldTypeContext::dslConverter, DslConverter::isCompatibleWith,
				"dslConverter" );
	}

	@Override
	public DslConverter<F, F> rawDslConverter() {
		return getFromTypeIfCompatible( LuceneSearchIndexValueFieldTypeContext::rawDslConverter, DslConverter::isCompatibleWith,
				"rawDslConverter" );
	}

	@Override
	public ProjectionConverter<F, ?> projectionConverter() {
		return getFromTypeIfCompatible( LuceneSearchIndexValueFieldTypeContext::projectionConverter,
				ProjectionConverter::isCompatibleWith, "projectionConverter" );
	}

	@Override
	public ProjectionConverter<F, F> rawProjectionConverter() {
		return getFromTypeIfCompatible( LuceneSearchIndexValueFieldTypeContext::rawProjectionConverter,
				ProjectionConverter::isCompatibleWith, "rawProjectionConverter" );
	}

	@Override
	public Analyzer searchAnalyzerOrNormalizer() {
		return getFromTypeIfCompatible( LuceneSearchIndexValueFieldTypeContext::searchAnalyzerOrNormalizer, Object::equals,
				"searchAnalyzerOrNormalizer" );
	}

	private <T> T getFromTypeIfCompatible(Function<LuceneSearchIndexValueFieldTypeContext<F>, T> getter,
			BiPredicate<T, T> compatibilityChecker, String attributeName) {
		T attribute = null;
		for ( LuceneSearchIndexValueFieldContext<F> indexElement : elementForEachIndex ) {
			LuceneSearchIndexValueFieldTypeContext<F> fieldType = indexElement.type();
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
