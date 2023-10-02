/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg.spi;

import java.util.function.Function;

import org.hibernate.search.engine.environment.bean.BeanReference;

public interface KeyContext {
	OptionalPropertyContext<String> asString();

	OptionalPropertyContext<Boolean> asBoolean();

	/**
	 * @return The next context.
	 * @deprecated Use {@link #asIntegerPositiveOrZeroOrNegative()} instead.
	 */
	@Deprecated
	default OptionalPropertyContext<Integer> asInteger() {
		return asIntegerPositiveOrZeroOrNegative();
	}

	OptionalPropertyContext<Integer> asIntegerPositiveOrZeroOrNegative();

	OptionalPropertyContext<Integer> asIntegerPositiveOrZero();

	OptionalPropertyContext<Integer> asIntegerStrictlyPositive();

	/**
	 * @return The next context.
	 * @deprecated Use {@link #asLongPositiveOrZeroOrNegative()} instead.
	 */
	@Deprecated
	default OptionalPropertyContext<Long> asLong() {
		return asLongPositiveOrZeroOrNegative();
	}

	OptionalPropertyContext<Long> asLongPositiveOrZeroOrNegative();

	OptionalPropertyContext<Long> asLongStrictlyPositive();

	<T> OptionalPropertyContext<BeanReference<? extends T>> asBeanReference(Class<T> expectedBeanType);

	<T> OptionalPropertyContext<T> as(Class<T> expectedType, Function<String, T> parser);
}
