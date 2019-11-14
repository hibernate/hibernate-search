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
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingBuildContext;
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
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
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
	public <I> BoundIdentifierBridge<I> bindIdentifier(
			IndexedEntityBindingContext indexedEntityBindingContext,
			BoundPojoModelPathPropertyNode<?, I> modelPath,
			IdentifierBinder binder) {
		PojoGenericTypeModel<I> identifierTypeModel = modelPath.valueWithoutExtractors().getTypeModel();

		IdentifierBinder defaultedBinder = binder;
		if ( binder == null ) {
			defaultedBinder = bridgeResolver.resolveIdentifierBinderForType( identifierTypeModel );
		}

		IdentifierBindingContextImpl<I> bindingContext = new IdentifierBindingContextImpl<>(
				beanResolver,
				indexedEntityBindingContext,
				identifierTypeModel
		);

		return bindingContext.applyBinder( defaultedBinder );
	}

	@Override
	public <T> BoundRoutingKeyBridge<T> bindRoutingKey(IndexedEntityBindingContext indexedEntityBindingContext,
			BoundPojoModelPathTypeNode<T> modelPath, RoutingKeyBinder binder) {
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

		return bindingContext.applyBinder( binder );
	}

	@Override
	public <T> Optional<BoundTypeBridge<T>> bindType(IndexBindingContext indexBindingContext,
			BoundPojoModelPathTypeNode<T> modelPath, TypeBinder binder) {
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

		return bindingContext.applyBinder( binder );
	}

	@Override
	public <P> Optional<BoundPropertyBridge<P>> bindProperty(IndexBindingContext indexBindingContext,
			BoundPojoModelPathPropertyNode<?, P> modelPath, PropertyBinder binder) {
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

		return bindingContext.applyBinder( binder );
	}

	@Override
	public <V> Optional<BoundValueBridge<V, ?>> bindValue(IndexBindingContext indexBindingContext,
			BoundPojoModelPathValueNode<?, ?, V> modelPath, boolean multiValued,
			ValueBinder binder,
			String relativeFieldName, FieldModelContributor contributor) {
		Integer decimalScale = typeAdditionalMetadataProvider.get( modelPath ).getDecimalScale();
		IndexFieldTypeDefaultsProvider defaultsProvider = new IndexFieldTypeDefaultsProvider( decimalScale );

		PojoGenericTypeModel<V> valueTypeModel = modelPath.getTypeModel();

		ValueBinder defaultedBinder = binder;
		if ( binder == null ) {
			defaultedBinder = bridgeResolver.resolveValueBinderForType( valueTypeModel );
		}

		ValueBindingContextImpl<V> bindingContext = new ValueBindingContextImpl<>(
				beanResolver,
				valueTypeModel, multiValued,
				indexBindingContext, defaultsProvider,
				relativeFieldName, contributor
		);

		return bindingContext.applyBinder( defaultedBinder );
	}
}
