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
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

public class ElasticsearchMultiIndexSearchObjectFieldContext implements ElasticsearchSearchObjectFieldContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Set<String> indexNames;
	private final String absolutePath;
	private final List<ElasticsearchSearchObjectFieldContext> fieldForEachIndex;

	public ElasticsearchMultiIndexSearchObjectFieldContext(Set<String> indexNames, String absolutePath,
			List<ElasticsearchSearchObjectFieldContext> fieldForEachIndex) {
		this.indexNames = indexNames;
		this.absolutePath = absolutePath;
		this.fieldForEachIndex = fieldForEachIndex;
	}

	@Override
	public boolean isObjectField() {
		return true;
	}

	@Override
	public ElasticsearchSearchObjectFieldContext toObjectField() {
		return this;
	}

	@Override
	public String absolutePath() {
		return absolutePath;
	}

	@Override
	public List<String> nestedPathHierarchy() {
		return getFromFieldIfCompatible( ElasticsearchSearchObjectFieldContext::nestedPathHierarchy, Object::equals,
				"nestedPathHierarchy" );
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
		ElasticsearchSearchObjectFieldQueryElementFactory<T> factory = queryElementFactory( key );
		if ( factory == null ) {
			throw log.cannotUseQueryElementForObjectField( absolutePath(), key.toString(), eventContext() );
		}
		return factory.create( searchContext, this );
	}

	@Override
	public boolean nested() {
		return getFromFieldIfCompatible( ElasticsearchSearchObjectFieldContext::nested, Object::equals, "nested" );
	}

	@Override
	public <T> ElasticsearchSearchObjectFieldQueryElementFactory<T> queryElementFactory(SearchQueryElementTypeKey<T> key) {
		ElasticsearchSearchObjectFieldQueryElementFactory<T> factory = null;
		for ( ElasticsearchSearchObjectFieldContext fieldContext : fieldForEachIndex ) {
			ElasticsearchSearchObjectFieldQueryElementFactory<T> factoryForFieldContext =
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

	private <T> T getFromFieldIfCompatible(Function<ElasticsearchSearchObjectFieldContext, T> getter,
			BiPredicate<T, T> compatiblityChecker, String attributeName) {
		T attribute = null;
		for ( ElasticsearchSearchObjectFieldContext fieldContext : fieldForEachIndex ) {
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
			ElasticsearchSearchObjectFieldQueryElementFactory<T> factory1,
			ElasticsearchSearchObjectFieldQueryElementFactory<T> factory2) {
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
