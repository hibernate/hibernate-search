/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexSchemaContributionListener;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.engine.mapper.mapping.spi.MappingBuildContext;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.BoundValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.IdentifierBridgeBindingContextImpl;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.PropertyBridgeBindingContextImpl;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.RoutingKeyBridgeBindingContextImpl;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.TypeBridgeBindingContextImpl;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.ValueBindingContextImpl;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.BridgeResolver;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.BridgeBuildContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBridgeBuilder;
import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorBinder;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerExtractorHolder;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributor;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl.PojoTypeAdditionalMetadataProvider;
import org.hibernate.search.mapper.pojo.model.dependency.impl.AbstractPojoBridgedElementDependencyContext;
import org.hibernate.search.mapper.pojo.model.dependency.impl.PojoPropertyIndexingDependencyConfigurationContextImpl;
import org.hibernate.search.mapper.pojo.model.dependency.impl.PojoTypeIndexingDependencyConfigurationContextImpl;
import org.hibernate.search.mapper.pojo.model.impl.AbstractPojoModelCompositeElement;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelPropertyRootElement;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelTypeRootElement;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelValueElement;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.impl.GenericTypeContext;
import org.hibernate.search.util.common.reflect.impl.ReflectionUtils;


public class PojoIndexModelBinderImpl implements PojoIndexModelBinder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BeanResolver beanResolver;
	private final BridgeBuildContext bridgeBuildContext;
	private final PojoBootstrapIntrospector introspector;
	private final ContainerExtractorBinder extractorBinder;
	private final BridgeResolver bridgeResolver;
	private final PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider;

	PojoIndexModelBinderImpl(MappingBuildContext buildContext,
			PojoBootstrapIntrospector introspector,
			ContainerExtractorBinder extractorBinder, BridgeResolver bridgeResolver,
			PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider) {
		this.beanResolver = buildContext.getBeanResolver();
		this.bridgeBuildContext = new BridgeBuildContextImpl( buildContext );
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
	public <I> BeanHolder<? extends IdentifierBridge<I>> addIdentifierBridge(
			IndexedEntityBindingContext bindingContext,
			BoundPojoModelPathPropertyNode<?, I> modelPath,
			IdentifierBridgeBuilder builder) {
		PojoGenericTypeModel<I> typeModel = modelPath.valueWithoutExtractors().getTypeModel();
		IdentifierBridgeBuilder defaultedBuilder = builder;
		if ( builder == null ) {
			defaultedBuilder = bridgeResolver.resolveIdentifierBridgeForType( typeModel );
		}

		BeanHolder<? extends IdentifierBridge<?>> bridgeHolder = defaultedBuilder.buildForIdentifier( bridgeBuildContext );
		try {
			// This cast is safe, see the similar cast in addValueBridge for a detailed explanation
			@SuppressWarnings({"unchecked", "rawtypes"})
			BeanHolder<? extends IdentifierBridge<I>> boundIdentifierBridge = bindIdentifierBridge(
					bindingContext, typeModel,
					(BeanHolder<? extends IdentifierBridge>) bridgeHolder
			);
			return boundIdentifierBridge;
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( holder -> holder.get().close(), bridgeHolder )
					.push( BeanHolder::close, bridgeHolder );
			throw e;
		}
	}

	@Override
	public <T> BoundRoutingKeyBridge<T> addRoutingKeyBridge(IndexedEntityBindingContext bindingContext,
			BoundPojoModelPathTypeNode<T> modelPath, RoutingKeyBridgeBuilder<?> builder) {
		BeanHolder<? extends RoutingKeyBridge> bridgeHolder = builder.buildForRoutingKey( bridgeBuildContext );
		try {
			PojoModelTypeRootElement<T> pojoModelRootElement =
					new PojoModelTypeRootElement<>( modelPath, typeAdditionalMetadataProvider );
			PojoTypeIndexingDependencyConfigurationContextImpl<T> pojoDependencyContext =
					new PojoTypeIndexingDependencyConfigurationContextImpl<>(
							introspector,
							extractorBinder,
							typeAdditionalMetadataProvider,
							modelPath.getTypeModel()
					);
			bridgeHolder.get().bind( new RoutingKeyBridgeBindingContextImpl(
					pojoModelRootElement,
					pojoDependencyContext
			) );

			bindingContext.explicitRouting();

			checkBridgeDependencies( pojoModelRootElement, pojoDependencyContext );

			return new BoundRoutingKeyBridge<>( bridgeHolder, pojoModelRootElement, pojoDependencyContext );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( holder -> holder.get().close(), bridgeHolder )
					.push( BeanHolder::close, bridgeHolder );
			throw e;
		}
	}

	@Override
	public <T> Optional<BoundTypeBridge<T>> addTypeBridge(IndexBindingContext bindingContext,
			BoundPojoModelPathTypeNode<T> modelPath, TypeBridgeBuilder<?> builder) {
		BeanHolder<? extends TypeBridge> bridgeHolder = builder.buildForType( bridgeBuildContext );
		try {
			PojoIndexSchemaContributionListener listener = new PojoIndexSchemaContributionListener();

			PojoModelTypeRootElement<T> pojoModelRootElement =
					new PojoModelTypeRootElement<>( modelPath, typeAdditionalMetadataProvider );
			PojoTypeIndexingDependencyConfigurationContextImpl<T> pojoDependencyContext =
					new PojoTypeIndexingDependencyConfigurationContextImpl<>(
							introspector,
							extractorBinder,
							typeAdditionalMetadataProvider,
							modelPath.getTypeModel()
					);
			bridgeHolder.get().bind( new TypeBridgeBindingContextImpl(
					pojoModelRootElement,
					pojoDependencyContext,
					bindingContext.createTypeFactory(),
					bindingContext.getSchemaElement( listener )
			) );

			checkBridgeDependencies( pojoModelRootElement, pojoDependencyContext );

			// If all fields are filtered out, we should ignore the bridge
			if ( listener.schemaContributed ) {
				return Optional.of( new BoundTypeBridge<>( bridgeHolder, pojoModelRootElement, pojoDependencyContext ) );
			}
			else {
				try ( Closer<RuntimeException> closer = new Closer<>() ) {
					closer.push( holder -> holder.get().close(), bridgeHolder );
					closer.push( BeanHolder::close, bridgeHolder );
				}
				return Optional.empty();
			}
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( holder -> holder.get().close(), bridgeHolder )
					.push( BeanHolder::close, bridgeHolder );
			throw e;
		}
	}

	@Override
	public <P> Optional<BoundPropertyBridge<P>> addPropertyBridge(IndexBindingContext bindingContext,
			BoundPojoModelPathPropertyNode<?, P> modelPath, PropertyBridgeBuilder<?> builder) {
		BeanHolder<? extends PropertyBridge> bridgeHolder = builder.buildForProperty( bridgeBuildContext );
		try {
			PojoIndexSchemaContributionListener listener = new PojoIndexSchemaContributionListener();

			PojoModelPropertyRootElement<P> pojoModelRootElement =
					new PojoModelPropertyRootElement<>( modelPath, typeAdditionalMetadataProvider );
			PojoPropertyIndexingDependencyConfigurationContextImpl<P> pojoDependencyContext =
					new PojoPropertyIndexingDependencyConfigurationContextImpl<>(
							introspector,
							extractorBinder,
							typeAdditionalMetadataProvider,
							modelPath
					);
			bridgeHolder.get().bind( new PropertyBridgeBindingContextImpl(
					pojoModelRootElement,
					pojoDependencyContext,
					bindingContext.createTypeFactory(),
					bindingContext.getSchemaElement( listener )
			) );

			checkBridgeDependencies( pojoModelRootElement, pojoDependencyContext );

			// If all fields are filtered out, we should ignore the bridge
			if ( listener.schemaContributed ) {
				return Optional.of( new BoundPropertyBridge<>(
						bridgeHolder, pojoModelRootElement, pojoDependencyContext
				) );
			}
			else {
				try ( Closer<RuntimeException> closer = new Closer<>() ) {
					closer.push( holder -> holder.get().close(), bridgeHolder );
					closer.push( BeanHolder::close, bridgeHolder );
				}
				return Optional.empty();
			}
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( holder -> holder.get().close(), bridgeHolder )
					.push( BeanHolder::close, bridgeHolder );
			throw e;
		}
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

	private <I, I2, B extends IdentifierBridge<I2>> BeanHolder<? extends IdentifierBridge<I>> bindIdentifierBridge(
			IndexedEntityBindingContext bindingContext, PojoGenericTypeModel<I> valueTypeModel,
			BeanHolder<? extends B> bridgeHolder) {
		IdentifierBridge<I2> bridge = bridgeHolder.get();

		GenericTypeContext bridgeTypeContext = new GenericTypeContext( bridge.getClass() );
		Class<?> bridgeParameterType = bridgeTypeContext.resolveTypeArgument( IdentifierBridge.class, 0 )
				.map( ReflectionUtils::getRawType )
				.orElseThrow( () -> new AssertionFailure(
						"Could not auto-detect the input type for identifier bridge '"
						+ bridge + "'."
						+ " There is a bug in Hibernate Search, please report it."
				) );
		// TODO HSEARCH-3243 perform more precise checks, we're just comparing raw types here and we might miss some type errors
		//  Also we're checking that the bridge parameter type is a subtype, but we really should be checking it
		//  is either the same type or a generic type parameter that can represent the same type.
		if ( !valueTypeModel.getRawType().isSubTypeOf( bridgeParameterType ) ) {
			throw log.invalidInputTypeForBridge( bridge, valueTypeModel );
		}

		@SuppressWarnings("unchecked") // Checked using reflection just above
		PojoGenericTypeModel<I2> castedValueTypeModel =
				(PojoGenericTypeModel<I2>) valueTypeModel;
		@SuppressWarnings("unchecked") // Checked using reflection just above
		BeanHolder<? extends IdentifierBridge<I>> castedBridgeHolder =
				(BeanHolder<? extends IdentifierBridge<I>>) bridgeHolder;

		bridge.bind( new IdentifierBridgeBindingContextImpl<>(
				new PojoModelValueElement<>( castedValueTypeModel )
		) );

		bindingContext.idDslConverter( new PojoIdentifierBridgeToDocumentIdentifierValueConverter<>( bridge ) );

		return castedBridgeHolder;
	}

	private void checkBridgeDependencies(AbstractPojoModelCompositeElement<?> pojoModelRootElement,
			AbstractPojoBridgedElementDependencyContext pojoDependencyContext) {
		boolean isUseRootOnly = pojoDependencyContext.isUseRootOnly();
		boolean hasDependency = pojoModelRootElement.hasDependency()
				|| pojoDependencyContext.hasNonRootDependency();
		boolean hasNonRootDependency = pojoModelRootElement.hasNonRootDependency()
				|| pojoDependencyContext.hasNonRootDependency();
		if ( isUseRootOnly && hasNonRootDependency ) {
			throw log.inconsistentBridgeDependencyDeclaration();
		}
		else if ( !isUseRootOnly && !hasDependency ) {
			throw log.missingBridgeDependencyDeclaration();
		}
	}

	private class PojoIndexSchemaContributionListener implements IndexSchemaContributionListener {
		private boolean schemaContributed = false;

		@Override
		public void onSchemaContributed() {
			schemaContributed = true;
		}
	}
}
