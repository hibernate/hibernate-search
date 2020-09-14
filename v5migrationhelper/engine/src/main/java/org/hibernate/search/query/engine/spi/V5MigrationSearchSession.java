/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.spi;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.scope.spi.V5MigrationSearchScope;

/**
 * @param <LOS> The type of the initial step of the loading options definition DSL accessible through {@link SearchQueryOptionsStep#loading(Consumer)}.
 * @deprecated This class will be removed without replacement. Use actual API instead.
 */
@Deprecated
public interface V5MigrationSearchSession<LOS> {

	SearchQuerySelectStep<?, ?, ?, LOS, ?, ?> search(V5MigrationSearchScope scope);

}
