/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.backend.lucene.directory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class OpenResourceTracker {
	private final ConcurrentMap<Object, StackTraceElement[]> openResources = new ConcurrentHashMap<>( 10 );

	public void onOpen(Object resource) {
		openResources.put( resource, new Exception().getStackTrace() );
	}

	public void onClose(Object resource) {
		openResources.remove( resource );
	}

	public Map<Object, StackTraceElement[]> openResources() {
		return openResources;
	}
}
