/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

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
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
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

	private final ObjectFieldStorage storage;
	private boolean multiValued = false;

	private ElasticsearchIndexObjectFieldReference reference;

	ElasticsearchIndexSchemaObjectFieldNodeBuilder(AbstractElasticsearchIndexSchemaObjectNodeBuilder parent,
			String relativeFieldName, IndexFieldInclusion inclusion, ObjectFieldStorage storage) {
		this.parent = parent;
		String parentAbsolutePath = parent.getAbsolutePath();
		this.absoluteFieldPath = parentAbsolutePath == null ? relativeFieldName
				: FieldPaths.compose( parentAbsolutePath, relativeFieldName );
		this.relativeFieldName = relativeFieldName;
		this.inclusion = inclusion;
		this.storage = storage;
	}

	@Override
	public EventContext getEventContext() {
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
			throw log.cannotCreateReferenceMultipleTimes( getEventContext() );
		}
		this.reference = new ElasticsearchIndexObjectFieldReference();
		return reference;
	}

	@Override
	public void contribute(ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode,
			List<AbstractElasticsearchIndexSchemaFieldNode> staticChildrenForParent,
			AbstractTypeMapping parentMapping) {
		if ( reference == null ) {
			throw log.incompleteFieldDefinition( getEventContext() );
		}

		List<AbstractElasticsearchIndexSchemaFieldNode> staticChildren = new ArrayList<>();
		ElasticsearchIndexSchemaObjectFieldNode fieldNode = new ElasticsearchIndexSchemaObjectFieldNode(
				parentNode, relativeFieldName, inclusion, storage, multiValued, staticChildren
		);

		staticChildrenForParent.add( fieldNode );
		collector.collect( absoluteFieldPath, fieldNode );

		reference.setSchemaNode( fieldNode );

		DynamicType dynamicType = parentMapping.getDynamic();
		if ( DynamicType.STRICT.equals( dynamicType ) ) {
			dynamicType = resolveSelfDynamicType();
		}

		PropertyMapping mapping = createPropertyMapping( storage, dynamicType );

		if ( IndexFieldInclusion.INCLUDED.equals( fieldNode.getInclusion() ) ) {
			parentMapping.addProperty( relativeFieldName, mapping );
		}

		contributeChildren( mapping, fieldNode, collector, staticChildren );
	}

	@Override
	ElasticsearchIndexSchemaRootNodeBuilder getRootNodeBuilder() {
		return parent.getRootNodeBuilder();
	}

	@Override
	String getAbsolutePath() {
		return absoluteFieldPath;
	}

	static PropertyMapping createPropertyMapping(ObjectFieldStorage storage,
			DynamicType dynamicType) {
		PropertyMapping mapping = new PropertyMapping();
		String dataType = DataTypes.OBJECT;
		switch ( storage ) {
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
