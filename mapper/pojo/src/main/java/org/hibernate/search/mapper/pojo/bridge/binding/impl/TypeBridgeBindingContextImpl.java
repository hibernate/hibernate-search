/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryContext;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBridgeBindingContext;
import org.hibernate.search.mapper.pojo.model.PojoModelType;

public class TypeBridgeBindingContextImpl implements TypeBridgeBindingContext {
	private final PojoModelType bridgedElement;
	private final IndexFieldTypeFactoryContext indexFieldTypeFactoryContext;
	private final IndexSchemaElement indexSchemaElement;

	public TypeBridgeBindingContextImpl(PojoModelType bridgedElement,
			IndexFieldTypeFactoryContext indexFieldTypeFactoryContext,
			IndexSchemaElement indexSchemaElement) {
		this.bridgedElement = bridgedElement;
		this.indexSchemaElement = indexSchemaElement;
		this.indexFieldTypeFactoryContext = indexFieldTypeFactoryContext;
	}

	@Override
	public PojoModelType getBridgedElement() {
		return bridgedElement;
	}

	@Override
	public IndexFieldTypeFactoryContext getTypeFactory() {
		return indexFieldTypeFactoryContext;
	}

	@Override
	public IndexSchemaElement getIndexSchemaElement() {
		return indexSchemaElement;
	}
}
