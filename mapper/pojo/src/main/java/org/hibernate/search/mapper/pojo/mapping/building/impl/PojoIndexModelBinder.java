/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import org.hibernate.search.engine.backend.document.model.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.IndexSchemaFieldContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.FieldModelContributor;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.engine.mapper.model.SearchModel;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.FunctionBridge;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.model.PojoModelElement;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;
import org.hibernate.search.mapper.pojo.model.PojoModelType;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.processing.impl.FunctionBridgeProcessor;

/**
 * Provides the ability to contribute the entity model to the index model
 * by creating bridges and binding them
 * ({@link TypeBridge#bind(IndexSchemaElement, PojoModelType, SearchModel)},
 * {@link PropertyBridge#bind(IndexSchemaElement, PojoModelProperty, SearchModel)},
 * {@link FunctionBridge#bind(IndexSchemaFieldContext)}).
 * <p>
 * Incidentally, this will also generate the index model,
 * due to bridges contributing to the index model as we bind them.
 *
 * @author Yoann Rodiere
 */
public interface PojoIndexModelBinder {

	<T> IdentifierBridge<T> createIdentifierBridge(PojoModelElement pojoModelElement, PojoTypeModel<T> typeModel,
			BridgeBuilder<? extends IdentifierBridge<?>> bridgeBuilder);

	RoutingKeyBridge addRoutingKeyBridge(IndexModelBindingContext bindingContext,
			PojoModelElement pojoModelElement, BridgeBuilder<? extends RoutingKeyBridge> bridgeBuilder);

	TypeBridge addTypeBridge(IndexModelBindingContext bindingContext,
			PojoModelType pojoModelType, BridgeBuilder<? extends TypeBridge> bridgeBuilder);

	PropertyBridge addPropertyBridge(IndexModelBindingContext bindingContext,
			PojoModelProperty pojoModelProperty, BridgeBuilder<? extends PropertyBridge> bridgeBuilder);

	<T> FunctionBridgeProcessor<T, ?> addFunctionBridge(IndexModelBindingContext bindingContext,
			PojoTypeModel<T> typeModel, BridgeBuilder<? extends FunctionBridge<?, ?>> bridgeBuilder,
			String fieldName, FieldModelContributor contributor);

}
