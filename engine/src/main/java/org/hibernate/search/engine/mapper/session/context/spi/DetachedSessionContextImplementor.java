/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.session.context.spi;


import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;

/**
 * Provides visibility from the lower layers of Hibernate Search (engine, backend)
 * to the session defined in the upper layers (mapping).
 * <p>
 * On contrary to {@link SessionContextImplementor},
 * this context is expected to be detached from the actual session,
 * allowing it to be used after the session was closed.
 * The main downside is that this context cannot be used
 * everywhere {@link SessionContextImplementor} can.
 * In particular, it cannot be used when creating document-related
 * {@link org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor#createWorkPlan(SessionContextImplementor, DocumentCommitStrategy, DocumentRefreshStrategy) work plans}
 * or {@link org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor#createDocumentWorkExecutor(SessionContextImplementor, DocumentCommitStrategy) work executors}
 * because these may need access to the session.
 */
public final class DetachedSessionContextImplementor {

	public static DetachedSessionContextImplementor of(SessionContextImplementor sessionContext) {
		return new DetachedSessionContextImplementor(
				sessionContext.getMappingContext(),
				sessionContext.getTenantIdentifier()
		);
	}

	private final MappingContextImplementor mappingContext;

	private final String tenantIdentifier;

	private DetachedSessionContextImplementor(
			MappingContextImplementor mappingContext, String tenantIdentifier) {
		this.mappingContext = mappingContext;
		this.tenantIdentifier = tenantIdentifier;
	}

	public MappingContextImplementor getMappingContext() {
		return mappingContext;
	}

	public String getTenantIdentifier() {
		return tenantIdentifier;
	}

}
