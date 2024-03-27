/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StubLoadingContext {

	private final Map<PersistenceTypeKey<?, ?>, Map<?, ?>> persistenceMaps = new ConcurrentHashMap<>();

	private final List<LoaderCall> loaderCalls = new ArrayList<>();

	@SuppressWarnings("unchecked")
	public <I, E> Map<I, E> persistenceMap(PersistenceTypeKey<E, I> key) {
		return (Map<I, E>) persistenceMaps.computeIfAbsent( key, ignored -> new ConcurrentHashMap<>() );
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
