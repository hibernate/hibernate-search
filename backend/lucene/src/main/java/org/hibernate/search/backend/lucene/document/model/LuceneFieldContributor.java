/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model;

import java.util.function.Consumer;

import org.apache.lucene.index.IndexableField;

/**
 * A contributor adding native Lucene fields to the index schema.
 *
 * @param <V> The type of the value.
 */
public interface LuceneFieldContributor<V> {

	/**
	 * Contribute Lucene fields to the collector.
	 *
	 * @param absoluteFieldPath The absolute path of the field.
	 * @param value The value.
	 * @param collector The collector to which the fields are contributing.
	 */
	void contribute(String absoluteFieldPath, V value, Consumer<IndexableField> collector);

}
