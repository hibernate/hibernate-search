/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.converter;

import org.apache.lucene.index.IndexableField;

/**
 * An extractor extracting the value from a native Lucene field.
 *
 * @param <F> The type of the value.
 */
public interface LuceneFieldValueExtractor<F> {

	/**
	 * Extract the value from the Lucene field.
	 *
	 * @param field The first field contributed to the schema.
	 * @return The extracted value.
	 */
	F extract(IndexableField field);

}
