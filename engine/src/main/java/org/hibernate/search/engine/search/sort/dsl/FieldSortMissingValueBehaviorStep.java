/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.dsl;

import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.util.common.SearchException;

/**
 * The step in a sort definition where the behavior on missing values can be set.
 *
 * @param <N> The type of the next step (returned by {@link FieldSortMissingValueBehaviorStep#first()}, for example).
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface FieldSortMissingValueBehaviorStep<N> extends FieldSortMissingValueBehaviorGenericStep<Object, N> {

	/**
	 * When documents are missing a value on the sort field, use the given value instead.
	 * <p>
	 * This method will apply DSL converters to {@code value} before Hibernate Search attempts to interpret it as a field value.
	 * See {@link ValueModel#MAPPING}.
	 *
	 * @param value The value to use as a default when a document is missing a value on the sort field.
	 * @return The next step.
	 * @throws SearchException If the field is not numeric.
	 */
	default N use(Object value) {
		return use( value, ValueModel.MAPPING );
	}

	/**
	 * When documents are missing a value on the sort field, use the given value instead.
	 *
	 * @param value The value to use as a default when a document is missing a value on the sort field.
	 * @param convert Controls how the {@code value} should be converted before Hibernate Search attempts to interpret it as a field value.
	 * See {@link org.hibernate.search.engine.search.common.ValueConvert}.
	 * @return The next step.
	 * @throws SearchException If the field is not numeric.
	 * @deprecated Use {@link #use(Object, ValueModel)} instead.
	 */
	@Deprecated(since = "7.2")
	default N use(Object value, org.hibernate.search.engine.search.common.ValueConvert convert) {
		return use( value, org.hibernate.search.engine.search.common.ValueConvert.toValueModel( convert ) );
	}

	/**
	 * When documents are missing a value on the sort field, use the given value instead.
	 *
	 * @param value The value to use as a default when a document is missing a value on the sort field.
	 * @param valueModel The model value, determines how the {@code value} should be converted before Hibernate Search attempts to interpret it as a field value.
	 * See {@link ValueModel}.
	 * @return The next step.
	 *
	 * @throws SearchException If the field is not numeric.
	 */
	N use(Object value, ValueModel valueModel);

}
