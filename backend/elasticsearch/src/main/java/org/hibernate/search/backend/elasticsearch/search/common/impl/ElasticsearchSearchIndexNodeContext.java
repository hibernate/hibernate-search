/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.common.impl;

import org.hibernate.search.engine.search.common.spi.SearchIndexNodeContext;

public interface ElasticsearchSearchIndexNodeContext
		extends SearchIndexNodeContext<ElasticsearchSearchIndexScope<?>> {

	@Override
	ElasticsearchSearchIndexCompositeNodeContext toComposite();

	@Override
	ElasticsearchSearchIndexCompositeNodeContext toObjectField();

	@Override
	ElasticsearchSearchIndexValueFieldContext<?> toValueField();

}
