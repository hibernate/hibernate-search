/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import org.hibernate.search.engine.backend.document.model.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.IndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.FieldModelContributor;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.mapper.pojo.bridge.impl.BridgeResolver;
import org.hibernate.search.mapper.pojo.bridge.impl.FunctionBridgeUtil;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.Bridge;
import org.hibernate.search.mapper.pojo.bridge.FunctionBridge;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.model.PojoModelElement;
import org.hibernate.search.mapper.pojo.model.PojoModelElementAccessor;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.processing.impl.BridgeValueProcessor;
import org.hibernate.search.mapper.pojo.processing.impl.FunctionBridgeValueProcessor;
import org.hibernate.search.mapper.pojo.processing.impl.ValueProcessor;
import org.hibernate.search.util.SearchException;

/**
 * @author Yoann Rodiere
 */
public class PojoIndexModelBinderImpl implements PojoIndexModelBinder {

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
			defaultedBuilder = bridgeResolver.resolveIdentifierBridgeForType( typeModel.getJavaClass() );
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
	public ValueProcessor addBridge(IndexModelBindingContext bindingContext,
			PojoModelElement pojoModelElement, BridgeBuilder<? extends Bridge> builder) {
		return doAddBridge( bindingContext, pojoModelElement, builder );
	}

	@Override
	public ValueProcessor addFunctionBridge(IndexModelBindingContext bindingContext,
			PojoModelElement pojoModelElement, PojoTypeModel<?> typeModel,
			BridgeBuilder<? extends FunctionBridge<?, ?>> builder,
			String fieldName, FieldModelContributor contributor) {
		BridgeBuilder<? extends FunctionBridge<?, ?>> defaultedBuilder = builder;
		if ( builder == null ) {
			defaultedBuilder = bridgeResolver.resolveFunctionBridgeForType( typeModel.getJavaClass() );
		}

		// TODO check that the bridge is suitable for the given typeModel?
		FunctionBridge<?, ?> bridge = defaultedBuilder.build( buildContext );

		return doAddFunctionBridge( bindingContext, pojoModelElement, bridge, fieldName, contributor );
	}

	private ValueProcessor doAddBridge(IndexModelBindingContext bindingContext,
			PojoModelElement pojoModelElement, BridgeBuilder<? extends Bridge> builder) {
		Bridge bridge = builder.build( buildContext );

		// FIXME if all fields are filtered out, we should ignore the processor
		bridge.bind( bindingContext.getSchemaElement(), pojoModelElement, bindingContext.getSearchModel() );

		return new BridgeValueProcessor( bridge );
	}

	private <T, R> ValueProcessor doAddFunctionBridge(IndexModelBindingContext bindingContext,
			PojoModelElement pojoModelElement, FunctionBridge<T, R> bridge,
			String fieldName, FieldModelContributor contributor) {
		PojoModelElementAccessor<? extends T> pojoModelElementAccessor = getReferenceForBridge( pojoModelElement, bridge );
		return doAddFunctionBridge( bindingContext, pojoModelElementAccessor, bridge, fieldName, contributor );
	}

	@SuppressWarnings("unchecked")
	private <T> PojoModelElementAccessor<? extends T> getReferenceForBridge(PojoModelElement pojoModelElement,
			FunctionBridge<T, ?> bridge) {
		return FunctionBridgeUtil.inferParameterType( bridge )
				.map( c -> pojoModelElement.createAccessor( c ) )
				.orElse( (PojoModelElementAccessor<T>) pojoModelElement.createAccessor() );
	}

	private <T, R> ValueProcessor doAddFunctionBridge(IndexModelBindingContext bindingContext,
			PojoModelElementAccessor<? extends T> pojoModelElementAccessor,
			FunctionBridge<T, R> bridge, String fieldName, FieldModelContributor contributor) {
		IndexSchemaFieldContext fieldContext = bindingContext.getSchemaElement().field( fieldName );

		// First give the bridge a chance to contribute to the model
		IndexSchemaFieldTypedContext<R> typedFieldContext = bridge.bind( fieldContext );
		if ( typedFieldContext == null ) {
			Class<R> returnType = FunctionBridgeUtil.inferReturnType( bridge )
					.orElseThrow( () -> new SearchException( "Could not auto-detect the return type for bridge "
							+ bridge + "; configure encoding explicitly in the bridge." ) );
			typedFieldContext = fieldContext.as( returnType );
		}
		// Then give the mapping a chance to override some of the model (add storage, ...)
		contributor.contribute( typedFieldContext );

		// FIXME if the field is filtered out, we should ignore the processor

		IndexFieldAccessor<R> indexFieldAccessor = typedFieldContext.createAccessor();
		return new FunctionBridgeValueProcessor<>( bridge, pojoModelElementAccessor, indexFieldAccessor );
	}

}
