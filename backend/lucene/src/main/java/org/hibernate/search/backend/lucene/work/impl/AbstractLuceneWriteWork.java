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
public abstract class AbstractLuceneWriteWork<T> implements LuceneWriteWork<T> {

	protected final String workType;

	protected final String indexName;

	public AbstractLuceneWriteWork(String workType, String indexName) {
		this.workType = workType;
		this.indexName = indexName;
	}

	@Override
	public Object getInfo() {
		// TODO extract immutable work relevant info. We need to think about it. See HSEARCH-3110.
		return this;
	}

	protected final EventContext getEventContext() {
		return EventContexts.fromIndexName( indexName );
	}
}
