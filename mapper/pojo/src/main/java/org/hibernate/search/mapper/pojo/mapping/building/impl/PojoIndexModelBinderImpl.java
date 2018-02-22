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
import org.hibernate.search.engine.backend.document.model.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.IndexSchemaFieldTypedContext;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.FieldModelContributor;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexSchemaContributionListener;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.FunctionBridge;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.impl.BridgeResolver;
import org.hibernate.search.mapper.pojo.bridge.impl.FunctionBridgeUtil;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.PojoModelElement;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;
import org.hibernate.search.mapper.pojo.model.PojoModelType;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.processing.impl.FunctionBridgeProcessor;
import org.hibernate.search.util.spi.LoggerFactory;

/**
 * @author Yoann Rodiere
 */
public class PojoIndexModelBinderImpl implements PojoIndexModelBinder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BuildContext buildContext;
	private final BridgeResolver bridgeResolver;

	PojoIndexModelBinderImpl(BuildContext buildContext, BridgeResolver bridgeResolver) {
		this.buildContext = buildContext;
		this.bridgeResolver = bridgeResolver;
	}

	@Override
	public <T> IdentifierBridge<T> createIdentifierBridge(PojoModelElement pojoModelElement, PojoTypeModel<T> typeModel,
			BridgeBuilder<? extends IdentifierBridge<?>> builder) {
		BridgeBuilder<? extends IdentifierBridge<?>> defaultedBuilder = builder;
		if ( builder == null ) {
			defaultedBuilder = bridgeResolver.resolveIdentifierBridgeForType( typeModel );
		}
		/*
		 * TODO check that the bridge is suitable for the given typeModel
		 * (use introspection, similarly to what we do to detect the function bridges field type?)
		 */
		IdentifierBridge<?> bridge = defaultedBuilder.build( buildContext );

		return (IdentifierBridge<T>) bridge;
	}

	@Override
	public RoutingKeyBridge addRoutingKeyBridge(IndexModelBindingContext bindingContext,
			PojoModelElement pojoModelElement, BridgeBuilder<? extends RoutingKeyBridge> builder) {
		RoutingKeyBridge bridge = builder.build( buildContext );
		bridge.bind( pojoModelElement );

		bindingContext.explicitRouting();

		return bridge;
	}

	@Override
	public Optional<TypeBridge> addTypeBridge(IndexModelBindingContext bindingContext,
			PojoModelType pojoModelType, BridgeBuilder<? extends TypeBridge> builder) {
		TypeBridge bridge = builder.build( buildContext );

		IndexSchemaContributionListenerImpl listener = new IndexSchemaContributionListenerImpl();

		bridge.bind( bindingContext.getSchemaElement( listener ), pojoModelType, bindingContext.getSearchModel() );

		// If all fields are filtered out, we should ignore the bridge
		if ( listener.schemaContributed ) {
			return Optional.of( bridge );
		}
		else {
			bridge.close();
			return Optional.empty();
		}
	}

	@Override
	public Optional<PropertyBridge> addPropertyBridge(IndexModelBindingContext bindingContext,
			PojoModelProperty pojoModelProperty, BridgeBuilder<? extends PropertyBridge> builder) {
		PropertyBridge bridge = builder.build( buildContext );

		IndexSchemaContributionListenerImpl listener = new IndexSchemaContributionListenerImpl();

		bridge.bind( bindingContext.getSchemaElement( listener ), pojoModelProperty, bindingContext.getSearchModel() );

		// If all fields are filtered out, we should ignore the bridge
		if ( listener.schemaContributed ) {
			return Optional.of( bridge );
		}
		else {
			bridge.close();
			return Optional.empty();
		}
	}

	@Override
	public <T> Optional<FunctionBridgeProcessor<T, ?>> addFunctionBridge(IndexModelBindingContext bindingContext,
			PojoTypeModel<T> typeModel, BridgeBuilder<? extends FunctionBridge<?, ?>> builder,
			String fieldName, FieldModelContributor contributor) {
		BridgeBuilder<? extends FunctionBridge<?, ?>> defaultedBuilder = builder;
		if ( builder == null ) {
			defaultedBuilder = bridgeResolver.resolveFunctionBridgeForType( typeModel );
		}

		FunctionBridge<?, ?> bridge = defaultedBuilder.build( buildContext );

		Class<?> bridgeParameterType = FunctionBridgeUtil.inferInputType( bridge )
				.orElseThrow( () -> log.unableToInferFunctionBridgeInputType( bridge ) );
		if ( !typeModel.getSuperType( bridgeParameterType ).isPresent() ) {
			throw log.invalidInputTypeForFunctionBridge( bridge, typeModel );
		}

		@SuppressWarnings( "unchecked" ) // We checked just above that this cast is valid
		FunctionBridge<? super T, ?> typedBridge = (FunctionBridge<? super T, ?>) bridge;

		return doAddFunctionBridge( bindingContext, typedBridge, fieldName, contributor );
	}

	private <T, R> Optional<FunctionBridgeProcessor<T, ?>> doAddFunctionBridge(IndexModelBindingContext bindingContext,
			FunctionBridge<? super T, R> bridge, String fieldName, FieldModelContributor contributor) {
		IndexSchemaContributionListenerImpl listener = new IndexSchemaContributionListenerImpl();

		IndexSchemaFieldContext fieldContext = bindingContext.getSchemaElement( listener ).field( fieldName );

		// First give the bridge a chance to contribute to the model
		IndexSchemaFieldTypedContext<R> typedFieldContext = bridge.bind( fieldContext );
		if ( typedFieldContext == null ) {
			Class<R> returnType = FunctionBridgeUtil.inferReturnType( bridge )
					.orElseThrow( () -> log.unableToInferFunctionBridgeIndexFieldType( bridge ) );
			typedFieldContext = fieldContext.as( returnType );
		}
		// Then give the mapping a chance to override some of the model (add storage, ...)
		contributor.contribute( typedFieldContext );

		IndexFieldAccessor<R> indexFieldAccessor = typedFieldContext.createAccessor();


		// If all fields are filtered out, we should ignore the bridge
		if ( listener.schemaContributed ) {
			return Optional.of( new FunctionBridgeProcessor<>( bridge, indexFieldAccessor ) );
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
