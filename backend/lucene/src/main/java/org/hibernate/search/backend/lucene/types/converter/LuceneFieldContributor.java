/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.converter;

import java.util.function.Consumer;

import org.apache.lucene.index.IndexableField;

/**
 * A contributor adding native Lucene fields to the index schema.
 *
 * @param <F> The type of the value.
 */
public interface LuceneFieldContributor<F> {

	/**
	 * Contribute Lucene fields to the collector.
	 *
	 * @param absoluteFieldPath The absolute path of the field.
	 * @param value The value.
	 * @param collector The collector to which the fields are contributing.
	 */
	void contribute(String absoluteFieldPath, F value, Consumer<IndexableField> collector);

}
