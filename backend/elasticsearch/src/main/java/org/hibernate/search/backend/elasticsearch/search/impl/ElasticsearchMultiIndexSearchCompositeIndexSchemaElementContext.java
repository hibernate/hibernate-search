/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

public final class ElasticsearchMultiIndexSearchCompositeIndexSchemaElementContext
		implements ElasticsearchSearchCompositeIndexSchemaElementContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Set<String> indexNames;
	private final String absolutePath;
	private final List<ElasticsearchSearchCompositeIndexSchemaElementContext> fieldForEachIndex;

	public ElasticsearchMultiIndexSearchCompositeIndexSchemaElementContext(Set<String> indexNames,
			String absolutePath,
			List<ElasticsearchSearchCompositeIndexSchemaElementContext> elementForEachIndex) {
		this.indexNames = indexNames;
		this.absolutePath = absolutePath;
		this.fieldForEachIndex = elementForEachIndex;
	}

	@Override
	public String absolutePath() {
		return absolutePath;
	}

	@Override
	public String absolutePath(String relativeFieldName) {
		return FieldPaths.compose( absolutePath, relativeFieldName );
	}

	@Override
	public List<String> nestedPathHierarchy() {
		return getFromElementIfCompatible( ElasticsearchSearchCompositeIndexSchemaElementContext::nestedPathHierarchy, Object::equals,
				"nestedPathHierarchy" );
	}

	@Override
	public EventContext eventContext() {
		return indexesEventContext().append( relativeEventContext() );
	}

	private EventContext indexesEventContext() {
		return EventContexts.fromIndexNames( indexNames );
	}

	private EventContext relativeEventContext() {
		return absolutePath == null ? EventContexts.indexSchemaRoot()
				: EventContexts.fromIndexFieldAbsolutePath( absolutePath );
	}

	@Override
	public <T> T queryElement(SearchQueryElementTypeKey<T> key, ElasticsearchSearchIndexScope scope) {
		AbstractElasticsearchSearchCompositeIndexSchemaElementQueryElementFactory<T> factory = queryElementFactory( key );
		if ( factory == null ) {
			throw log.cannotUseQueryElementForCompositeIndexElement( relativeEventContext(), key.toString(),
					indexesEventContext() );
		}
		try {
			return factory.create( scope, this );
		}
		catch (SearchException e) {
			throw log.cannotUseQueryElementForCompositeIndexElementBecauseCreationException( relativeEventContext(), key.toString(),
					e.getMessage(), e, eventContext() );
		}
	}

	@Override
	public boolean isComposite() {
		return true;
	}

	@Override
	public ElasticsearchSearchCompositeIndexSchemaElementContext toComposite() {
		return this;
	}

	@Override
	public boolean nested() {
		return getFromElementIfCompatible( ElasticsearchSearchCompositeIndexSchemaElementContext::nested, Object::equals, "nested" );
	}

	@Override
	public <T> AbstractElasticsearchSearchCompositeIndexSchemaElementQueryElementFactory<T> queryElementFactory(SearchQueryElementTypeKey<T> key) {
		AbstractElasticsearchSearchCompositeIndexSchemaElementQueryElementFactory<T> factory = null;
		for ( ElasticsearchSearchCompositeIndexSchemaElementContext fieldContext : fieldForEachIndex ) {
			AbstractElasticsearchSearchCompositeIndexSchemaElementQueryElementFactory<T> factoryForFieldContext =
					fieldContext.queryElementFactory( key );
			if ( factory == null ) {
				factory = factoryForFieldContext;
			}
			else {
				checkFactoryCompatibility( key, factory, factoryForFieldContext );
			}
		}
		return factory;
	}

	private <T> T getFromElementIfCompatible(Function<ElasticsearchSearchCompositeIndexSchemaElementContext, T> getter,
			BiPredicate<T, T> compatibilityChecker, String attributeName) {
		T attribute = null;
		for ( ElasticsearchSearchCompositeIndexSchemaElementContext fieldContext : fieldForEachIndex ) {
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

	private <T> void checkFactoryCompatibility(SearchQueryElementTypeKey<T> key,
			AbstractElasticsearchSearchCompositeIndexSchemaElementQueryElementFactory<T> factory1,
			AbstractElasticsearchSearchCompositeIndexSchemaElementQueryElementFactory<T> factory2) {
		if ( factory1 == null && factory2 == null ) {
			return;
		}
		try {
			try {
				if ( factory1 == null || factory2 == null ) {
					throw log.partialSupportForQueryElementInCompositeIndexElement( key.toString() );
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
