/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.dsl;

import org.hibernate.search.backend.lucene.search.predicate.dsl.LuceneSearchPredicateFactory;
import org.hibernate.search.backend.lucene.search.sort.dsl.LuceneSearchSortFactory;
import org.hibernate.search.backend.lucene.types.converter.LuceneFieldContributor;
import org.hibernate.search.backend.lucene.types.converter.LuceneFieldValueExtractor;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeOptionsStep;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

public interface LuceneIndexFieldTypeFactory extends IndexFieldTypeFactory {

	/**
	 * Define a native field type.
	 * <p>
	 * A native field type has the following characteristics:
	 * <ul>
	 *     <li>Hibernate Search doesn't know its exact type, so it cannot be configured precisely,
	 *     except through the parameters passed to this method</li>
	 *     <li>When indexing, fields values will be passed to the {@link LuceneFieldContributor field contributor}.
	 *     This contributor will translate the value into {@link org.apache.lucene.index.IndexableField} instances
	 *     which will be added to the document.</li>
	 *     <li>The predicate/sort DSLs cannot be used on fields of this type.
	 *     It is recommended to create the predicate/sort/projections targeting these fields from native Lucene objects
	 *     using {@link LuceneSearchPredicateFactory#fromLuceneQuery(Query)}
	 *     or {@link LuceneSearchSortFactory#fromLuceneSort(Sort)}</li>
	 *     <li>The projection DSL can only be used on fields of this type of {@code fieldValueExtractor} is non-null.
	 *     When projecting, the value extractor will be passed the {@link org.apache.lucene.index.IndexableField}
	 *     and will return the corresponding projected value of type {@code F}.</li>
	 * </ul>
	 *
	 * @param valueType The type of the value.
	 * @param fieldContributor The field contributor.
	 * @param fieldValueExtractor The field value extractor used when projecting on this field.
	 * @param <F> The type of the value.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	<F> IndexFieldTypeOptionsStep<?, F> asNative(Class<F> valueType,
			LuceneFieldContributor<F> fieldContributor,
			LuceneFieldValueExtractor<F> fieldValueExtractor);

	/**
	 * Define a native field type on which projection is not allowed.
	 * <p>
	 * See {@link #asNative(Class, LuceneFieldContributor, LuceneFieldValueExtractor)}.
	 *
	 * @param valueType The type of the value.
	 * @param fieldContributor The field contributor.
	 * @param <F> The type of the value.
	 * @return A DSL step where the index field type can be defined in more details.
	 */
	default <F> IndexFieldTypeOptionsStep<?, F> asNative(Class<F> valueType,
			LuceneFieldContributor<F> fieldContributor) {
		return asNative( valueType, fieldContributor, null );
	}

}
