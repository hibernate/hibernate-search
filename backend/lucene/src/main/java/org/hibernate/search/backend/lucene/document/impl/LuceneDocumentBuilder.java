/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.impl;

import org.hibernate.search.engine.backend.document.DocumentElement;

import org.apache.lucene.index.IndexableField;


public interface LuceneDocumentBuilder extends DocumentElement {

	/**
	 * Add a field to this document.
	 * @param field The field to add.
	 */
	void addField(IndexableField field);

	/**
	 * Explicitly mark a field name as existing in this document.
	 * @param absoluteFieldPath The path of the field.
	 * May be different from {@code field.name()}, for example for geo-point fields.
	 */
	void addFieldName(String absoluteFieldPath);

}
