/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.bridge.spi;

import org.hibernate.search.bridge.spi.FieldMetadataCreationContext;
import org.hibernate.search.bridge.spi.FieldType;
import org.hibernate.search.elasticsearch.bridge.builtin.impl.ElasticsearchBridgeDefinedField;
import org.hibernate.search.elasticsearch.cfg.DynamicType;
import org.hibernate.search.engine.metadata.impl.BridgeDefinedField;

/**
 * Extends {@link FieldMetadataCreationContext} allowing the user to define additional properties for a field specific
 * to Elasticsearch.
 *
 * @hsearch.experimental This contract is currently under active development and may be altered in future releases,
 * @author Davide D'Alto
 */
public class Elasticsearch implements FieldMetadataCreationContext {

	private final FieldMetadataCreationContext creationContext;
	private final ElasticsearchBridgeDefinedField elasticsearchDefinedField;

	public Elasticsearch(BridgeDefinedField definedField, FieldMetadataCreationContext creationContext) {
		this.elasticsearchDefinedField = new ElasticsearchBridgeDefinedField();
		definedField.add( ElasticsearchBridgeDefinedField.class, elasticsearchDefinedField );
		this.creationContext = creationContext;
	}

	@Override
	public FieldMetadataCreationContext field(String name, FieldType type) {
		return creationContext.field( name, type );
	}

	@Override
	public FieldMetadataCreationContext sortable(boolean sortable) {
		return creationContext.sortable( sortable );
	}

	@Override
	public <T extends FieldMetadataCreationContext> T mappedOn(Class<T> backendType) {
		return creationContext.mappedOn( backendType );
	}

	/**
	 * The dynamic type of the field, overrides the global configuration.
	 * <p>
	 * For more details: {@link DynamicType}
	 *
	 * @param dynamicType the dynamic value for the field
	 * @return the context for the fluent API
	 */
	public Elasticsearch dynamic(DynamicType dynamicType) {
		elasticsearchDefinedField.setDynamic( dynamicType );
		return this;
	}
}
