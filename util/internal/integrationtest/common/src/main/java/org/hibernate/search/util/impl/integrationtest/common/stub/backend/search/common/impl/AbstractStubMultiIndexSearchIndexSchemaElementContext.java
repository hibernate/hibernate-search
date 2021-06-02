/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl;

import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.reporting.EventContext;

public abstract class AbstractStubMultiIndexSearchIndexSchemaElementContext<E extends StubSearchIndexSchemaElementContext>
		implements StubSearchIndexSchemaElementContext {

	protected final StubSearchIndexScope scope;
	protected final String absolutePath;
	protected final List<E> elementForEachIndex;

	public AbstractStubMultiIndexSearchIndexSchemaElementContext(StubSearchIndexScope scope,
			String absolutePath, List<E> elementForEachIndex) {
		this.scope = scope;
		this.absolutePath = absolutePath;
		this.elementForEachIndex = elementForEachIndex;
	}

	@Override
	public final String absolutePath() {
		return absolutePath;
	}

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

	public final <T> StubSearchQueryElementFactory<T> queryElementFactory(SearchQueryElementTypeKey<T> key) {
		StubSearchQueryElementFactory<T> factory = null;
		for ( E indexElement : elementForEachIndex ) {
			StubSearchQueryElementFactory<T> factoryForIndexElement =
					queryElementFactory( indexElement, key );
			if ( factory == null ) {
				factory = factoryForIndexElement;
			}
			else {
				checkFactoryCompatibility( key, factory, factoryForIndexElement );
			}
		}
		return factory;
	}

	protected abstract <T> StubSearchQueryElementFactory<T> queryElementFactory(E indexElement,
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
			StubSearchQueryElementFactory<T> factory1,
			StubSearchQueryElementFactory<T> factory2) {
		if ( factory1 == null && factory2 == null ) {
			return;
		}
		try {
			try {
				if ( factory1 == null || factory2 == null ) {
					throw new SearchException(
							String.format( Locale.ROOT,
									"'%1$s' can be used in some of the targeted indexes, but not all of them.", key ) );
				}

				factory1.checkCompatibleWith( factory2 );
			}
			catch (SearchException e) {
				throw new SearchException(
						String.format( Locale.ROOT, "Inconsistent support for '%1$s': %2$s", key.toString(), e.getMessage() ), e );
			}
		}
		catch (SearchException e) {
			throw new SearchException(
					String.format( Locale.ROOT,
							"Inconsistent configuration for %1$s in a search query across multiple indexes: %2$s",
							relativeEventContext(), e.getMessage()
					),
					e, indexesEventContext()
			);
		}
	}

	protected final <T> void checkAttributeCompatibility(BiPredicate<T, T> compatibilityChecker, String attributeName,
			T attribute1, T attribute2) {
		try {
			if ( !compatibilityChecker.test( attribute1, attribute2 ) ) {
				throw new SearchException(
						String.format( Locale.ROOT, "Attribute '%1$s' differs: '%2$s' vs. '%3$s'.", attributeName, attribute1,
								attribute2
						) );
			}
		}
		catch (SearchException e) {
			throw new SearchException( String.format( Locale.ROOT,
					"Inconsistent configuration for %1$s in a search query across multiple indexes: %2$s",
					absolutePath,
					e.getMessage()
			), e );
		}
	}
}
