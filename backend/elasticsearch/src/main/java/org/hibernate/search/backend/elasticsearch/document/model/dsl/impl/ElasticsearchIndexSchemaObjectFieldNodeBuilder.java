/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchIndexObjectFieldReference;
import org.hibernate.search.backend.elasticsearch.document.model.impl.AbstractElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeContributor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.AbstractTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicType;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectFieldNodeBuilder;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

class ElasticsearchIndexSchemaObjectFieldNodeBuilder extends AbstractElasticsearchIndexSchemaObjectNodeBuilder
		implements IndexSchemaObjectFieldNodeBuilder, ElasticsearchIndexSchemaNodeContributor {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final AbstractElasticsearchIndexSchemaObjectNodeBuilder parent;
	private final String absoluteFieldPath;
	private final String relativeFieldName;
	private final IndexFieldInclusion inclusion;

	private final ObjectStructure structure;
	private boolean multiValued = false;

	private ElasticsearchIndexObjectFieldReference reference;

	ElasticsearchIndexSchemaObjectFieldNodeBuilder(AbstractElasticsearchIndexSchemaObjectNodeBuilder parent,
			String relativeFieldName, IndexFieldInclusion inclusion, ObjectStructure structure) {
		this.parent = parent;
		String parentAbsolutePath = parent.getAbsolutePath();
		this.absoluteFieldPath = parentAbsolutePath == null ? relativeFieldName
				: FieldPaths.compose( parentAbsolutePath, relativeFieldName );
		this.relativeFieldName = relativeFieldName;
		this.inclusion = inclusion;
		this.structure = structure;
	}

	@Override
	public EventContext eventContext() {
		return getRootNodeBuilder().getIndexEventContext()
				.append( EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
	}

	@Override
	public void multiValued() {
		this.multiValued = true;
	}

	@Override
	public IndexObjectFieldReference toReference() {
		if ( reference != null ) {
			throw log.cannotCreateReferenceMultipleTimes( eventContext() );
		}
		this.reference = new ElasticsearchIndexObjectFieldReference();
		return reference;
	}

	@Override
	public void contribute(ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode,
			Map<String, AbstractElasticsearchIndexSchemaFieldNode> staticChildrenByNameForParent,
			AbstractTypeMapping parentMapping) {
		if ( reference == null ) {
			throw log.incompleteFieldDefinition( eventContext() );
		}

		Map<String, AbstractElasticsearchIndexSchemaFieldNode> staticChildrenByName = new TreeMap<>();
		ElasticsearchIndexSchemaObjectFieldNode fieldNode = new ElasticsearchIndexSchemaObjectFieldNode(
				parentNode, relativeFieldName, inclusion, structure, multiValued, staticChildrenByName
		);

		staticChildrenByNameForParent.put( relativeFieldName, fieldNode );
		collector.collect( absoluteFieldPath, fieldNode );

		reference.setSchemaNode( fieldNode );

		DynamicType dynamicType = resolveSelfDynamicType( parentMapping.getDynamic() );

		PropertyMapping mapping = createPropertyMapping( structure, dynamicType );

		if ( IndexFieldInclusion.INCLUDED.equals( fieldNode.inclusion() ) ) {
			parentMapping.addProperty( relativeFieldName, mapping );
		}

		contributeChildren( mapping, fieldNode, collector, staticChildrenByName );
	}

	@Override
	ElasticsearchIndexSchemaRootNodeBuilder getRootNodeBuilder() {
		return parent.getRootNodeBuilder();
	}

	@Override
	String getAbsolutePath() {
		return absoluteFieldPath;
	}

	static PropertyMapping createPropertyMapping(ObjectStructure structure,
			DynamicType dynamicType) {
		PropertyMapping mapping = new PropertyMapping();
		String dataType = DataTypes.OBJECT;
		switch ( structure ) {
			case DEFAULT:
				break;
			case FLATTENED:
				dataType = DataTypes.OBJECT;
				break;
			case NESTED:
				dataType = DataTypes.NESTED;
				break;
		}
		mapping.setType( dataType );
		mapping.setDynamic( dynamicType );
		return mapping;
	}
}
