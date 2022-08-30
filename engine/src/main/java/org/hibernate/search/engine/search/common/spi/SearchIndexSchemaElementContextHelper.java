/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.common.spi;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

public abstract class SearchIndexSchemaElementContextHelper {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static <T extends SearchIndexCompositeNodeContext<?>> T throwingToComposite(SearchIndexNodeContext<?> element) {
		throw log.invalidIndexNodeTypeNotComposite( element.relativeEventContext() );
	}

	public static <T extends SearchIndexCompositeNodeContext<?>> T throwingToObjectField(SearchIndexNodeContext<?> element) {
		throw log.invalidIndexNodeTypeNotObjectField( element.relativeEventContext() );
	}

	public static <T extends SearchIndexValueFieldContext<?>> T throwingToValueField(SearchIndexNodeContext<?> element) {
		throw log.invalidIndexNodeTypeNotValueField( element.relativeEventContext() );
	}

	public static void checkNestedDocumentPathCompatibility(SearchIndexNodeContext<?> left, SearchIndexNodeContext<?> right) {
		String leftNestedDocumentPathHierarchy = left.nestedDocumentPath();
		String rightNestedDocumentPathHierarchy = right.nestedDocumentPath();

		if ( !Objects.equals( leftNestedDocumentPathHierarchy, rightNestedDocumentPathHierarchy ) ) {
			throw log.targetFieldsSpanningMultipleNestedPaths(
					left.absolutePath(), pathEventContext( leftNestedDocumentPathHierarchy ),
					right.absolutePath(), pathEventContext( rightNestedDocumentPathHierarchy ) );
		}
	}

	private static EventContext pathEventContext(String path) {
		return path == null ? EventContexts.indexSchemaRoot() : EventContexts.fromIndexFieldAbsolutePath( path );
	}

	private SearchIndexSchemaElementContextHelper() {
	}

	public static final SearchIndexSchemaElementContextHelper VALUE_FIELD = new SearchIndexSchemaElementContextHelper() {
		@Override
		protected String missingSupportHint(SearchQueryElementTypeKey<?> key) {
			return log.missingSupportHintForValueField( key );
		}

		@Override
		public String partialSupportHint() {
			return log.partialSupportHintForValueField();
		}

	};

	public static final SearchIndexSchemaElementContextHelper COMPOSITE = new SearchIndexSchemaElementContextHelper() {
		@Override
		protected String missingSupportHint(SearchQueryElementTypeKey<?> key) {
			return log.missingSupportHintForCompositeNode();
		}

		@Override
		public String partialSupportHint() {
			return log.partialSupportHintForCompositeNode();
		}
	};

	public <T, SC extends SearchIndexScope<?>, N extends SearchIndexNodeContext<SC>>
			SearchException cannotUseQueryElement(SearchQueryElementTypeKey<T> key, N node, String hint,
			Exception causeOrNull) {
		throw log.cannotUseQueryElementForIndexNode( node.relativeEventContext(), key,
				hint, node.eventContext(), causeOrNull );
	}

	public <T, SC extends SearchIndexScope<?>, N extends SearchIndexNodeContext<SC>>
			T queryElement(SearchQueryElementTypeKey<T> key,
					SearchQueryElementFactory<? extends T, ? super SC, ? super N> factory, SC scope, N node) {
		if ( factory == null ) {
			throw cannotUseQueryElement( key, node, missingSupportHint( key ), null );
		}
		try {
			return factory.create( scope, node );
		}
		catch (SearchException e) {
			throw cannotUseQueryElement( key, node, e.getMessage(), e );
		}
	}

	protected abstract String missingSupportHint(SearchQueryElementTypeKey<?> key);

	public abstract String partialSupportHint();
}
