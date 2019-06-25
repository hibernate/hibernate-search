/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBridgeBindingContext;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;
import org.hibernate.search.mapper.pojo.model.dependency.PojoPropertyIndexingDependencyConfigurationContext;
import org.hibernate.search.mapper.pojo.model.dependency.impl.PojoPropertyIndexingDependencyConfigurationContextImpl;

public class PropertyBridgeBindingContextImpl implements PropertyBridgeBindingContext {
	private final PojoModelProperty bridgedElement;
	private final PojoPropertyIndexingDependencyConfigurationContextImpl<?> pojoDependencyContext;
	private final IndexFieldTypeFactory indexFieldTypeFactory;
	private final IndexSchemaElement indexSchemaElement;

	public PropertyBridgeBindingContextImpl(PojoModelProperty bridgedElement,
			PojoPropertyIndexingDependencyConfigurationContextImpl<?> pojoDependencyContext,
			IndexFieldTypeFactory indexFieldTypeFactory,
			IndexSchemaElement indexSchemaElement) {
		this.bridgedElement = bridgedElement;
		this.pojoDependencyContext = pojoDependencyContext;
		this.indexFieldTypeFactory = indexFieldTypeFactory;
		this.indexSchemaElement = indexSchemaElement;
	}

	@Override
	public PojoModelProperty getBridgedElement() {
		return bridgedElement;
	}

	@Override
	public PojoPropertyIndexingDependencyConfigurationContext getDependencies() {
		return pojoDependencyContext;
	}

	@Override
	public IndexFieldTypeFactory getTypeFactory() {
		return indexFieldTypeFactory;
	}

	@Override
	public IndexSchemaElement getIndexSchemaElement() {
		return indexSchemaElement;
	}
}
