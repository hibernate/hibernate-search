/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 *  Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 *  indicated by the @author tags or express copyright attribution
 *  statements applied by the authors.  All third-party contributions are
 *  distributed under license by Red Hat, Inc.
 *
 *  This copyrighted material is made available to anyone wishing to use, modify,
 *  copy, or redistribute it subject to the terms and conditions of the GNU
 *  Lesser General Public License, as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this distribution; if not, write to:
 *  Free Software Foundation, Inc.
 *  51 Franklin Street, Fifth Floor
 *  Boston, MA  02110-1301  USA
 */
package org.hibernate.search.bridge;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;

/**
 * A helper class for building Field objects and associating them to Documents.
 * A wrapper class for Lucene parameters needed for indexing.
 *
 * The recommended approach to index is to use {@link #addFieldToDocument(String, String, org.apache.lucene.document.Document)}
 * as all the options delcared by the user are transparently carried over. Compression is also provided transparently.

 * {code}
 * String fieldValue = convertToString(value);
 * luceneOptions.addFieldToDocument(name, fieldValue, document);
 * {code}
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
public interface LuceneOptions {

	void addNumericFieldToDocument(NumericField numericField, Document document);

	NumericField createNumericField(String name);

	/**
	 * Add a new field with the name {@code fieldName} to the Lucene Document {@code document} using the value
	 * {@code indexedString}.
	 * If the indexedString is null then the field is not added to the document.
	 *
	 * The field options are following the user declaration:
	 *  - stored or not
	 *  - compressed or not
	 *  - what type of indexing strategy
	 *  - what type of term vector strategy
	 *
	 * @param fieldName The field name
	 * @param indexedString The value to index
	 * @param document the document to which to add the the new field
	 */
	void addFieldToDocument(String fieldName, String indexedString, Document document);

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
	 * Return the storage strategy declared by the user
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
	 */
	Field.Store getStore();

	/**
	 * Prefer the use of {@link #addFieldToDocument(String, String, org.apache.lucene.document.Document)}
	 * over manually building your Field objects and adding them to the Document.
	 *
	 * Return the indexing strategy declared by the user
	 */
	Field.Index getIndex();

	/**
	 * Prefer the use of {@link #addFieldToDocument(String, String, org.apache.lucene.document.Document)}
	 * over manually building your Field objects and adding them to the Document.
	 *
	 * Return the term vector strategy declared by the user
	 */
	Field.TermVector getTermVector();

	/**
	 * Prefer the use of {@link #addFieldToDocument(String, String, org.apache.lucene.document.Document)}
	 * over manually building your Field objects and adding them to the Document.
	 *
	 * Return the boost factor declared by the user
	 */
	Float getBoost();
}
