/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

import java.util.function.Function;

import org.hibernate.search.engine.environment.bean.BeanReference;

public interface KeyContext {
	OptionalPropertyContext<String> asString();

	OptionalPropertyContext<Boolean> asBoolean();

	OptionalPropertyContext<Integer> asInteger();

	OptionalPropertyContext<Integer> asIntegerPositiveOrZero();

	OptionalPropertyContext<Integer> asIntegerStrictlyPositive();

	OptionalPropertyContext<Long> asLong();

	<T> OptionalPropertyContext<BeanReference<? extends T>> asBeanReference(Class<T> expectedBeanType);

	<T> OptionalPropertyContext<T> as(Class<T> expectedType, Function<String, T> parser);
}
