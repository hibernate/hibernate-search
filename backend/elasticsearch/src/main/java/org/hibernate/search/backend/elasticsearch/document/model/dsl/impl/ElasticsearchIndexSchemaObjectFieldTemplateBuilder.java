/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectFieldTemplate;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataMatchingTypes;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicTemplate;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicType;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.NamedDynamicTemplate;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;

class ElasticsearchIndexSchemaObjectFieldTemplateBuilder
		extends AbstractElasticsearchIndexSchemaFieldTemplateBuilder<
				ElasticsearchIndexSchemaObjectFieldTemplateBuilder, ElasticsearchIndexSchemaObjectFieldTemplate
		> {

	private final ObjectFieldStorage storage;

	ElasticsearchIndexSchemaObjectFieldTemplateBuilder(AbstractElasticsearchIndexSchemaObjectNodeBuilder parent,
			String templateName, IndexFieldInclusion inclusion, ObjectFieldStorage storage, String prefix) {
		super( parent, templateName, inclusion, prefix );
		this.storage = storage;
	}

	@Override
	protected ElasticsearchIndexSchemaObjectFieldTemplateBuilder thisAsS() {
		return this;
	}

	@Override
	protected void doContribute(ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode, IndexFieldInclusion inclusion,
			SimpleGlobPattern absolutePathGlob, boolean multiValued) {
		ElasticsearchIndexSchemaObjectFieldTemplate fieldTemplate = new ElasticsearchIndexSchemaObjectFieldTemplate(
				parentNode, absolutePathGlob, inclusion, multiValued, storage
		);

		PropertyMapping mapping =
				ElasticsearchIndexSchemaObjectFieldNodeBuilder.createPropertyMapping( storage, DynamicType.TRUE );

		collector.collect( fieldTemplate );

		if ( IndexFieldInclusion.INCLUDED.equals( fieldTemplate.getInclusion() ) ) {
			DynamicTemplate dynamicTemplate = new DynamicTemplate();
			dynamicTemplate.setMatchMappingType( DataMatchingTypes.OBJECT );
			dynamicTemplate.setPathMatch( absolutePathGlob.toPatternString() );
			dynamicTemplate.setMapping( mapping );
			NamedDynamicTemplate namedDynamicTemplate = new NamedDynamicTemplate( absolutePath, dynamicTemplate );

			collector.collect( namedDynamicTemplate );
		}
	}

}
