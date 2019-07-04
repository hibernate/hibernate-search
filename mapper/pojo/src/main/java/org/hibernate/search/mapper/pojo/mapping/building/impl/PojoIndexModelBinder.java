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
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundPropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundRoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundTypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributor;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorHolder;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;

/**
 * Binds a mapping to a given entity model and index model
 * by creating the appropriate {@link ContainerExtractor extractors} and bridges.
 * <p>
 * Also binds the bridges where appropriate:
 * {@link TypeBinder#bind(TypeBindingContext)},
 * {@link PropertyBinder#bind(PropertyBindingContext)},
 * {@link ValueBinder#bind(ValueBindingContext)}.
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

	<I> BoundIdentifierBridge<I> bindIdentifier(
			IndexedEntityBindingContext bindingContext,
			BoundPojoModelPathPropertyNode<?, I> modelPath, IdentifierBinder binder);

	<T> BoundRoutingKeyBridge<T> bindRoutingKey(IndexedEntityBindingContext bindingContext,
			BoundPojoModelPathTypeNode<T> modelPath, RoutingKeyBinder<?> binder);

	<T> Optional<BoundTypeBridge<T>> bindType(IndexBindingContext bindingContext,
			BoundPojoModelPathTypeNode<T> modelPath, TypeBinder<?> binder);

	<P> Optional<BoundPropertyBridge<P>> bindProperty(IndexBindingContext bindingContext,
			BoundPojoModelPathPropertyNode<?, P> modelPath, PropertyBinder<?> binder);

	<V> Optional<BoundValueBridge<V, ?>> bindValue(IndexBindingContext bindingContext,
			BoundPojoModelPathValueNode<?, ?, V> modelPath, boolean multiValued,
			ValueBinder binder,
			String relativeFieldName, FieldModelContributor contributor);

}
