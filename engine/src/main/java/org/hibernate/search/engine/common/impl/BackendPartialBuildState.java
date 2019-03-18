/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.reporting.impl.RootFailureCollector;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.reporting.spi.EventContexts;

class BackendPartialBuildState {

	private final String backendName;
	private final BackendImplementor<?> partiallyBuiltBackend;

	BackendPartialBuildState(String backendName, BackendImplementor<?> partiallyBuiltBackend) {
		this.backendName = backendName;
		this.partiallyBuiltBackend = partiallyBuiltBackend;
	}

	void closeOnFailure() {
		partiallyBuiltBackend.close();
	}

	BackendImplementor<?> finalizeBuild(RootFailureCollector rootFailureCollector) {
		ContextualFailureCollector backendFailureCollector =
				rootFailureCollector.withContext( EventContexts.fromBackendName( backendName ) );
		BackendStartContextImpl startContext = new BackendStartContextImpl( backendFailureCollector );
		try {
			partiallyBuiltBackend.start( startContext );
		}
		catch (RuntimeException e) {
			backendFailureCollector.add( e );
		}
		return partiallyBuiltBackend; // The backend is now fully built
	}
}
