/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

import java.util.List;
import java.util.function.Supplier;

public interface OptionalPropertyContext<T> {

	OptionalPropertyContext<List<T>> multivalued();

	DefaultedPropertyContext<T> withDefault(T defaultValue);

	DefaultedPropertyContext<T> withDefault(Supplier<T> defaultValueSupplier);

	OptionalConfigurationProperty<T> build();

}
