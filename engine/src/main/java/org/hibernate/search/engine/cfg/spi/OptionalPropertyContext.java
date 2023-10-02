/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg.spi;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public interface OptionalPropertyContext<T> {

	OptionalPropertyContext<T> substitute(UnaryOperator<Object> substitution);

	OptionalPropertyContext<T> substitute(Object expected, Object replacement);

	OptionalPropertyContext<List<T>> multivalued();

	OptionalPropertyContext<T> validate(Consumer<T> validation);

	DefaultedPropertyContext<T> withDefault(T defaultValue);

	DefaultedPropertyContext<T> withDefault(Supplier<T> defaultValueSupplier);

	OptionalConfigurationProperty<T> build();

}
