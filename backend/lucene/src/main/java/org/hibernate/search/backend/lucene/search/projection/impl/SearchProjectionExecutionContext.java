/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;

public class SearchProjectionExecutionContext {

	private final SessionContextImplementor sessionContext;

	public SearchProjectionExecutionContext(SessionContextImplementor sessionContext) {
		this.sessionContext = sessionContext;
	}

	SessionContextImplementor getSessionContext() {
		return sessionContext;
	}
}
