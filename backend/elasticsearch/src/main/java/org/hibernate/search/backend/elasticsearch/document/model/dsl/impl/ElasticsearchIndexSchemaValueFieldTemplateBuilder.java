/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaValueFieldTemplate;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicTemplate;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.NamedDynamicTemplate;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexValueFieldType;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;

class ElasticsearchIndexSchemaValueFieldTemplateBuilder
		extends AbstractElasticsearchIndexSchemaFieldTemplateBuilder<
				ElasticsearchIndexSchemaValueFieldTemplateBuilder, ElasticsearchIndexSchemaValueFieldTemplate
		> {

	private final ElasticsearchIndexValueFieldType<?> type;

	ElasticsearchIndexSchemaValueFieldTemplateBuilder(AbstractElasticsearchIndexSchemaObjectNodeBuilder parent,
			String templateName, IndexFieldInclusion inclusion, ElasticsearchIndexValueFieldType<?> type, String prefix) {
		super( parent, templateName, inclusion, prefix );
		this.type = type;
	}

	@Override
	protected ElasticsearchIndexSchemaValueFieldTemplateBuilder thisAsS() {
		return this;
	}

	@Override
	protected void doContribute(ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode, IndexFieldInclusion inclusion,
			SimpleGlobPattern absolutePathGlob, boolean multiValued) {
		ElasticsearchIndexSchemaValueFieldTemplate fieldTemplate = new ElasticsearchIndexSchemaValueFieldTemplate(
				parentNode, absolutePathGlob, inclusion, multiValued, type
		);

		collector.collect( fieldTemplate );

		if ( IndexFieldInclusion.INCLUDED.equals( fieldTemplate.getInclusion() ) ) {
			DynamicTemplate dynamicTemplate = new DynamicTemplate();
			dynamicTemplate.setPathMatch( absolutePathGlob.toPatternString() );
			dynamicTemplate.setMapping( type.mapping() );
			NamedDynamicTemplate namedDynamicTemplate = new NamedDynamicTemplate( absolutePath, dynamicTemplate );

			collector.collect( namedDynamicTemplate );
		}
	}

}
