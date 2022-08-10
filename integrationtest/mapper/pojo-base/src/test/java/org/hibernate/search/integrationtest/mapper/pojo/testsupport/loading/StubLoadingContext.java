/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StubLoadingContext {

	private final Map<PersistenceTypeKey<?, ?>, Map<?, ?>> persistenceMaps = new HashMap<>();

	private final List<LoaderCall> loaderCalls = new ArrayList<>();

	@SuppressWarnings("unchecked")
	public <I, E> Map<I, E> persistenceMap(PersistenceTypeKey<E, I> key) {
		return (Map<I, E>) persistenceMaps.computeIfAbsent( key, ignored -> new LinkedHashMap<>() );
	}

	public List<LoaderCall> loaderCalls() {
		return loaderCalls;
	}

	public static final class LoaderCall {
		final Object strategy;
		public final List<Object> ids;

		public LoaderCall(Object strategy, List<?> ids) {
			this.strategy = strategy;
			this.ids = new ArrayList<>( ids );
		}
	}

}
