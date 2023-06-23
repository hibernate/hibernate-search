/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexCompositeNodeContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexCompositeNodeType;
import org.hibernate.search.engine.backend.document.model.spi.IndexCompositeNode;

public interface ElasticsearchIndexCompositeNode
		extends IndexCompositeNode<
				ElasticsearchSearchIndexScope<?>,
				ElasticsearchIndexCompositeNodeType,
				ElasticsearchIndexField>,
		ElasticsearchIndexNode, ElasticsearchSearchIndexCompositeNodeContext {

}
