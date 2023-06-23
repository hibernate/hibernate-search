/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexCompositeNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexObjectFieldTemplate;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataMatchingTypes;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicTemplate;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicType;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.NamedDynamicTemplate;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexCompositeNodeType;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;

class ElasticsearchIndexObjectFieldTemplateBuilder
		extends AbstractElasticsearchIndexFieldTemplateBuilder<
				ElasticsearchIndexObjectFieldTemplateBuilder,
				ElasticsearchIndexObjectFieldTemplate> {

	protected final ElasticsearchIndexCompositeNodeType.Builder typeBuilder;

	ElasticsearchIndexObjectFieldTemplateBuilder(AbstractElasticsearchIndexCompositeNodeBuilder parent,
			String templateName, TreeNodeInclusion inclusion, ObjectStructure structure, String prefix) {
		super( parent, templateName, inclusion, prefix );
		this.typeBuilder = new ElasticsearchIndexCompositeNodeType.Builder( structure );
	}

	@Override
	protected ElasticsearchIndexObjectFieldTemplateBuilder thisAsS() {
		return this;
	}

	@Override
	protected void doContribute(ElasticsearchIndexNodeCollector collector,
			ElasticsearchIndexCompositeNode parentNode, TreeNodeInclusion inclusion,
			SimpleGlobPattern absolutePathGlob, boolean multiValued) {
		ElasticsearchIndexCompositeNodeType type = typeBuilder.build();
		ElasticsearchIndexObjectFieldTemplate fieldTemplate = new ElasticsearchIndexObjectFieldTemplate(
				parentNode, absolutePathGlob, type, inclusion, multiValued );

		PropertyMapping mapping = type.createMapping( DynamicType.TRUE );

		collector.collect( fieldTemplate );

		if ( TreeNodeInclusion.INCLUDED.equals( fieldTemplate.inclusion() ) ) {
			DynamicTemplate dynamicTemplate = new DynamicTemplate();
			dynamicTemplate.setMatchMappingType( DataMatchingTypes.OBJECT );
			dynamicTemplate.setPathMatch( absolutePathGlob.toPatternString() );
			dynamicTemplate.setMapping( mapping );
			NamedDynamicTemplate namedDynamicTemplate = new NamedDynamicTemplate( absolutePath, dynamicTemplate );

			collector.collect( namedDynamicTemplate );
		}
	}

}
