/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.common.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class ElasticsearchMultiIndexSearchIndexCompositeNodeContext
		extends AbstractElasticsearchMultiIndexSearchIndexNodeContext<ElasticsearchSearchIndexCompositeNodeContext>
		implements ElasticsearchSearchIndexCompositeNodeContext {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public ElasticsearchMultiIndexSearchIndexCompositeNodeContext(ElasticsearchSearchIndexScope scope,
			String absolutePath,
			List<ElasticsearchSearchIndexCompositeNodeContext> elementForEachIndex) {
		super( scope, absolutePath, elementForEachIndex );
	}

	@Override
	protected ElasticsearchSearchIndexCompositeNodeContext self() {
		return this;
	}

	@Override
	public boolean isComposite() {
		return true;
	}

	@Override
	public ElasticsearchSearchIndexCompositeNodeContext toComposite() {
		return this;
	}

	@Override
	public String absolutePath(String relativeFieldName) {
		return FieldPaths.compose( absolutePath, relativeFieldName );
	}

	@Override
	public boolean nested() {
		return getFromElementIfCompatible( ElasticsearchSearchIndexCompositeNodeContext::nested, Object::equals, "nested" );
	}

	@Override
	protected String missingSupportHint(String queryElementName) {
		return log.missingSupportHintForCompositeIndexElement();
	}

	@Override
	protected String partialSupportHint() {
		return log.partialSupportHintForCompositeIndexElement();
	}

	@Override
	protected <T> ElasticsearchSearchQueryElementFactory<T, ElasticsearchSearchIndexCompositeNodeContext> queryElementFactory(
			ElasticsearchSearchIndexCompositeNodeContext indexElement, SearchQueryElementTypeKey<T> key) {
		return indexElement.queryElementFactory( key );
	}

}
