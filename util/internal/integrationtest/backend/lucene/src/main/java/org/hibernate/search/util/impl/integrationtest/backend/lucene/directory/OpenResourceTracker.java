/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
