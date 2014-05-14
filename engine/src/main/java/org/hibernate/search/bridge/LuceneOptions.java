/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

/**
 * A helper class for building Field objects and associating them to Documents.
 * A wrapper class for Lucene parameters needed for indexing.
 *
 * The recommended approach to index is to use {@link #addFieldToDocument(String, String, org.apache.lucene.document.Document)}
 * or {@link #addNumericFieldToDocument(String, Object, org.apache.lucene.document.Document)} as all the options declared by
 * the user are transparently carried over. Compression is also provided transparently.
 * <pre>
 * {@code String fieldValue = convertToString(value);
 * luceneOptions.addFieldToDocument(name, fieldValue, document);
 * // Numeric
 * Double aDouble = ...
 * luceneOptions.addNumericFieldToDocument(name, value, document);
 * }
 * </pre>
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
public interface LuceneOptions {

	/**
	 * Add a new field with the name {@code fieldName} to the Lucene Document {@code document} using the value
	 * {@code indexedString}.
	 * If the indexedString is null then the field is not added to the document.
	 *
	 * The field options are following the user declaration:
	 * <ul>
	 * <li> stored or not </li>
	 * <li> compressed or not </li>
	 * <li> what type of indexing strategy </li>
	 * <li> what type of term vector strategy </li>
	 * </ul>
	 *
	 * @param fieldName The field name
	 * @param indexedString The value to index
	 * @param document the document to which to add the the new field
	 */
	void addFieldToDocument(String fieldName, String indexedString, Document document);

	/**
	 * Add a new NumericField with the name {@code fieldName} to the Lucene Document {@code document}
	 * using the value {@code numericValue}. If the value is not numeric then the field is not added to the document
	 *
	 * @param fieldName The name of the field
	 * @param numericValue The numeric value, either an Int, Long, Float or Double
	 * @param document the document to which to add the the new field
	 */
	void addNumericFieldToDocument(String fieldName, Object numericValue, Document document);

	/**
	 * Prefer the use of {@link #addFieldToDocument(String, String, org.apache.lucene.document.Document)}
	 * over manually building your Field objects and adding them to the Document.
	 *
	 * To use compression either use #addFieldToDocument or refer
	 * to Lucene documentation to implement your own compression
	 * strategy.
	 *
	 * @return the compression strategy declared by the user {@code true} if the field value is compressed, {@code false} otherwise.
	 */
	boolean isCompressed();

	/**
	 * Prefer the use of {@link #addFieldToDocument(String, String, org.apache.lucene.document.Document)}
	 * over manually building your Field objects and adding them to the Document.
	 *
	 * {@code org.apache.lucene.document.Field.Store.YES} if the field is stored
	 * {@code org.apache.lucene.document.Field.Store.NO} otherwise.
	 *
	 * To determine if the field must be compressed, use {@link #isCompressed()}.
	 *
	 * Starting from version 3.3, Store.COMPRESS is no longer returned, use {@link #isCompressed()}
	 *
	 * To use compression either use #addFieldToDocument or refer
	 * to Lucene documentation to implement your own compression
	 * strategy.
	 *
	 * @return Returns the store strategy declared by the user
	 */
	Field.Store getStore();

	/**
	 * Prefer the use of {@link #addFieldToDocument(String, String, org.apache.lucene.document.Document)}
	 * over manually building your Field objects and adding them to the Document.
	 *
	 * @return Returns the indexing strategy declared by the user
	 */
	Field.Index getIndex();

	/**
	 * Prefer the use of {@link #addFieldToDocument(String, String, org.apache.lucene.document.Document)}
	 * over manually building your Field objects and adding them to the Document.
	 *
	 * @return Returns the term vector strategy declared by the user
	 */
	Field.TermVector getTermVector();

	/**
	 * Prefer the use of {@link #addFieldToDocument(String, String, org.apache.lucene.document.Document)}
	 * over manually building your Field objects and adding them to the Document.
	 *
	 * @return Returns the boost factor declared by the user
	 */
	float getBoost();

	/**
	 * @return Returns the string for indexing {@code null} values. {@code null} is returned in case no null token has
	 *         been specified.
	 */
	String indexNullAs();
}
