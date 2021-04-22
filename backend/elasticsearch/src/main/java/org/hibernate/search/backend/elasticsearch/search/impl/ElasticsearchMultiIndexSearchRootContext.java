/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.util.List;
import java.util.Set;

import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;

public class ElasticsearchMultiIndexSearchRootContext
		extends AbstractElasticsearchMultiIndexSearchCompositeIndexSchemaElementContext {

	public ElasticsearchMultiIndexSearchRootContext(Set<String> indexNames,
			List<ElasticsearchSearchCompositeIndexSchemaElementContext> rootForEachIndex) {
		super( indexNames, rootForEachIndex );
	}

	@Override
	public boolean isObjectField() {
		return true;
	}

	@Override
	public ElasticsearchSearchCompositeIndexSchemaElementContext toObjectField() {
		return this;
	}

	@Override
	public String absolutePath() {
		return null;
	}

	@Override
	protected EventContext relativeEventContext() {
		return EventContexts.indexSchemaRoot();
	}

}
