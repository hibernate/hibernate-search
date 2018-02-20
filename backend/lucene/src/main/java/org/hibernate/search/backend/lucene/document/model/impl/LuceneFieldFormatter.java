/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.util.Collections;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.SortField;
import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;

/**
 * @author Guillaume Smet
 */
public interface LuceneFieldFormatter<T> {

	void addFields(LuceneDocumentBuilder documentBuilder, LuceneIndexSchemaObjectNode parentNode, String fieldName, T value);

	default Set<String> getOverriddenStoredFields() {
		return Collections.emptySet();
	}

	T parse(Document document, String fieldName);

	Object format(Object value);

	SortField.Type getDefaultSortFieldType();

	Object getSortMissingLast();

	Object getSortMissingFirst();
}
