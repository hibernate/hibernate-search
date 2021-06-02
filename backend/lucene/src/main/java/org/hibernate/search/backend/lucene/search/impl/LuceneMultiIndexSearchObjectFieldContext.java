/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

import java.util.List;

import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;

public final class LuceneMultiIndexSearchObjectFieldContext extends
		AbstractLuceneMultiIndexSearchCompositeIndexSchemaElementContext {

	private final String absolutePath;

	public LuceneMultiIndexSearchObjectFieldContext(LuceneSearchIndexScope scope,
			String absolutePath, List<LuceneSearchCompositeIndexSchemaElementContext> fieldForEachIndex) {
		super( scope, fieldForEachIndex );
		this.absolutePath = absolutePath;
	}

	@Override
	public boolean isObjectField() {
		return true;
	}

	@Override
	public LuceneSearchCompositeIndexSchemaElementContext toObjectField() {
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
