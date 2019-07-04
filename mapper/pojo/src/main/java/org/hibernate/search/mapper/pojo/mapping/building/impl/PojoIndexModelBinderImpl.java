/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.engine.mapper.mapping.spi.MappingBuildContext;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundIdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundPropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundRoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundTypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.IdentifierBindingContextImpl;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.PropertyBindingContextImpl;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.RoutingKeyBindingContextImpl;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.TypeBindingContextImpl;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.ValueBindingContextImpl;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.BridgeResolver;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBridgeBuilder;
import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorBinder;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorHolder;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributor;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl.PojoTypeAdditionalMetadataProvider;
import org.hibernate.search.mapper.pojo.model.dependency.impl.PojoPropertyIndexingDependencyConfigurationContextImpl;
import org.hibernate.search.mapper.pojo.model.dependency.impl.PojoTypeIndexingDependencyConfigurationContextImpl;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelPropertyRootElement;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelTypeRootElement;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;


public class PojoIndexModelBinderImpl implements PojoIndexModelBinder {

	private final BeanResolver beanResolver;
	private final PojoBootstrapIntrospector introspector;
	private final ContainerExtractorBinder extractorBinder;
	private final BridgeResolver bridgeResolver;
	private final PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider;

	PojoIndexModelBinderImpl(MappingBuildContext buildContext,
			PojoBootstrapIntrospector introspector,
			ContainerExtractorBinder extractorBinder, BridgeResolver bridgeResolver,
			PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider) {
		this.beanResolver = buildContext.getBeanResolver();
		this.introspector = introspector;
		this.extractorBinder = extractorBinder;
		this.bridgeResolver = bridgeResolver;
		this.typeAdditionalMetadataProvider = typeAdditionalMetadataProvider;
	}

	@Override
	public <C> BoundContainerExtractorPath<C, ?> bindExtractorPath(
			PojoGenericTypeModel<C> pojoGenericTypeModel, ContainerExtractorPath extractorPath) {
		return extractorBinder.bindPath( pojoGenericTypeModel, extractorPath );
	}

	@Override
	public <C, V> ContainerExtractorHolder<C, V> createExtractors(
			BoundContainerExtractorPath<C, V> boundExtractorPath) {
		return extractorBinder.create( boundExtractorPath );
	}

	@Override
	public <I> BoundIdentifierBridge<I> addIdentifierBridge(
			IndexedEntityBindingContext indexedEntityBindingContext,
			BoundPojoModelPathPropertyNode<?, I> modelPath,
			IdentifierBridgeBuilder builder) {
		PojoGenericTypeModel<I> identifierTypeModel = modelPath.valueWithoutExtractors().getTypeModel();

		IdentifierBridgeBuilder defaultedBuilder = builder;
		if ( builder == null ) {
			defaultedBuilder = bridgeResolver.resolveIdentifierBridgeForType( identifierTypeModel );
		}

		IdentifierBindingContextImpl<I> bindingContext = new IdentifierBindingContextImpl<>(
				beanResolver,
				indexedEntityBindingContext,
				identifierTypeModel
		);

		return bindingContext.applyBuilder( defaultedBuilder );
	}

	@Override
	public <T> BoundRoutingKeyBridge<T> addRoutingKeyBridge(IndexedEntityBindingContext indexedEntityBindingContext,
			BoundPojoModelPathTypeNode<T> modelPath, RoutingKeyBridgeBuilder<?> builder) {
		PojoModelTypeRootElement<T> pojoModelRootElement =
				new PojoModelTypeRootElement<>( modelPath, typeAdditionalMetadataProvider );
		PojoTypeIndexingDependencyConfigurationContextImpl<T> pojoDependencyContext =
				new PojoTypeIndexingDependencyConfigurationContextImpl<>(
						introspector,
						extractorBinder,
						typeAdditionalMetadataProvider,
						modelPath.getTypeModel()
				);
		RoutingKeyBindingContextImpl<T> bindingContext = new RoutingKeyBindingContextImpl<>(
				beanResolver,
				indexedEntityBindingContext,
				pojoModelRootElement,
				pojoDependencyContext
		);

		return bindingContext.applyBuilder( builder );
	}

	@Override
	public <T> Optional<BoundTypeBridge<T>> addTypeBridge(IndexBindingContext indexBindingContext,
			BoundPojoModelPathTypeNode<T> modelPath, TypeBridgeBuilder<?> builder) {
		PojoModelTypeRootElement<T> pojoModelRootElement =
				new PojoModelTypeRootElement<>( modelPath, typeAdditionalMetadataProvider );
		PojoTypeIndexingDependencyConfigurationContextImpl<T> pojoDependencyContext =
				new PojoTypeIndexingDependencyConfigurationContextImpl<>(
						introspector,
						extractorBinder,
						typeAdditionalMetadataProvider,
						modelPath.getTypeModel()
				);
		TypeBindingContextImpl<T> bindingContext = new TypeBindingContextImpl<>(
				beanResolver,
				indexBindingContext,
				pojoModelRootElement,
				pojoDependencyContext
		);

		return bindingContext.applyBuilder( builder );
	}

	@Override
	public <P> Optional<BoundPropertyBridge<P>> addPropertyBridge(IndexBindingContext indexBindingContext,
			BoundPojoModelPathPropertyNode<?, P> modelPath, PropertyBridgeBuilder<?> builder) {
		PojoModelPropertyRootElement<P> pojoModelRootElement =
				new PojoModelPropertyRootElement<>( modelPath, typeAdditionalMetadataProvider );
		PojoPropertyIndexingDependencyConfigurationContextImpl<P> pojoDependencyContext =
				new PojoPropertyIndexingDependencyConfigurationContextImpl<>(
						introspector,
						extractorBinder,
						typeAdditionalMetadataProvider,
						modelPath
				);
		PropertyBindingContextImpl<P> bindingContext = new PropertyBindingContextImpl<>(
				beanResolver,
				indexBindingContext,
				pojoModelRootElement,
				pojoDependencyContext
		);

		return bindingContext.applyBuilder( builder );
	}

	@Override
	public <V> Optional<BoundValueBridge<V, ?>> addValueBridge(IndexBindingContext indexBindingContext,
			BoundPojoModelPathValueNode<?, ?, V> modelPath, boolean multiValued,
			ValueBridgeBuilder builder,
			String relativeFieldName, FieldModelContributor contributor) {
		Integer decimalScale = typeAdditionalMetadataProvider.get( modelPath ).getDecimalScale();
		IndexFieldTypeDefaultsProvider defaultsProvider = new IndexFieldTypeDefaultsProvider( decimalScale );

		PojoGenericTypeModel<V> valueTypeModel = modelPath.getTypeModel();

		ValueBridgeBuilder defaultedBuilder = builder;
		if ( builder == null ) {
			defaultedBuilder = bridgeResolver.resolveValueBridgeForType( valueTypeModel );
		}

		ValueBindingContextImpl<V> bindingContext = new ValueBindingContextImpl<>(
				beanResolver,
				valueTypeModel, multiValued,
				indexBindingContext, defaultsProvider,
				relativeFieldName, contributor
		);

		return bindingContext.applyBuilder( defaultedBuilder );
	}
}
