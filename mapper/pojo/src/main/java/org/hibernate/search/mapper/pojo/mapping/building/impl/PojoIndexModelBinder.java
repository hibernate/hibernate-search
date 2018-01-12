/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaElement;
import org.hibernate.search.engine.mapper.mapping.building.spi.FieldModelContributor;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.spi.Bridge;
import org.hibernate.search.mapper.pojo.bridge.spi.FunctionBridge;
import org.hibernate.search.mapper.pojo.bridge.spi.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.spi.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.model.spi.PojoModelElement;
import org.hibernate.search.mapper.pojo.processing.impl.ValueProcessor;

/**
 * Provides the ability to contribute the entity model to the index model
 * by creating bridges and
 * {@link org.hibernate.search.mapper.pojo.bridge.spi.Bridge#bind(IndexSchemaElement, PojoModelElement) binding}
 * them.
 * <p>
 * Incidentally, this will also generate the index model,
 * due to bridges contributing to the index model as we contribute them.
 *
 * @author Yoann Rodiere
 */
public interface PojoIndexModelBinder {

	<T> IdentifierBridge<T> createIdentifierBridge(Class<T> sourceType,
			BridgeBuilder<? extends IdentifierBridge<?>> bridgeBuilder);

	RoutingKeyBridge addRoutingKeyBridge(IndexModelBindingContext bindingContext,
			PojoModelElement pojoModelElement, BridgeBuilder<? extends RoutingKeyBridge> bridgeBuilder);

	ValueProcessor addBridge(IndexModelBindingContext bindingContext,
			PojoModelElement pojoModelElement, BridgeBuilder<? extends Bridge> bridgeBuilder);

	ValueProcessor addFunctionBridge(IndexModelBindingContext bindingContext,
			PojoModelElement pojoModelElement, Class<?> sourceType,
			BridgeBuilder<? extends FunctionBridge<?, ?>> bridgeBuilder,
			String fieldName, FieldModelContributor contributor);

}
