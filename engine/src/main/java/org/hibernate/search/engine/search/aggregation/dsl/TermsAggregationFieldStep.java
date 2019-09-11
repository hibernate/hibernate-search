/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.dsl;

import java.util.Map;

import org.hibernate.search.engine.search.common.ValueConvert;

/**
 * The initial step in a "terms" aggregation definition, where the target field can be set.
 */
public interface TermsAggregationFieldStep {

	/**
	 * Target the given field in the terms aggregation.
	 *
	 * @param absoluteFieldPath The absolute path of the field.
	 * @param type The type of field values.
	 * @param <F> The type of field values.
	 * @return The next step.
	 */
	default <F> TermsAggregationOptionsStep<F, Map<F, Long>> field(String absoluteFieldPath, Class<F> type) {
		return field( absoluteFieldPath, type, ValueConvert.YES );
	}

	/**
	 * Target the given field in the terms aggregation.
	 *
	 * @param absoluteFieldPath The absolute path of the field.
	 * @param type The type of field values.
	 * @param <F> The type of field values.
	 * @param convert Controls how the ranges passed to the next steps and fetched from the backend should be converted.
	 * See {@link ValueConvert}.
	 * @return The next step.
	 */
	<F> TermsAggregationOptionsStep<F, Map<F, Long>> field(String absoluteFieldPath, Class<F> type,
			ValueConvert convert);

}
