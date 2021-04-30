/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class StubLoadingContext {

	private final Map<PersistenceTypeKey<?, ?>, Map<?, ?>> persistenceMaps = new HashMap<>();

	@SuppressWarnings("unchecked")
	public <I, E> Map<I, E> persistenceMap(PersistenceTypeKey<E, I> key) {
		return (Map<I, E>) persistenceMaps.computeIfAbsent( key, ignored -> new LinkedHashMap<>() );
	}

}
