/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.util.Collections;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;

/**
 * @author Guillaume Smet
 */
public interface LuceneFieldCodec<T> {

	void encode(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, T value);

	default Set<String> getOverriddenStoredFields() {
		return Collections.emptySet();
	}

	T decode(Document document, String absoluteFieldPath);

	// equals()/hashCode() needs to be implemented if the codec is not a singleton

	boolean equals(Object obj);

	int hashCode();
}
