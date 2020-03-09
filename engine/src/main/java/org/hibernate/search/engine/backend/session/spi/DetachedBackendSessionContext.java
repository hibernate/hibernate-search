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
 * {@link org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor#createIndexingPlan(BackendSessionContext, org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory, DocumentCommitStrategy, DocumentRefreshStrategy) indexing plans}
 * or {@link org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor#createIndexer(BackendSessionContext, DocumentCommitStrategy) indexers}
 * because these may need access to the session.
 */
public final class DetachedBackendSessionContext {

	public static DetachedBackendSessionContext of(BackendSessionContext sessionContext) {
		return new DetachedBackendSessionContext(
				sessionContext.getMappingContext(),
				sessionContext.getTenantIdentifier()
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

	public BackendMappingContext getMappingContext() {
		return mappingContext;
	}

	public String getTenantIdentifier() {
		return tenantIdentifier;
	}

}
