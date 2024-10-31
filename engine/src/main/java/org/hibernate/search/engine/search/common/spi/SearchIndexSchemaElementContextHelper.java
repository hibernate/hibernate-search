/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.common.spi;

import java.util.Objects;

import org.hibernate.search.engine.logging.impl.MappingLog;
import org.hibernate.search.engine.logging.impl.QueryLog;
import org.hibernate.search.engine.reporting.impl.EngineHints;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.reporting.EventContext;

public abstract class SearchIndexSchemaElementContextHelper {

	public static <T extends SearchIndexCompositeNodeContext<?>> T throwingToComposite(SearchIndexNodeContext<?> element) {
		throw MappingLog.INSTANCE.invalidIndexNodeTypeNotComposite( element.relativeEventContext() );
	}

	public static <T extends SearchIndexCompositeNodeContext<?>> T throwingToObjectField(SearchIndexNodeContext<?> element) {
		throw MappingLog.INSTANCE.invalidIndexNodeTypeNotObjectField( element.relativeEventContext() );
	}

	public static <T extends SearchIndexValueFieldContext<?>> T throwingToValueField(SearchIndexNodeContext<?> element) {
		throw MappingLog.INSTANCE.invalidIndexNodeTypeNotValueField( element.relativeEventContext() );
	}

	public static void checkNestedDocumentPathCompatibility(SearchIndexNodeContext<?> left, SearchIndexNodeContext<?> right) {
		String leftNestedDocumentPathHierarchy = left.nestedDocumentPath();
		String rightNestedDocumentPathHierarchy = right.nestedDocumentPath();

		if ( !Objects.equals( leftNestedDocumentPathHierarchy, rightNestedDocumentPathHierarchy ) ) {
			throw QueryLog.INSTANCE.targetFieldsSpanningMultipleNestedPaths(
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
			return EngineHints.INSTANCE.missingSupportHintForValueField( key );
		}

		@Override
		public String partialSupportHint() {
			return EngineHints.INSTANCE.partialSupportHintForValueField();
		}

	};

	public static final SearchIndexSchemaElementContextHelper COMPOSITE = new SearchIndexSchemaElementContextHelper() {
		@Override
		protected String missingSupportHint(SearchQueryElementTypeKey<?> key) {
			return EngineHints.INSTANCE.missingSupportHintForCompositeNode();
		}

		@Override
		public String partialSupportHint() {
			return EngineHints.INSTANCE.partialSupportHintForCompositeNode();
		}
	};

	public <T, SC extends SearchIndexScope<?>, N extends SearchIndexNodeContext<SC>> T queryElement(
			SearchQueryElementTypeKey<T> key,
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

	public <T, SC extends SearchIndexScope<?>, N extends SearchIndexNodeContext<SC>> SearchException cannotUseQueryElement(
			SearchQueryElementTypeKey<T> key, N node, String hint,
			Exception causeOrNull) {
		throw QueryLog.INSTANCE.cannotUseQueryElementForIndexNode( node.relativeEventContext(), key,
				hint, node.eventContext(), causeOrNull );
	}

	protected abstract String missingSupportHint(SearchQueryElementTypeKey<?> key);

	public abstract String partialSupportHint();
}
