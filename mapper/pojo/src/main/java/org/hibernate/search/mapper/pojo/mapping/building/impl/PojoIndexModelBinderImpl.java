/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.dsl.StandardIndexSchemaFieldTypedContext;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.mapper.mapping.building.spi.FieldModelContributor;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexSchemaContributionListener;
import org.hibernate.search.engine.mapper.mapping.spi.MappingBuildContext;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.IdentifierBridgeBindingContextImpl;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.PropertyBridgeBindingContextImpl;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.RoutingKeyBridgeBindingContextImpl;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.TypeBridgeBindingContextImpl;
import org.hibernate.search.mapper.pojo.bridge.binding.impl.ValueBridgeBindingContextImpl;
import org.hibernate.search.mapper.pojo.bridge.impl.BridgeResolver;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuildContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerValueExtractorBinder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl.PojoTypeAdditionalMetadataProvider;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelPropertyRootElement;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelTypeRootElement;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelValueElement;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.util.impl.GenericTypeContext;
import org.hibernate.search.mapper.pojo.util.impl.ReflectionUtils;
import org.hibernate.search.util.impl.common.Closer;
import org.hibernate.search.util.impl.common.LoggerFactory;
import org.hibernate.search.util.impl.common.SuppressingCloser;

/**
 * @author Yoann Rodiere
 */
public class PojoIndexModelBinderImpl implements PojoIndexModelBinder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BridgeBuildContext bridgeBuildContext;
	private final ContainerValueExtractorBinder extractorBinder;
	private final BridgeResolver bridgeResolver;
	private final PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider;

	PojoIndexModelBinderImpl(MappingBuildContext buildContext,
			ContainerValueExtractorBinder extractorBinder, BridgeResolver bridgeResolver,
			PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider) {
		this.bridgeBuildContext = new BridgeBuildContextImpl( buildContext );
		this.extractorBinder = extractorBinder;
		this.bridgeResolver = bridgeResolver;
		this.typeAdditionalMetadataProvider = typeAdditionalMetadataProvider;
	}

	@Override
	public <C> BoundContainerValueExtractorPath<C, ?> bindExtractorPath(
			PojoGenericTypeModel<C> pojoGenericTypeModel, ContainerValueExtractorPath extractorPath) {
		return extractorBinder.bindPath( pojoGenericTypeModel, extractorPath );
	}

	@Override
	public <C, V> ContainerValueExtractor<? super C, V> createExtractors(
			BoundContainerValueExtractorPath<C, V> boundExtractorPath) {
		return extractorBinder.create( boundExtractorPath );
	}

	@Override
	public <I> BeanHolder<? extends IdentifierBridge<I>> addIdentifierBridge(BoundPojoModelPathPropertyNode<?, I> modelPath,
			BridgeBuilder<? extends IdentifierBridge<?>> builder) {
		PojoGenericTypeModel<I> typeModel = modelPath.valueWithoutExtractors().getTypeModel();
		BridgeBuilder<? extends IdentifierBridge<?>> defaultedBuilder = builder;
		if ( builder == null ) {
			defaultedBuilder = bridgeResolver.resolveIdentifierBridgeForType( typeModel );
		}
		/*
		 * TODO HSEARCH-3243 check that the bridge is suitable for the given typeModel
		 * (use introspection, similarly to what we do to detect the value bridge's field type?)
		 */
		BeanHolder<? extends IdentifierBridge<I>> bridgeHolder =
				(BeanHolder<IdentifierBridge<I>>) defaultedBuilder.build( bridgeBuildContext );
		try {
			bridgeHolder.get().bind( new IdentifierBridgeBindingContextImpl<>(
					new PojoModelValueElement<>( typeModel )
			) );
			return bridgeHolder;
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( holder -> holder.get().close(), bridgeHolder )
					.push( BeanHolder::close, bridgeHolder );
			throw e;
		}
	}

	@Override
	public <T> BoundRoutingKeyBridge<T> addRoutingKeyBridge(IndexModelBindingContext bindingContext,
			BoundPojoModelPathTypeNode<T> modelPath, BridgeBuilder<? extends RoutingKeyBridge> builder) {
		BeanHolder<? extends RoutingKeyBridge> bridgeHolder = builder.build( bridgeBuildContext );
		try {
			PojoModelTypeRootElement<T> pojoModelRootElement =
					new PojoModelTypeRootElement<>( modelPath, typeAdditionalMetadataProvider );
			bridgeHolder.get().bind( new RoutingKeyBridgeBindingContextImpl(
					pojoModelRootElement
			) );

			bindingContext.explicitRouting();

			return new BoundRoutingKeyBridge<>( bridgeHolder, pojoModelRootElement );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( holder -> holder.get().close(), bridgeHolder )
					.push( BeanHolder::close, bridgeHolder );
			throw e;
		}
	}

	@Override
	public <T> Optional<BoundTypeBridge<T>> addTypeBridge(IndexModelBindingContext bindingContext,
			BoundPojoModelPathTypeNode<T> modelPath, BridgeBuilder<? extends TypeBridge> builder) {
		BeanHolder<? extends TypeBridge> bridgeHolder = builder.build( bridgeBuildContext );
		try {
			PojoIndexSchemaContributionListener listener = new PojoIndexSchemaContributionListener();

			PojoModelTypeRootElement<T> pojoModelRootElement =
					new PojoModelTypeRootElement<>( modelPath, typeAdditionalMetadataProvider );
			bridgeHolder.get().bind( new TypeBridgeBindingContextImpl(
					pojoModelRootElement,
					bindingContext.getSchemaElement( listener )
			) );

			// If all fields are filtered out, we should ignore the bridge
			if ( listener.schemaContributed ) {
				return Optional.of( new BoundTypeBridge<>( bridgeHolder, pojoModelRootElement ) );
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
	public <P> Optional<BoundPropertyBridge<P>> addPropertyBridge(IndexModelBindingContext bindingContext,
			BoundPojoModelPathPropertyNode<?, P> modelPath, BridgeBuilder<? extends PropertyBridge> builder) {
		BeanHolder<? extends PropertyBridge> bridgeHolder = builder.build( bridgeBuildContext );
		try {
			PojoIndexSchemaContributionListener listener = new PojoIndexSchemaContributionListener();

			PojoModelPropertyRootElement<P> pojoModelRootElement =
					new PojoModelPropertyRootElement<>( modelPath, typeAdditionalMetadataProvider );
			bridgeHolder.get().bind( new PropertyBridgeBindingContextImpl(
					pojoModelRootElement,
					bindingContext.getSchemaElement( listener )
			) );

			// If all fields are filtered out, we should ignore the bridge
			if ( listener.schemaContributed ) {
				return Optional.of( new BoundPropertyBridge<>( bridgeHolder, pojoModelRootElement ) );
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
	public <V> Optional<BoundValueBridge<V, ?>> addValueBridge(IndexModelBindingContext bindingContext,
			BoundPojoModelPathValueNode<?, ?, V> modelPath, BridgeBuilder<? extends ValueBridge<?, ?>> builder,
			String relativeFieldName, FieldModelContributor contributor) {
		PojoGenericTypeModel<V> valueTypeModel = modelPath.getTypeModel();

		BridgeBuilder<? extends ValueBridge<?, ?>> defaultedBuilder = builder;
		if ( builder == null ) {
			defaultedBuilder = bridgeResolver.resolveValueBridgeForType( valueTypeModel );
		}

		BeanHolder<? extends ValueBridge<?, ?>> bridgeHolder = defaultedBuilder.build( bridgeBuildContext );
		try {

			PojoIndexSchemaContributionListener listener = new PojoIndexSchemaContributionListener();
			IndexSchemaElement schemaElement = bindingContext.getSchemaElement( listener );

			/*
			 * In general, this cast is illegal because SomeGenericType<? extends ValueBridge<?, ?>>,
			 * for all we know, may always return a different bridge for each invocation of its get() method:
			 * first a ValueBridge<Object, Integer>, then a ValueBridge<Long, String>, etc.
			 * Thus there may not a be a single pair of values V and F such that the generic type can safely
			 * be considered as implementing SomeGenericType<? extends ValueBridge<V, F>>.
			 * But in the case of BeanHolder, only one instance of the bridge is ever returned by get(),
			 * so there *is* a single pair of values V and F such that our bean holder can safely be considered
			 * an instance of BeanHolder<? extends ValueBridge<V, F>>.
			 */
			@SuppressWarnings({"unchecked", "rawtypes"})
			BoundValueBridge<V, ?> boundValueBridge = bindValueBridge(
					schemaElement, valueTypeModel,
					(BeanHolder<? extends ValueBridge>) bridgeHolder,
					relativeFieldName, contributor
			);

			// If all fields are filtered out, we should ignore the bridge
			if ( listener.schemaContributed ) {
				return Optional.of( boundValueBridge );
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

	private <V, V2, F, B extends ValueBridge<V2, F>> BoundValueBridge<V, ?> bindValueBridge(
			IndexSchemaElement schemaElement, PojoGenericTypeModel<V> valueTypeModel,
			BeanHolder<? extends B> bridgeHolder,
			String relativeFieldName, FieldModelContributor contributor) {
		B bridge = bridgeHolder.get();

		GenericTypeContext bridgeTypeContext = new GenericTypeContext( bridgeHolder.get().getClass() );
		Class<?> bridgeParameterType = bridgeTypeContext.resolveTypeArgument( ValueBridge.class, 0 )
				.map( ReflectionUtils::getRawType )
				.orElseThrow( () -> log.unableToInferValueBridgeInputType( bridge ) );
		// TODO HSEARCH-3243 perform more precise checks, we're just comparing raw types here and we might miss some type errors
		if ( !valueTypeModel.getRawType().isSubTypeOf( bridgeParameterType ) ) {
			throw log.invalidInputTypeForValueBridge( bridge, valueTypeModel );
		}

		@SuppressWarnings("unchecked") // Checked using reflection just above
		PojoGenericTypeModel<? extends V2> castedValueTypeModel =
				(PojoGenericTypeModel<? extends V2>) valueTypeModel;
		@SuppressWarnings("unchecked") // Checked using reflection just above
		BeanHolder<? extends ValueBridge<? super V, F>> castedBridgeHolder =
				(BeanHolder<? extends ValueBridge<? super V, F>>) bridgeHolder;

		// Then give the bridge a chance to contribute to the index schema
		IndexSchemaFieldContext fieldContext = schemaElement.field( relativeFieldName );
		ValueBridgeBindingContextImpl<V2> bridgeBindingContext = new ValueBridgeBindingContextImpl<>(
				new PojoModelValueElement<>( castedValueTypeModel ),
				fieldContext
		);
		StandardIndexSchemaFieldTypedContext<?, ? super F> typedFieldContext = bridge.bind( bridgeBindingContext );

		// If the bridge did not contribute anything, infer the field type and define it automatically
		if ( typedFieldContext == null ) {
			@SuppressWarnings( "unchecked" ) // We ensure this cast is safe through reflection
			Class<? super F> returnType =
					(Class<? super F>) bridgeTypeContext.resolveTypeArgument( ValueBridge.class, 1 )
					.map( ReflectionUtils::getRawType )
					.orElseThrow( () -> log.unableToInferValueBridgeIndexFieldType( bridge ) );
			typedFieldContext = fieldContext.as( returnType );
		}

		// Then register the bridge itself as a converter to use in the DSL
		typedFieldContext.dslConverter(
				new PojoValueBridgeToIndexFieldValueConverter<>( bridge )
		);

		// Then give the mapping a chance to override some of the model (add storage, ...)
		contributor.contribute( typedFieldContext );

		IndexFieldAccessor<? super F> indexFieldAccessor = typedFieldContext.createAccessor();

		return new BoundValueBridge<>( castedBridgeHolder, indexFieldAccessor );
	}

	private class PojoIndexSchemaContributionListener implements IndexSchemaContributionListener {
		private boolean schemaContributed = false;

		@Override
		public void onSchemaContributed() {
			schemaContributed = true;
		}
	}
}
