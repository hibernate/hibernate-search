/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl.nullencoding;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.hibernate.search.bridge.LuceneOptions;

/**
 * Contract to manage coding and decoding (querying) of null values.
 * Null values can be stored as special markers or skipping the field
 * altogether.
 * When encoding a marker the index field type shall match the other fields,
 * as field types often need to be consistent across the whole index.
 * Also the strategy to find a match for such a null-encoded value might vary.
 *
 * @author Sanne Grinovero
 */
public interface NullMarkerCodec {

	/**
	 * This is mostly a requirement for integration with other old-style
	 * contracts which expect a strongly String based strategy.
	 *
	 * @return a string representation of the null-marker, or null if no marker is used.
	 */
	String nullRepresentedAsString();

	/**
	 * Store the null marker in the Document.
	 * Some implementations might not do anything
	 *
	 * @param fieldName the name of the field
	 * @param document the document where to store the null marker
	 * @param luceneOptions indexing options
	 */
	void encodeNullValue(String fieldName, Document document, LuceneOptions luceneOptions);

	/**
	 * Create a Query to find all documents which have a 'null' value encoded in the specified field
	 *
	 * @param fieldName the field to target with the Query
	 * @return a new Lucene Query
	 */
	Query createNullMatchingQuery(String fieldName);

	/**
	 * Check if the field represents the encoding for a null element
	 *
	 * @param field the fields to check
	 * @return true if the field argument is representing the encoding for a null element.
	 */
	boolean representsNullValue(IndexableField field);

}
