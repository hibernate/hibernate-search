/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public interface OptionalPropertyContext<T> extends PropertyContext<Optional<T>> {
	OptionalPropertyContext<List<T>> multivalued(Pattern separatorPattern);

	PropertyContext<T> withDefault(T defaultValue);

	PropertyContext<T> withDefault(Supplier<T> defaultValueSupplier);

}
