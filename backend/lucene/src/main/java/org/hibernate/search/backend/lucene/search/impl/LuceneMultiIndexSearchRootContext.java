/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

public final class LuceneMultiIndexSearchRootContext extends
		AbstractLuceneMultiIndexSearchCompositeIndexSchemaElementContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public LuceneMultiIndexSearchRootContext(LuceneSearchIndexScope scope,
			List<LuceneSearchCompositeIndexSchemaElementContext> rootForEachIndex) {
		super( scope, rootForEachIndex );
	}

	@Override
	public boolean isObjectField() {
		return false;
	}

	@Override
	public LuceneSearchCompositeIndexSchemaElementContext toObjectField() {
		throw log.invalidIndexElementTypeRootIsNotObjectField();
	}

	@Override
	public String absolutePath() {
		return null;
	}

	@Override
	public String absolutePath(String relativeFieldName) {
		return relativeFieldName;
	}

	@Override
	protected EventContext relativeEventContext() {
		return EventContexts.indexSchemaRoot();
	}

}
