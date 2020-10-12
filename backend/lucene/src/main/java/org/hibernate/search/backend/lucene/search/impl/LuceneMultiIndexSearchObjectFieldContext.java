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

public class LuceneMultiIndexSearchObjectFieldContext implements LuceneSearchObjectFieldContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSearchIndexesContext indexesContext;
	private final String absolutePath;
	private final List<LuceneSearchObjectFieldContext> fieldForEachIndex;

	private Map<String, LuceneSearchFieldContext> staticChildrenByName;

	public LuceneMultiIndexSearchObjectFieldContext(LuceneSearchIndexesContext indexesContext,
			String absolutePath, List<LuceneSearchObjectFieldContext> fieldForEachIndex) {
		this.indexesContext = indexesContext;
		this.absolutePath = absolutePath;
		this.fieldForEachIndex = fieldForEachIndex;
	}

	@Override
	public boolean isObjectField() {
		return true;
	}

	@Override
	public LuceneSearchObjectFieldContext toObjectField() {
		return this;
	}

	@Override
	public String absolutePath() {
		return absolutePath;
	}

	@Override
	public List<String> nestedPathHierarchy() {
		return getFromFieldIfCompatible( LuceneSearchObjectFieldContext::nestedPathHierarchy, Object::equals,
				"nestedPathHierarchy" );
	}

	@Override
	public EventContext eventContext() {
		return indexesEventContext()
				.append( EventContexts.fromIndexFieldAbsolutePath( absolutePath ) );
	}

	private EventContext indexesEventContext() {
		return EventContexts.fromIndexNames( indexesContext.indexNames() );
	}

	@Override
	public <T> T queryElement(SearchQueryElementTypeKey<T> key, LuceneSearchContext searchContext) {
		LuceneSearchObjectFieldQueryElementFactory<T> factory = queryElementFactory( key );
		if ( factory == null ) {
			throw log.cannotUseQueryElementForObjectField( absolutePath(), key.toString(), eventContext() );
		}
		return factory.create( searchContext, this );
	}

	@Override
	public Map<String, ? extends LuceneSearchFieldContext> staticChildrenByName() {
		if ( staticChildrenByName != null ) {
			return staticChildrenByName;
		}
		Map<String, LuceneSearchFieldContext> result = new TreeMap<>();
		Function<String, LuceneSearchFieldContext> createChildFieldContext = indexesContext::field;
		for ( LuceneSearchObjectFieldContext fieldContext : fieldForEachIndex ) {
			for ( LuceneSearchFieldContext child : fieldContext.staticChildrenByName().values() ) {
				try {
					result.computeIfAbsent( child.absolutePath(), createChildFieldContext );
				}
				catch (SearchException e) {
					throw log.inconsistentConfigurationForFieldForSearch( absolutePath, e.getMessage(),
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
		return getFromFieldIfCompatible( LuceneSearchObjectFieldContext::nested, Object::equals, "nested" );
	}

	@Override
	public <T> LuceneSearchObjectFieldQueryElementFactory<T> queryElementFactory(SearchQueryElementTypeKey<T> key) {
		LuceneSearchObjectFieldQueryElementFactory<T> factory = null;
		for ( LuceneSearchObjectFieldContext fieldContext : fieldForEachIndex ) {
			LuceneSearchObjectFieldQueryElementFactory<T> factoryForFieldContext =
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

	private <T> T getFromFieldIfCompatible(Function<LuceneSearchObjectFieldContext, T> getter,
			BiPredicate<T, T> compatiblityChecker, String attributeName) {
		T attribute = null;
		for ( LuceneSearchObjectFieldContext fieldContext : fieldForEachIndex ) {
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
			LuceneSearchObjectFieldQueryElementFactory<T> factory1,
			LuceneSearchObjectFieldQueryElementFactory<T> factory2) {
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
