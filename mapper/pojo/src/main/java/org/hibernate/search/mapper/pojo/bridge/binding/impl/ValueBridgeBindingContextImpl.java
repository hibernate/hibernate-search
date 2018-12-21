/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryContext;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBridgeBindingContext;
import org.hibernate.search.mapper.pojo.model.PojoModelValue;

public class ValueBridgeBindingContextImpl<T> implements ValueBridgeBindingContext<T> {
	private final PojoModelValue<T> bridgedElement;
	private final IndexFieldTypeFactoryContext indexFieldTypeFactoryContext;

	public ValueBridgeBindingContextImpl(PojoModelValue<T> bridgedElement,
			IndexFieldTypeFactoryContext indexFieldTypeFactoryContext) {
		this.bridgedElement = bridgedElement;
		this.indexFieldTypeFactoryContext = indexFieldTypeFactoryContext;
	}

	@Override
	public PojoModelValue<T> getBridgedElement() {
		return bridgedElement;
	}

	@Override
	public IndexFieldTypeFactoryContext getTypeFactory() {
		return indexFieldTypeFactoryContext;
	}

}
