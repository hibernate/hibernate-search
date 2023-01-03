/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.session.spi;


import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;

/**
 * Provides visibility from the lower layers of Hibernate Search (engine, backend)
 * to the session defined in the upper layers (mapping).
 * <p>
 * On contrary to {@link BackendSessionContext},
 * this context is expected to be detached from the actual session,
 * allowing it to be used after the session was closed.
 * The main downside is that this context cannot be used
 * everywhere {@link BackendSessionContext} can.
 * In particular, it cannot be used when creating document-related
 * {@link org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor#createIndexingPlan(BackendSessionContext, DocumentCommitStrategy, DocumentRefreshStrategy) indexing plans}
 * or {@link org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor#createIndexer(BackendSessionContext) indexers}
 * because these may need access to the session.
 *
 * @deprecated SPIs should simply pass around a string representing the tenant ID instead of using this class.
 * In cases where the mapping context is also necessary, it should be passed around as a separate parameter.
 */
@Deprecated
public final class DetachedBackendSessionContext {

	public static DetachedBackendSessionContext of(BackendSessionContext sessionContext) {
		return new DetachedBackendSessionContext(
				sessionContext.mappingContext(),
				sessionContext.tenantIdentifier()
		);
	}

	public static DetachedBackendSessionContext of(BackendMappingContext mappingContext, String tenantIdentifier) {
		return new DetachedBackendSessionContext(
				mappingContext,
				tenantIdentifier
		);
	}

	private final BackendMappingContext mappingContext;

	private final String tenantIdentifier;

	private DetachedBackendSessionContext(
			BackendMappingContext mappingContext, String tenantIdentifier) {
		this.mappingContext = mappingContext;
		this.tenantIdentifier = tenantIdentifier;
	}

	public BackendMappingContext mappingContext() {
		return mappingContext;
	}

	public String tenantIdentifier() {
		return tenantIdentifier;
	}

}
