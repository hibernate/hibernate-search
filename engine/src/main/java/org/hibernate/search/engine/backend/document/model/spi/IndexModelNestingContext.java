/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.spi;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author Yoann Rodiere
 */
public interface IndexModelNestingContext {

	<T> Optional<T> applyIfIncluded(String relativeName, Function<String, T> action);

	<T> Optional<T> applyIfIncluded(String relativeName, BiFunction<String, IndexModelNestingContext, T> action);

	static IndexModelNestingContext excludeAll() {
		return ExcludeAllIndexModelNestingContext.INSTANCE;
	}

	static IndexModelNestingContext includeAll() {
		return IncludeAllIndexModelNestingContext.INSTANCE;
	}

}
