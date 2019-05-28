/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchIndexObjectFieldReference;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeContributor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.AbstractTypeMapping;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DynamicType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.util.impl.ElasticsearchFields;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectFieldNodeBuilder;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

class ElasticsearchIndexSchemaObjectFieldNodeBuilder extends AbstractElasticsearchIndexSchemaObjectNodeBuilder
		implements IndexSchemaObjectFieldNodeBuilder, ElasticsearchIndexSchemaNodeContributor {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final AbstractElasticsearchIndexSchemaObjectNodeBuilder parent;
	private final String absoluteFieldPath;
	private final String relativeFieldName;
	private final ObjectFieldStorage storage;
	private boolean multiValued = false;

	private ElasticsearchIndexObjectFieldReference reference;

	ElasticsearchIndexSchemaObjectFieldNodeBuilder(AbstractElasticsearchIndexSchemaObjectNodeBuilder parent,
			String relativeFieldName, ObjectFieldStorage storage) {
		this.parent = parent;
		String parentAbsolutePath = parent.getAbsolutePath();
		this.absoluteFieldPath = parentAbsolutePath == null ? relativeFieldName
				: ElasticsearchFields.compose( parentAbsolutePath, relativeFieldName );
		this.relativeFieldName = relativeFieldName;
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
		JsonObjectAccessor jsonAccessor = JsonAccessor.root().property( relativeFieldName ).asObject();
		this.reference = new ElasticsearchIndexObjectFieldReference( jsonAccessor );
		return reference;
	}

	@Override
	public void contribute(ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode, AbstractTypeMapping parentMapping) {
		if ( reference == null ) {
			throw log.incompleteFieldDefinition( getEventContext() );
		}

		ElasticsearchIndexSchemaObjectNode fieldNode =
				new ElasticsearchIndexSchemaObjectNode( parentNode, absoluteFieldPath, storage, multiValued );
		collector.collect( absoluteFieldPath, fieldNode );

		reference.enable( fieldNode );

		PropertyMapping mapping = new PropertyMapping();
		DataType dataType = DataType.OBJECT;
		switch ( storage ) {
			case DEFAULT:
				break;
			case FLATTENED:
				dataType = DataType.OBJECT;
				break;
			case NESTED:
				dataType = DataType.NESTED;
				break;
		}
		mapping.setType( dataType );

		// TODO HSEARCH-3273 allow to configure this, both at index level (configuration properties) and at field level (ElasticsearchExtension)
		mapping.setDynamic( DynamicType.STRICT );

		parentMapping.addProperty( relativeFieldName, mapping );

		contributeChildren( mapping, fieldNode, collector );
	}

	@Override
	ElasticsearchIndexSchemaRootNodeBuilder getRootNodeBuilder() {
		return parent.getRootNodeBuilder();
	}

	@Override
	String getAbsolutePath() {
		return absoluteFieldPath;
	}
}
