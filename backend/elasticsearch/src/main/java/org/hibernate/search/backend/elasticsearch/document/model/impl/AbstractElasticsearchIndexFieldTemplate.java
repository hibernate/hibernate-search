/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.engine.backend.document.model.spi.AbstractIndexFieldTemplate;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;


public abstract class AbstractElasticsearchIndexFieldTemplate<FT>
		extends AbstractIndexFieldTemplate<
						ElasticsearchIndexModel,
						ElasticsearchIndexField,
						ElasticsearchIndexCompositeNode,
						FT
				> {

	AbstractElasticsearchIndexFieldTemplate(ElasticsearchIndexCompositeNode declaringParent,
			SimpleGlobPattern absolutePathGlob, FT type, TreeNodeInclusion inclusion, boolean multiValued) {
		super( declaringParent, absolutePathGlob, type, inclusion, multiValued );
	}

}
