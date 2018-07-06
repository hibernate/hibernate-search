/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model;

import org.apache.lucene.index.IndexableField;

/**
 * An extractor extracting the value from a native Lucene field.
 *
 * @param <V> The type of the value.
 */
public interface LuceneFieldValueExtractor<V> {

	/**
	 * Extract the value from the Lucene field.
	 *
	 * @param field The first field contributed to the schema.
	 * @return The extracted value.
	 */
	V extract(IndexableField field);

}
