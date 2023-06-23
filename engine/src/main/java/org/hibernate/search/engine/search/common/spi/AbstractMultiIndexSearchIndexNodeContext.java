/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.common.spi;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

public abstract class AbstractMultiIndexSearchIndexNodeContext<
		S extends SearchIndexNodeContext<SC>,
		SC extends SearchIndexScope<?>,
		NT extends SearchIndexNodeTypeContext<SC, S>>
		implements SearchIndexNodeContext<SC>, SearchIndexNodeTypeContext<SC, S> {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final SC scope;
	protected final String absolutePath;
	protected final List<? extends S> nodeForEachIndex;

	AbstractMultiIndexSearchIndexNodeContext(SC scope, String absolutePath, List<? extends S> nodeForEachIndex) {
		this.scope = scope;
		this.absolutePath = absolutePath;
		this.nodeForEachIndex = nodeForEachIndex;
	}

	protected abstract S self();

	protected abstract NT selfAsNodeType();

	protected abstract NT typeOf(S indexElement);

	@Override
	public final String absolutePath() {
		return absolutePath;
	}

	@Override
	public final String[] absolutePathComponents() {
		// The path is the same for all fields, so we just pick the first one.
		return nodeForEachIndex.get( 0 ).absolutePathComponents();
	}

	@Override
	public final List<String> nestedPathHierarchy() {
		return fromNodeIfCompatible( SearchIndexNodeContext::nestedPathHierarchy,
				Object::equals, "nestedPathHierarchy" );
	}

	@Override
	public String nestedDocumentPath() {
		return fromNodeIfCompatible( SearchIndexNodeContext::nestedDocumentPath,
				Object::equals, "nestedDocumentPath" );
	}

	@Override
	public String closestMultiValuedParentAbsolutePath() {
		return fromNodeIfCompatible( SearchIndexNodeContext::closestMultiValuedParentAbsolutePath,
				Objects::equals, "closestMultiValuedParentAbsolutePath" );
	}

	@Override
	public boolean multiValued() {
		for ( S field : nodeForEachIndex ) {
			if ( field.multiValued() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public final boolean multiValuedInRoot() {
		for ( S field : nodeForEachIndex ) {
			if ( field.multiValuedInRoot() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public final EventContext eventContext() {
		return indexesEventContext().append( relativeEventContext() );
	}

	protected final EventContext indexesEventContext() {
		return scope.eventContext();
	}

	@Override
	public final EventContext relativeEventContext() {
		return absolutePath == null
				? EventContexts.indexSchemaRoot()
				: EventContexts.fromIndexFieldAbsolutePath( absolutePath );
	}

	@Override
	public final <T> T queryElement(SearchQueryElementTypeKey<T> key, SC scope) {
		SearchQueryElementFactory<? extends T, ? super SC, ? super S> factory = queryElementFactory( key );
		return helper().queryElement( key, factory, scope, self() );
	}

	@Override
	public SearchException cannotUseQueryElement(SearchQueryElementTypeKey<?> key, String hint, Exception causeOrNull) {
		return helper().cannotUseQueryElement( key, self(), hint, causeOrNull );
	}

	abstract SearchIndexSchemaElementContextHelper helper();

	@Override
	public final <T> SearchQueryElementFactory<? extends T, ? super SC, ? super S> queryElementFactory(
			SearchQueryElementTypeKey<T> key) {
		SearchQueryElementFactory<? extends T, ? super SC, ? super S> factory = null;
		for ( S indexElement : nodeForEachIndex ) {
			SearchQueryElementFactory<? extends T, ? super SC, ? super S> factoryForIndexElement =
					typeOf( indexElement ).queryElementFactory( key );
			if ( factory == null ) {
				factory = factoryForIndexElement;
			}
			else {
				checkFactoryCompatibility( key, factory, factoryForIndexElement );
			}
		}
		return factory;
	}

	protected final <T> T fromNodeIfCompatible(Function<S, T> getter, BiPredicate<T, T> compatibilityChecker,
			String attributeName) {
		T attribute = null;
		for ( S indexElement : nodeForEachIndex ) {
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

	protected final <T> T fromTypeIfCompatible(Function<NT, T> getter,
			BiPredicate<T, T> compatibilityChecker, String attributeName) {
		T attribute = null;
		for ( S indexElement : nodeForEachIndex ) {
			NT fieldType = typeOf( indexElement );
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

	private <T> void checkFactoryCompatibility(SearchQueryElementTypeKey<T> key,
			SearchQueryElementFactory<? extends T, ? super SC, ? super S> factory1,
			SearchQueryElementFactory<? extends T, ? super SC, ? super S> factory2) {
		if ( factory1 == null && factory2 == null ) {
			return;
		}
		try {
			if ( factory1 == null || factory2 == null ) {
				throw log.partialSupportForQueryElement(
						key, helper().partialSupportHint() );
			}

			factory1.checkCompatibleWith( factory2 );
		}
		catch (SearchException e) {
			SearchException inconsistentSupportException = log.inconsistentSupportForQueryElement( key, e.getMessage(), e );
			throw log.inconsistentConfigurationInContextForSearch( relativeEventContext(),
					inconsistentSupportException.getMessage(), indexesEventContext(), inconsistentSupportException );
		}
	}

	final <T> void checkAttributeCompatibility(BiPredicate<T, T> compatibilityChecker, String attributeName,
			T attribute1, T attribute2) {
		try {
			if ( !compatibilityChecker.test( attribute1, attribute2 ) ) {
				throw log.differentAttribute( attributeName, attribute1, attribute2 );
			}
		}
		catch (SearchException e) {
			throw log.inconsistentConfigurationInContextForSearch( relativeEventContext(), e.getMessage(),
					indexesEventContext(), e );
		}
	}
}
