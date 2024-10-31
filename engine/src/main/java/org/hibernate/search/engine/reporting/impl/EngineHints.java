/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.reporting.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

@MessageBundle(projectCode = MessageConstants.PROJECT_CODE)
public interface EngineHints {

	EngineHints INSTANCE = Messages.getBundle( MethodHandles.lookup(), EngineHints.class );

	@Message(value = "The default backend can be retrieved")
	String defaultBackendAvailable();

	@Message(value = "The default backend cannot be retrieved, because no entity is mapped to that backend")
	String defaultBackendUnavailable();

	@Message(
			value = "Make sure the field is marked as searchable/sortable/projectable/aggregable/highlightable (whichever is relevant)."
					+ " If it already is, then '%1$s' is not available for fields of this type.")
	String missingSupportHintForValueField(SearchQueryElementTypeKey<?> key);

	@Message(value = "Some object field features require a nested structure;"
			+ " try setting the field structure to 'NESTED' and reindexing all your data."
			+ " If you are trying to use another feature, it probably isn't available for this field.")
	String missingSupportHintForCompositeNode();

	@Message(value = "Make sure the field is marked as searchable/sortable/projectable/aggregable/highlightable"
			+ " (whichever is relevant) in all indexes,"
			+ " and that the field has the same type in all indexes.")
	String partialSupportHintForValueField();

	@Message(value = "If you are trying to use the 'nested' predicate,"
			+ " set the field structure is to 'NESTED' in all indexes, then reindex all your data.")
	String partialSupportHintForCompositeNode();
}
