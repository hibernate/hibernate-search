/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.mapping.building.spi.FieldModelContributor;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.BridgeBuilder;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorHolder;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;

/**
 * Binds a mapping to a given entity model and index model
 * by creating the appropriate {@link ContainerExtractor extractors} and bridges.
 * <p>
 * Also binds the bridges where appropriate:
 * {@link TypeBridge#bind(TypeBridgeBindingContext)},
 * {@link PropertyBridge#bind(PropertyBridgeBindingContext)},
 * {@link ValueBridge#bind(ValueBridgeBindingContext)}.
 * <p>
 * Incidentally, this will also generate the index model,
 * due to bridges contributing to the index model as we bind them.
 *
 */
public interface PojoIndexModelBinder {

	<C> BoundContainerExtractorPath<C, ?> bindExtractorPath(
			PojoGenericTypeModel<C> pojoGenericTypeModel, ContainerExtractorPath extractorPath);

	<C, V> ContainerExtractorHolder<C, V> createExtractors(
			BoundContainerExtractorPath<C, V> boundExtractorPath);

	<I> BeanHolder<? extends IdentifierBridge<I>> addIdentifierBridge(
			IndexedEntityBindingContext bindingContext,
			BoundPojoModelPathPropertyNode<?, I> modelPath, BridgeBuilder<? extends IdentifierBridge<?>> bridgeBuilder);

	<T> BoundRoutingKeyBridge<T> addRoutingKeyBridge(IndexedEntityBindingContext bindingContext,
			BoundPojoModelPathTypeNode<T> modelPath, BridgeBuilder<? extends RoutingKeyBridge> bridgeBuilder);

	<T> Optional<BoundTypeBridge<T>> addTypeBridge(IndexBindingContext bindingContext,
			BoundPojoModelPathTypeNode<T> modelPath, BridgeBuilder<? extends TypeBridge> bridgeBuilder);

	<P> Optional<BoundPropertyBridge<P>> addPropertyBridge(IndexBindingContext bindingContext,
			BoundPojoModelPathPropertyNode<?, P> modelPath, BridgeBuilder<? extends PropertyBridge> bridgeBuilder);

	<V> Optional<BoundValueBridge<V, ?>> addValueBridge(IndexBindingContext bindingContext,
			BoundPojoModelPathValueNode<?, ?, V> modelPath, boolean multiValued,
			BridgeBuilder<? extends ValueBridge<?, ?>> bridgeBuilder,
			String relativeFieldName, FieldModelContributor contributor);

}
