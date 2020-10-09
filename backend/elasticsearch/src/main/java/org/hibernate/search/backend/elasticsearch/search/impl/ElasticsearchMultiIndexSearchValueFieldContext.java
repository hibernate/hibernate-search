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
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

public class ElasticsearchMultiIndexSearchValueFieldContext<F>
		implements ElasticsearchSearchValueFieldContext<F>, ElasticsearchSearchValueFieldTypeContext<F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Set<String> indexNames;
	private final String absolutePath;
	private final List<ElasticsearchSearchValueFieldContext<F>> fieldForEachIndex;

	public ElasticsearchMultiIndexSearchValueFieldContext(Set<String> indexNames, String absolutePath,
			List<ElasticsearchSearchValueFieldContext<F>> fieldForEachIndex) {
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
		return getFromFieldIfCompatible( ElasticsearchSearchValueFieldContext::nestedPathHierarchy, Object::equals,
				"nestedPathHierarchy" );
	}

	@Override
	public boolean multiValuedInRoot() {
		for ( ElasticsearchSearchValueFieldContext<F> field : fieldForEachIndex ) {
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
	public EventContext eventContext() {
		return indexesEventContext()
				.append( EventContexts.fromIndexFieldAbsolutePath( absolutePath ) );
	}

	private EventContext indexesEventContext() {
		return EventContexts.fromIndexNames( indexNames );
	}

	@Override
	public <T> T queryElement(SearchQueryElementTypeKey<T> key, ElasticsearchSearchContext searchContext) {
		ElasticsearchSearchValueFieldQueryElementFactory<T, F> factory = type().queryElementFactory( key );
		if ( factory == null ) {
			throw log.cannotUseQueryElementForField( absolutePath(), key.toString(), eventContext() );
		}
		return factory.create( searchContext, this );
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
		for ( ElasticsearchSearchValueFieldContext<F> fieldContext : fieldForEachIndex ) {
			if ( fieldContext.type().hasNormalizerOnAtLeastOneIndex() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public <T> ElasticsearchSearchValueFieldQueryElementFactory<T, F> queryElementFactory(SearchQueryElementTypeKey<T> key) {
		ElasticsearchSearchValueFieldQueryElementFactory<T, F> factory = null;
		for ( ElasticsearchSearchValueFieldContext<F> fieldContext : fieldForEachIndex ) {
			ElasticsearchSearchValueFieldTypeContext<F> fieldType = fieldContext.type();
			ElasticsearchSearchValueFieldQueryElementFactory<T, F> factoryForFieldContext =
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

	private <T> T getFromFieldIfCompatible(Function<ElasticsearchSearchValueFieldContext<F>, T> getter,
			BiPredicate<T, T> compatiblityChecker, String attributeName) {
		T attribute = null;
		for ( ElasticsearchSearchValueFieldContext<F> fieldContext : fieldForEachIndex ) {
			T attributeForFieldContext = getter.apply( fieldContext );
			if ( attribute == null ) {
				attribute = attributeForFieldContext;
			}
			else {
				checkAttributeCompatibility( compatiblityChecker, attributeName, attribute, attributeForFieldContext );
			}
		}
		return attribute;
	}

	private <T> T getFromTypeIfCompatible(Function<ElasticsearchSearchValueFieldTypeContext<F>, T> getter,
			BiPredicate<T, T> compatiblityChecker, String attributeName) {
		T attribute = null;
		for ( ElasticsearchSearchValueFieldContext<F> fieldContext : fieldForEachIndex ) {
			ElasticsearchSearchValueFieldTypeContext<F> fieldType = fieldContext.type();
			T attributeForFieldContext = getter.apply( fieldType );
			if ( attribute == null ) {
				attribute = attributeForFieldContext;
			}
			else {
				checkAttributeCompatibility( compatiblityChecker, attributeName, attribute, attributeForFieldContext );
			}
		}
		return attribute;
	}

	private <T> void checkFactoryCompatibility(SearchQueryElementTypeKey<T> key,
			ElasticsearchSearchValueFieldQueryElementFactory<T, F> factory1,
			ElasticsearchSearchValueFieldQueryElementFactory<T, F> factory2) {
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
			throw log.inconsistentConfigurationForFieldForSearch( absolutePath, e.getMessage(), indexesEventContext(), e );
		}
	}

	private <T> void checkAttributeCompatibility(BiPredicate<T, T> compatiblityChecker, String attributeName,
			T attribute1, T attribute2) {
		try {
			if ( !compatiblityChecker.test( attribute1, attribute2 ) ) {
				throw log.differentFieldAttribute( attributeName, attribute1, attribute2 );
			}
		}
		catch (SearchException e) {
			throw log.inconsistentConfigurationForFieldForSearch( absolutePath, e.getMessage(), indexesEventContext(), e );
		}
	}
}
