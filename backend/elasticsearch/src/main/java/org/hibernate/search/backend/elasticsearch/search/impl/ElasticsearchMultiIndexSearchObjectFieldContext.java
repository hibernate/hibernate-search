/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.util.List;
import java.util.Set;

import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;

public class ElasticsearchMultiIndexSearchObjectFieldContext
		extends AbstractElasticsearchMultiIndexSearchCompositeIndexSchemaElementContext {

	private final String absolutePath;

	public ElasticsearchMultiIndexSearchObjectFieldContext(Set<String> indexNames, String absolutePath,
			List<ElasticsearchSearchCompositeIndexSchemaElementContext> fieldForEachIndex) {
		super( indexNames, fieldForEachIndex );
		this.absolutePath = absolutePath;
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
		return absolutePath;
	}

	@Override
	public String absolutePath(String relativeFieldName) {
		return FieldPaths.compose( absolutePath, relativeFieldName );
	}

	@Override
	protected EventContext relativeEventContext() {
		return EventContexts.fromIndexFieldAbsolutePath( absolutePath );
	}

}
