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

	OptionalPropertyContext<Long> asLong();

	OptionalPropertyContext<BeanReference> asBeanReference(Class<?> expectedType);

	<T> OptionalPropertyContext<T> as(Class<T> expectedType, Function<String, T> parser);
}
