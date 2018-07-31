/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContext;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBridgeBindingContext;
import org.hibernate.search.mapper.pojo.model.PojoModelValue;

public class ValueBridgeBindingContextImpl implements ValueBridgeBindingContext {
	private final PojoModelValue bridgedElement;
	private final IndexSchemaFieldContext indexSchemaFieldContext;

	public ValueBridgeBindingContextImpl(PojoModelValue bridgedElement,
			IndexSchemaFieldContext indexSchemaFieldContext) {
		this.bridgedElement = bridgedElement;
		this.indexSchemaFieldContext = indexSchemaFieldContext;
	}

	@Override
	public PojoModelValue getBridgedElement() {
		return bridgedElement;
	}

	@Override
	public IndexSchemaFieldContext getIndexSchemaFieldContext() {
		return indexSchemaFieldContext;
	}
}
