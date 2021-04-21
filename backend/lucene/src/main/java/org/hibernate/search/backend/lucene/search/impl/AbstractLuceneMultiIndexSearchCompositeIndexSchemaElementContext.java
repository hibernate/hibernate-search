/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

abstract class AbstractLuceneMultiIndexSearchCompositeIndexSchemaElementContext
		implements LuceneSearchCompositeIndexSchemaElementContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSearchIndexesContext indexesContext;
	private final List<LuceneSearchCompositeIndexSchemaElementContext> fieldForEachIndex;

	private Map<String, LuceneSearchIndexSchemaElementContext> staticChildrenByName;

	public AbstractLuceneMultiIndexSearchCompositeIndexSchemaElementContext(LuceneSearchIndexesContext indexesContext,
			List<LuceneSearchCompositeIndexSchemaElementContext> elementForEachIndex) {
		this.indexesContext = indexesContext;
		this.fieldForEachIndex = elementForEachIndex;
	}

	@Override
	public List<String> nestedPathHierarchy() {
		return getFromFieldIfCompatible( LuceneSearchCompositeIndexSchemaElementContext::nestedPathHierarchy, Object::equals,
				"nestedPathHierarchy" );
	}

	@Override
	public EventContext eventContext() {
		return indexesEventContext().append( relativeEventContext() );
	}

	private EventContext indexesEventContext() {
		return EventContexts.fromIndexNames( indexesContext.indexNames() );
	}

	protected abstract EventContext relativeEventContext();

	@Override
	public <T> T queryElement(SearchQueryElementTypeKey<T> key, LuceneSearchContext searchContext) {
		LuceneSearchCompositeIndexSchemaElementQueryElementFactory<T> factory = queryElementFactory( key );
		if ( factory == null ) {
			throw log.cannotUseQueryElementForObjectField( absolutePath(), key.toString(), eventContext() );
		}
		try {
			return factory.create( searchContext, this );
		}
		catch (SearchException e) {
			throw log.cannotUseQueryElementForObjectFieldBecauseCreationException( absolutePath(), key.toString(),
					e.getMessage(), e, eventContext() );
		}
	}

	@Override
	public Map<String, ? extends LuceneSearchIndexSchemaElementContext> staticChildrenByName() {
		if ( staticChildrenByName != null ) {
			return staticChildrenByName;
		}

		// TODO HSEARCH-4050 remove this unnecessary restriction?
		getFromFieldIfCompatible( field -> field.staticChildrenByName().keySet(),
				Object::equals, "staticChildren" );

		Map<String, LuceneSearchIndexSchemaElementContext> result = new TreeMap<>();
		Function<String, LuceneSearchIndexSchemaElementContext> createChildFieldContext = indexesContext::field;
		for ( LuceneSearchCompositeIndexSchemaElementContext fieldContext : fieldForEachIndex ) {
			for ( LuceneSearchIndexSchemaElementContext child : fieldContext.staticChildrenByName().values() ) {
				try {
					result.computeIfAbsent( child.absolutePath(), createChildFieldContext );
				}
				catch (SearchException e) {
					throw log.inconsistentConfigurationForFieldForSearch( absolutePath(), e.getMessage(),
							indexesEventContext(), e );
				}
			}
		}
		// Only set this to a non-null value if we didn't detect any conflict during the loop.
		// If there was a conflict, we want the next call to this method to go through the loop again
		// and throw an exception again.
		staticChildrenByName = result;
		return staticChildrenByName;
	}

	@Override
	public boolean nested() {
		return getFromFieldIfCompatible( LuceneSearchCompositeIndexSchemaElementContext::nested, Object::equals, "nested" );
	}

	@Override
	public <T> LuceneSearchCompositeIndexSchemaElementQueryElementFactory<T> queryElementFactory(SearchQueryElementTypeKey<T> key) {
		LuceneSearchCompositeIndexSchemaElementQueryElementFactory<T> factory = null;
		for ( LuceneSearchCompositeIndexSchemaElementContext fieldContext : fieldForEachIndex ) {
			LuceneSearchCompositeIndexSchemaElementQueryElementFactory<T> factoryForFieldContext =
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

	private <T> T getFromFieldIfCompatible(Function<LuceneSearchCompositeIndexSchemaElementContext, T> getter,
			BiPredicate<T, T> compatiblityChecker, String attributeName) {
		T attribute = null;
		for ( LuceneSearchCompositeIndexSchemaElementContext fieldContext : fieldForEachIndex ) {
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

	private <T> void checkFactoryCompatibility(SearchQueryElementTypeKey<T> key,
			LuceneSearchCompositeIndexSchemaElementQueryElementFactory<T> factory1,
			LuceneSearchCompositeIndexSchemaElementQueryElementFactory<T> factory2) {
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
			throw log.inconsistentConfigurationForFieldForSearch( absolutePath(), e.getMessage(), indexesEventContext(), e );
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
			throw log.inconsistentConfigurationForFieldForSearch( absolutePath(), e.getMessage(), indexesEventContext(), e );
		}
	}
}
