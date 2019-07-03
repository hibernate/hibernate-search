/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import java.util.Optional;

import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundPropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundTypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributor;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
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
 * {@link TypeBridgeBuilder#bind(TypeBindingContext)},
 * {@link PropertyBridgeBuilder#bind(PropertyBindingContext)},
 * {@link ValueBridgeBuilder#bind(ValueBindingContext)}.
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
			BoundPojoModelPathPropertyNode<?, I> modelPath, IdentifierBridgeBuilder bridgeBuilder);

	<T> BoundRoutingKeyBridge<T> addRoutingKeyBridge(IndexedEntityBindingContext bindingContext,
			BoundPojoModelPathTypeNode<T> modelPath, RoutingKeyBridgeBuilder<?> bridgeBuilder);

	<T> Optional<BoundTypeBridge<T>> addTypeBridge(IndexBindingContext bindingContext,
			BoundPojoModelPathTypeNode<T> modelPath, TypeBridgeBuilder<?> bridgeBuilder);

	<P> Optional<BoundPropertyBridge<P>> addPropertyBridge(IndexBindingContext bindingContext,
			BoundPojoModelPathPropertyNode<?, P> modelPath, PropertyBridgeBuilder<?> bridgeBuilder);

	<V> Optional<BoundValueBridge<V, ?>> addValueBridge(IndexBindingContext bindingContext,
			BoundPojoModelPathValueNode<?, ?, V> modelPath, boolean multiValued,
			ValueBridgeBuilder bridgeBuilder,
			String relativeFieldName, FieldModelContributor contributor);

}
