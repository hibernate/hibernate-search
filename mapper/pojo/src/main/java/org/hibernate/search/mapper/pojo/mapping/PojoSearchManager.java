/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping;

import java.util.Collection;
import java.util.Collections;

import org.hibernate.search.engine.common.SearchManager;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.engine.search.dsl.SearchResultDefinitionContext;


/**
 * @author Yoann Rodiere
 */
public interface PojoSearchManager extends SearchManager {

	default SearchResultDefinitionContext<PojoReference> search() {
		return search( Collections.singleton( Object.class ) );
	}

	default SearchResultDefinitionContext<PojoReference> search(Class<?> targetedType) {
		return search( Collections.singleton( targetedType ) );
	}

	SearchResultDefinitionContext<PojoReference> search(Collection<? extends Class<?>> targetedTypes);

	/**
	 * @return The worker for this manager. Calling {@link ChangesetPojoWorker#execute()}
	 * is optional, as it will be executed upon closing this manager.
	 */
	ChangesetPojoWorker getWorker();

	/**
	 * @return A stream worker for this manager.
	 */
	StreamPojoWorker getStreamWorker();

}
