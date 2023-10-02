/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.apache.lucene.index.IndexableField;

public interface LuceneDocumentContent {

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
