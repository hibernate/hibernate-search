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
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTypedContext;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.FieldModelContributor;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexSchemaContributionListener;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.impl.BridgeResolver;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractor;
import org.hibernate.search.mapper.pojo.extractor.impl.BoundContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.extractor.impl.ContainerValueExtractorBinder;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl.PojoTypeAdditionalMetadataProvider;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelPropertyRootElement;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelTypeRootElement;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathTypeNode;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.util.impl.GenericTypeContext;
import org.hibernate.search.mapper.pojo.util.impl.ReflectionUtils;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * @author Yoann Rodiere
 */
public class PojoIndexModelBinderImpl implements PojoIndexModelBinder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BuildContext buildContext;
	private final PojoBootstrapIntrospector introspector;
	private final ContainerValueExtractorBinder extractorBinder;
	private final BridgeResolver bridgeResolver;
	private final PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider;

	PojoIndexModelBinderImpl(BuildContext buildContext, PojoBootstrapIntrospector introspector,
			ContainerValueExtractorBinder extractorBinder, BridgeResolver bridgeResolver,
			PojoTypeAdditionalMetadataProvider typeAdditionalMetadataProvider) {
		this.buildContext = buildContext;
		this.introspector = introspector;
		this.extractorBinder = extractorBinder;
		this.bridgeResolver = bridgeResolver;
		this.typeAdditionalMetadataProvider = typeAdditionalMetadataProvider;
	}

	@Override
	public <C> BoundContainerValueExtractorPath<C, ?> bindExtractorPath(
			PojoGenericTypeModel<C> pojoGenericTypeModel, ContainerValueExtractorPath extractorPath) {
		return extractorBinder.bindPath( introspector, pojoGenericTypeModel, extractorPath );
	}

	@Override
	public <C> Optional<BoundContainerValueExtractorPath<C, ?>> tryBindExtractorPath(
			PojoGenericTypeModel<C> pojoGenericTypeModel, ContainerValueExtractorPath extractorPath) {
		return extractorBinder.tryBindPath( introspector, pojoGenericTypeModel, extractorPath );
	}

	@Override
	public <C, V> Optional<? extends ContainerValueExtractor<? super C, V>> tryCreateExtractors(
			BoundContainerValueExtractorPath<C, V> boundExtractorPath) {
		return extractorBinder.tryCreate( boundExtractorPath );
	}

	@Override
	public <C, V> ContainerValueExtractor<? super C, V> createExtractors(
			BoundContainerValueExtractorPath<C, V> boundExtractorPath) {
		return extractorBinder.create( boundExtractorPath );
	}

	@Override
	public <P> IdentifierBridge<P> createIdentifierBridge(BoundPojoModelPathPropertyNode<?, P> modelPath,
			BridgeBuilder<? extends IdentifierBridge<?>> builder) {
		PojoTypeModel<P> typeModel = modelPath.valueWithoutExtractors().type().getTypeModel();
		BridgeBuilder<? extends IdentifierBridge<?>> defaultedBuilder = builder;
		if ( builder == null ) {
			defaultedBuilder = bridgeResolver.resolveIdentifierBridgeForType( typeModel );
		}
		/*
		 * TODO check that the bridge is suitable for the given typeModel
		 * (use introspection, similarly to what we do to detect the value bridge's field type?)
		 */
		IdentifierBridge<?> bridge = defaultedBuilder.build( buildContext );

		return (IdentifierBridge<P>) bridge;
	}

	@Override
	public <T> BoundRoutingKeyBridge<T> addRoutingKeyBridge(IndexModelBindingContext bindingContext,
			BoundPojoModelPathTypeNode<T> modelPath, BridgeBuilder<? extends RoutingKeyBridge> builder) {
		RoutingKeyBridge bridge = builder.build( buildContext );

		PojoModelTypeRootElement<T> pojoModelRootElement =
				new PojoModelTypeRootElement<>( modelPath, typeAdditionalMetadataProvider );
		bridge.bind( pojoModelRootElement );

		bindingContext.explicitRouting();

		return new BoundRoutingKeyBridge<>( bridge, pojoModelRootElement );
	}

	@Override
	public <T> Optional<BoundTypeBridge<T>> addTypeBridge(IndexModelBindingContext bindingContext,
			BoundPojoModelPathTypeNode<T> modelPath, BridgeBuilder<? extends TypeBridge> builder) {
		TypeBridge bridge = builder.build( buildContext );

		IndexSchemaContributionListenerImpl listener = new IndexSchemaContributionListenerImpl();

		PojoModelTypeRootElement<T> pojoModelRootElement =
				new PojoModelTypeRootElement<>( modelPath, typeAdditionalMetadataProvider );
		bridge.bind(
				bindingContext.getSchemaElement( listener ), pojoModelRootElement, bindingContext.getSearchModel()
		);

		// If all fields are filtered out, we should ignore the bridge
		if ( listener.schemaContributed ) {
			return Optional.of( new BoundTypeBridge<>( bridge, pojoModelRootElement ) );
		}
		else {
			bridge.close();
			return Optional.empty();
		}
	}

	@Override
	public <P> Optional<BoundPropertyBridge<P>> addPropertyBridge(IndexModelBindingContext bindingContext,
			BoundPojoModelPathPropertyNode<?, P> modelPath, BridgeBuilder<? extends PropertyBridge> builder) {
		PropertyBridge bridge = builder.build( buildContext );

		IndexSchemaContributionListenerImpl listener = new IndexSchemaContributionListenerImpl();

		PojoModelPropertyRootElement<P> pojoModelRootElement =
				new PojoModelPropertyRootElement<>( modelPath, typeAdditionalMetadataProvider );
		bridge.bind(
				bindingContext.getSchemaElement( listener ), pojoModelRootElement, bindingContext.getSearchModel()
		);

		// If all fields are filtered out, we should ignore the bridge
		if ( listener.schemaContributed ) {
			return Optional.of( new BoundPropertyBridge<>( bridge, pojoModelRootElement ) );
		}
		else {
			bridge.close();
			return Optional.empty();
		}
	}

	@Override
	public <V> Optional<BoundValueBridge<? super V, ?>> addValueBridge(IndexModelBindingContext bindingContext,
			BoundPojoModelPathValueNode<?, ?, V> modelPath, BridgeBuilder<? extends ValueBridge<?, ?>> builder,
			String relativeFieldName, FieldModelContributor contributor) {
		PojoTypeModel<V> typeModel = modelPath.type().getTypeModel();

		BridgeBuilder<? extends ValueBridge<?, ?>> defaultedBuilder = builder;
		if ( builder == null ) {
			defaultedBuilder = bridgeResolver.resolveValueBridgeForType( typeModel );
		}

		ValueBridge<?, ?> bridge = defaultedBuilder.build( buildContext );

		GenericTypeContext bridgeTypeContext = new GenericTypeContext( bridge.getClass() );

		Class<?> bridgeParameterType = bridgeTypeContext.resolveTypeArgument( ValueBridge.class, 0 )
				.map( ReflectionUtils::getRawType )
				.orElseThrow( () -> log.unableToInferValueBridgeInputType( bridge ) );
		if ( !typeModel.getRawType().isSubTypeOf( bridgeParameterType ) ) {
			throw log.invalidInputTypeForValueBridge( bridge, typeModel );
		}

		@SuppressWarnings( "unchecked" ) // We checked just above that this cast is valid
		ValueBridge<? super V, ?> typedBridge = (ValueBridge<? super V, ?>) bridge;

		return doAddValueBridge( bindingContext, typedBridge, bridgeTypeContext, relativeFieldName, contributor );
	}

	private <T, R> Optional<BoundValueBridge<? super T, ?>> doAddValueBridge(IndexModelBindingContext bindingContext,
			ValueBridge<? super T, R> bridge, GenericTypeContext bridgeTypeContext,
			String relativeFieldName, FieldModelContributor contributor) {
		IndexSchemaContributionListenerImpl listener = new IndexSchemaContributionListenerImpl();

		IndexSchemaFieldContext fieldContext = bindingContext.getSchemaElement( listener ).field( relativeFieldName );

		// First give the bridge a chance to contribute to the model
		IndexSchemaFieldTypedContext<? super R> typedFieldContext = bridge.bind( fieldContext );
		if ( typedFieldContext == null ) {
			@SuppressWarnings( "unchecked" ) // We ensure this cast is safe through reflection
			Class<? super R> returnType =
					(Class<? super R>) bridgeTypeContext.resolveTypeArgument( ValueBridge.class, 1 )
					.map( ReflectionUtils::getRawType )
					.orElseThrow( () -> log.unableToInferValueBridgeIndexFieldType( bridge ) );
			typedFieldContext = fieldContext.as( returnType );
		}
		// Then give the mapping a chance to override some of the model (add storage, ...)
		contributor.contribute( typedFieldContext );

		IndexFieldAccessor<? super R> indexFieldAccessor = typedFieldContext.createAccessor();

		// If all fields are filtered out, we should ignore the bridge
		if ( listener.schemaContributed ) {
			return Optional.of( new BoundValueBridge<>( bridge, indexFieldAccessor ) );
		}
		else {
			bridge.close();
			return Optional.empty();
		}
	}

	private class IndexSchemaContributionListenerImpl implements IndexSchemaContributionListener {
		private boolean schemaContributed = false;

		@Override
		public void onSchemaContributed() {
			schemaContributed = true;
		}
	}
}
