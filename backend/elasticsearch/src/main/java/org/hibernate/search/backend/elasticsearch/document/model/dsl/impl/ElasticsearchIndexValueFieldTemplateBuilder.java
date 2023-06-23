/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexCompositeNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexValueFieldTemplate;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicTemplate;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.NamedDynamicTemplate;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexValueFieldType;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;

class ElasticsearchIndexValueFieldTemplateBuilder
		extends AbstractElasticsearchIndexFieldTemplateBuilder<
				ElasticsearchIndexValueFieldTemplateBuilder,
				ElasticsearchIndexValueFieldTemplate> {

	private final ElasticsearchIndexValueFieldType<?> type;

	ElasticsearchIndexValueFieldTemplateBuilder(AbstractElasticsearchIndexCompositeNodeBuilder parent,
			String templateName, TreeNodeInclusion inclusion, ElasticsearchIndexValueFieldType<?> type, String prefix) {
		super( parent, templateName, inclusion, prefix );
		this.type = type;
	}

	@Override
	protected ElasticsearchIndexValueFieldTemplateBuilder thisAsS() {
		return this;
	}

	@Override
	protected void doContribute(ElasticsearchIndexNodeCollector collector,
			ElasticsearchIndexCompositeNode parentNode, TreeNodeInclusion inclusion,
			SimpleGlobPattern absolutePathGlob, boolean multiValued) {
		ElasticsearchIndexValueFieldTemplate fieldTemplate = new ElasticsearchIndexValueFieldTemplate(
				parentNode, absolutePathGlob, inclusion, multiValued, type
		);

		collector.collect( fieldTemplate );

		if ( TreeNodeInclusion.INCLUDED.equals( fieldTemplate.inclusion() ) ) {
			DynamicTemplate dynamicTemplate = new DynamicTemplate();
			dynamicTemplate.setPathMatch( absolutePathGlob.toPatternString() );
			dynamicTemplate.setMapping( type.mapping() );
			NamedDynamicTemplate namedDynamicTemplate = new NamedDynamicTemplate( absolutePath, dynamicTemplate );

			collector.collect( namedDynamicTemplate );
		}
	}

}
