/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl;


import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A factory for search aggregations.
 *
 * <h2 id="field-paths">Field paths</h2>
 *
 * By default, field paths passed to this DSL are interpreted as absolute,
 * i.e. relative to the index root.
 * <p>
 * However, a new, "relative" factory can be created with {@link #withRoot(String)}:
 * the new factory interprets paths as relative to the object field passed as argument to the method.
 * <p>
 * This can be useful when calling reusable methods that can apply the same aggregation
 * on different object fields that have same structure (same sub-fields).
 * <p>
 * Such a factory can also transform relative paths into absolute paths using {@link #toAbsolutePath(String)};
 * this can be useful for native aggregations in particular.
 *
 * <h2 id="field-references">Field references</h2>
 *
 * A {@link org.hibernate.search.engine.search.reference field reference} is always represented by the absolute field path and,
 * if applicable, i.e. when a field reference is typed, a combination of the {@link org.hibernate.search.engine.search.common.ValueModel} and the type.
 * <p>
 * Field references are usually accessed from the generated Hibernate Search's static metamodel classes that describe the index structure.
 * Such reference provides the information on which search capabilities the particular index field possesses, and allows switching between different
 * {@link org.hibernate.search.engine.search.common.ValueModel value model representations}.
 *
 * @param <SR> Scope root type.
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface TypedSearchAggregationFactory<SR> extends SearchAggregationFactory {

	/**
	 * Perform aggregation in range buckets.
	 * <p>
	 * Given a field and one or more ranges of values,
	 * this aggregation creates one bucket per range,
	 * and puts in each bucket every document for which
	 * the given field has a value that falls into the corresponding range.
	 * <p>
	 * For each bucket, the document count is computed,
	 * or more complex metrics or sub-aggregations for backends that support it.
	 *
	 * @return The next step.
	 */
	@Override
	RangeAggregationFieldStep<SR, ?> range();

	/**
	 * Perform aggregation in term buckets.
	 * <p>
	 * Given a field,
	 * this aggregation creates one bucket per term of that field in the index,
	 * and puts in each bucket every document for which
	 * the given field matches the corresponding term.
	 * <p>
	 * For each bucket, the document count is computed,
	 * or more complex metrics or sub-aggregations for backends that support it.
	 *
	 * @return The next step.
	 */
	@Override
	TermsAggregationFieldStep<SR, ?> terms();

	/**
	 * Perform the sum metric aggregation.
	 * <p>
	 * Sums up the field values.
	 *
	 * @return The next step.
	 */
	@Override
	@Incubating
	SumAggregationFieldStep<SR, ?> sum();

	/**
	 * Perform the min metric aggregation.
	 * <p>
	 * Provides the minimum value among the field values.
	 *
	 * @return The next step.
	 */
	@Override
	@Incubating
	MinAggregationFieldStep<SR, ?> min();

	/**
	 * Perform the max metric aggregation.
	 * <p>
	 * Provides the maximum value among the field values.
	 *
	 * @return The next step.
	 */
	@Override
	@Incubating
	MaxAggregationFieldStep<SR, ?> max();


	/**
	 * Perform the count values metric aggregation.
	 * <p>
	 * Counts the number of non-empty field values.
	 * <p>
	 * As this aggregation counts the field values for a multi-valued fields the resulting count
	 * may be greater as the number of the matched documents.
	 *
	 * @return The next step.
	 * @deprecated Use {@link #countValues()} instead.
	 */
	@SuppressWarnings("removal")
	@Deprecated(since = "8.1", forRemoval = true)
	@Incubating
	@Override
	default CountValuesAggregationFieldStep<SR, ?> count() {
		return countValues();
	}
	/**
	 * Perform the count values metric aggregation.
	 * <p>
	 * Counts the number of non-empty field values.
	 * <p>
	 * As this aggregation counts the field values for a multi-valued fields the resulting count
	 * may be greater as the number of the matched documents.
	 *
	 * @return The next step.
	 */
	@Override
	@Incubating
	CountValuesAggregationFieldStep<SR, ?> countValues();

	/**
	 * Perform the count distinct values metric aggregation.
	 * <p>
	 * Counts the number of unique field values.
	 *
	 * @return The next step.
	 * @deprecated Use {@link #countDistinctValues()} instead.
	 */
	@SuppressWarnings("removal")
	@Deprecated(since = "8.1", forRemoval = true)
	@Incubating
	@Override
	default CountDistinctValuesAggregationFieldStep<SR, ?> countDistinct() {
		return countDistinctValues();
	}

	/**
	 * Perform the count distinct values metric aggregation.
	 * <p>
	 * Counts the number of unique field values.
	 *
	 * @return The next step.
	 */
	@Override
	@Incubating
	CountDistinctValuesAggregationFieldStep<SR, ?> countDistinctValues();

	/**
	 * Perform the avg metric aggregation.
	 * <p>
	 * Calculates the average value of a given numeric or temporal field among the matched documents.
	 *
	 * @return the next step.
	 */
	@Override
	@Incubating
	AvgAggregationFieldStep<SR, ?> avg();

	/**
	 * Create a new aggregation factory whose root for all paths passed to the DSL
	 * will be the given object field.
	 * <p>
	 * See <a href="#field-paths">here</a> for more information.
	 *
	 * @param objectFieldPath The path from the current root to an object field that will become the new root.
	 * @return A new aggregation factory using the given object field as root.
	 */
	@Override
	@Incubating
	TypedSearchAggregationFactory<SR> withRoot(String objectFieldPath);

}
