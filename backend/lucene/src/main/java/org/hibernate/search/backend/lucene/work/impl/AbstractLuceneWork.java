/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.impl;

import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;

/**
 * @author Guillaume Smet
 */
public abstract class AbstractLuceneWork<T> implements LuceneIndexWork<T> {

	protected final String workType;

	protected final String indexName;

	public AbstractLuceneWork(String workType, String indexName) {
		this.workType = workType;
		this.indexName = indexName;
	}

	protected final EventContext getEventContext() {
		return EventContexts.fromIndexName( indexName );
	}
}
