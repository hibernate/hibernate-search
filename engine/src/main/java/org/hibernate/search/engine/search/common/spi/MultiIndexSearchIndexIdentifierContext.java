/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.common.spi;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.hibernate.search.engine.backend.scope.spi.AbstractSearchIndexScope;
import org.hibernate.search.engine.backend.types.converter.spi.DocumentIdentifierValueConverter;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

public class MultiIndexSearchIndexIdentifierContext
		implements SearchIndexIdentifierContext {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SearchIndexScope<?> scope;
	private final List<SearchIndexIdentifierContext> contextByIndex;

	public MultiIndexSearchIndexIdentifierContext(AbstractSearchIndexScope<?, ?, ?, ?> scope,
			List<SearchIndexIdentifierContext> contextByIndex) {
		this.scope = scope;
		this.contextByIndex = contextByIndex;
	}

	@Override
	public EventContext eventContext() {
		return scope.eventContext().append( relativeEventContext() );
	}

	@Override
	public EventContext relativeEventContext() {
		return EventContexts.indexSchemaIdentifier();
	}

	@Override
	public DocumentIdentifierValueConverter<?> dslConverter() {
		return fromContextsIfCompatible( SearchIndexIdentifierContext::dslConverter,
				DocumentIdentifierValueConverter::isCompatibleWith, "dslConverter" );
	}

	protected final <T> T fromContextsIfCompatible(Function<SearchIndexIdentifierContext, T> getter,
			BiPredicate<T, T> compatibilityChecker, String attributeName) {
		T attribute = null;
		for ( SearchIndexIdentifierContext context : contextByIndex ) {
			T attributeForContext = getter.apply( context );
			if ( attribute == null ) {
				attribute = attributeForContext;
			}
			else {
				checkAttributeCompatibility( compatibilityChecker, attributeName, attribute, attributeForContext );
			}
		}
		return attribute;
	}

	private <T> void checkAttributeCompatibility(BiPredicate<T, T> compatibilityChecker, String attributeName,
			T attribute1, T attribute2) {
		try {
			if ( !compatibilityChecker.test( attribute1, attribute2 ) ) {
				throw log.differentAttribute( attributeName, attribute1, attribute2 );
			}
		}
		catch (SearchException e) {
			throw log.inconsistentConfigurationInContextForSearch( relativeEventContext(), e.getMessage(),
					scope.eventContext(), e );
		}
	}
}
