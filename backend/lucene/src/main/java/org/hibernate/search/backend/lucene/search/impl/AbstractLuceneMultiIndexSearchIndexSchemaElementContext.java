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
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

abstract class AbstractLuceneMultiIndexSearchIndexSchemaElementContext<E extends LuceneSearchIndexSchemaElementContext>
		implements LuceneSearchIndexSchemaElementContext {
	protected static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );
	protected final LuceneSearchIndexScope scope;
	protected final String absolutePath;
	protected final List<E> elementForEachIndex;

	AbstractLuceneMultiIndexSearchIndexSchemaElementContext(LuceneSearchIndexScope scope, String absolutePath,
			List<E> elementForEachIndex) {
		this.scope = scope;
		this.absolutePath = absolutePath;
		this.elementForEachIndex = elementForEachIndex;
	}

	protected abstract E self();

	@Override
	public final EventContext eventContext() {
		return indexesEventContext().append( relativeEventContext() );
	}

	protected final EventContext indexesEventContext() {
		return EventContexts.fromIndexNames( scope.hibernateSearchIndexNames() );
	}

	protected final EventContext relativeEventContext() {
		return absolutePath == null ? EventContexts.indexSchemaRoot()
				: EventContexts.fromIndexFieldAbsolutePath( absolutePath );
	}

	@Override
	public final String absolutePath() {
		return absolutePath;
	}

	@Override
	public final List<String> nestedPathHierarchy() {
		return getFromElementIfCompatible( LuceneSearchIndexSchemaElementContext::nestedPathHierarchy,
				Object::equals, "nestedPathHierarchy" );
	}

	@Override
	public final <T> T queryElement(SearchQueryElementTypeKey<T> key, LuceneSearchIndexScope scope) {
		LuceneSearchQueryElementFactory<T, E> factory = queryElementFactory( key );
		if ( factory == null ) {
			throw log.cannotUseQueryElementForIndexElement( relativeEventContext(), key.toString(),
					missingSupportHint( key.toString() ), eventContext() );
		}
		try {
			return factory.create( scope, self() );
		}
		catch (SearchException e) {
			throw log.cannotUseQueryElementForIndexElementBecauseCreationException( relativeEventContext(),
					key.toString(), e.getMessage(), e, eventContext() );
		}
	}

	protected abstract String missingSupportHint(String queryElementName);

	public final <T> LuceneSearchQueryElementFactory<T, E> queryElementFactory(SearchQueryElementTypeKey<T> key) {
		LuceneSearchQueryElementFactory<T, E> factory = null;
		for ( E indexElement : elementForEachIndex ) {
			LuceneSearchQueryElementFactory<T, E> factoryForIndexElement = queryElementFactory( indexElement, key );
			if ( factory == null ) {
				factory = factoryForIndexElement;
			}
			else {
				checkFactoryCompatibility( key, factory, factoryForIndexElement );
			}
		}
		return factory;
	}

	protected abstract <T> LuceneSearchQueryElementFactory<T, E> queryElementFactory(E indexElement,
			SearchQueryElementTypeKey<T> key);

	protected final <T> T getFromElementIfCompatible(Function<E, T> getter, BiPredicate<T, T> compatibilityChecker,
			String attributeName) {
		T attribute = null;
		for ( E indexElement : elementForEachIndex ) {
			T attributeForIndexElement = getter.apply( indexElement );
			if ( attribute == null ) {
				attribute = attributeForIndexElement;
			}
			else {
				checkAttributeCompatibility( compatibilityChecker, attributeName, attribute, attributeForIndexElement );
			}
		}
		return attribute;
	}

	private <T> void checkFactoryCompatibility(SearchQueryElementTypeKey<T> key,
			LuceneSearchQueryElementFactory<T, E> factory1, LuceneSearchQueryElementFactory<T, E> factory2) {
		if ( factory1 == null && factory2 == null ) {
			return;
		}
		try {
			try {
				if ( factory1 == null || factory2 == null ) {
					throw log.partialSupportForQueryElement( key.toString(), partialSupportHint() );
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

	protected abstract String partialSupportHint();

	protected final <T> void checkAttributeCompatibility(BiPredicate<T, T> compatibilityChecker, String attributeName,
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
