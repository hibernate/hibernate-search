/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import java.lang.annotation.Annotation;

import org.hibernate.search.engine.backend.document.model.spi.FieldModelContext;
import org.hibernate.search.engine.backend.document.model.spi.TypedFieldModelContext;
import org.hibernate.search.engine.backend.document.spi.IndexFieldAccessor;
import org.hibernate.search.engine.common.spi.BeanReference;
import org.hibernate.search.engine.mapper.mapping.building.spi.FieldModelContributor;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexModelBindingContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoModelElementAccessor;
import org.hibernate.search.mapper.pojo.model.spi.PojoModelElement;
import org.hibernate.search.mapper.pojo.bridge.impl.BridgeFactory;
import org.hibernate.search.mapper.pojo.bridge.impl.BridgeReferenceResolver;
import org.hibernate.search.mapper.pojo.bridge.impl.FunctionBridgeUtil;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeDefinition;
import org.hibernate.search.mapper.pojo.bridge.spi.Bridge;
import org.hibernate.search.mapper.pojo.bridge.spi.FunctionBridge;
import org.hibernate.search.mapper.pojo.bridge.spi.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.spi.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.processing.impl.BridgeValueProcessor;
import org.hibernate.search.mapper.pojo.processing.impl.FunctionBridgeValueProcessor;
import org.hibernate.search.mapper.pojo.processing.impl.ValueProcessor;
import org.hibernate.search.util.SearchException;

/**
 * @author Yoann Rodiere
 */
public class PojoIndexModelBinderImpl implements PojoIndexModelBinder {

	private final BridgeFactory bridgeFactory;
	private final BridgeReferenceResolver bridgeReferenceResolver;

	public PojoIndexModelBinderImpl(BridgeFactory bridgeFactory,
			BridgeReferenceResolver bridgeReferenceResolver) {
		this.bridgeFactory = bridgeFactory;
		this.bridgeReferenceResolver = bridgeReferenceResolver;
	}

	@Override
	public <T> IdentifierBridge<T> createIdentifierBridge(Class<T> sourceType, BeanReference<? extends IdentifierBridge<?>> reference) {
		BeanReference<? extends IdentifierBridge<?>> defaultedReference = reference;
		if ( isEmpty( reference ) ) {
			defaultedReference = bridgeReferenceResolver.resolveIdentifierBridgeForType( sourceType );
		}
		/*
		 * TODO check that the bridge is suitable for the given sourceType
		 * (use introspection, similarly to what we do to detect the function bridges field type?)
		 */
		IdentifierBridge<?> bridge = bridgeFactory.createIdentifierBridge( defaultedReference );

		return (IdentifierBridge<T>) bridge;
	}

	@Override
	public RoutingKeyBridge addRoutingKeyBridge(IndexModelBindingContext bindingContext,
			PojoModelElement pojoModelElement, BeanReference<? extends RoutingKeyBridge> reference) {
		RoutingKeyBridge bridge = bridgeFactory.createRoutingKeyBridge( reference );
		bridge.bind( pojoModelElement );

		bindingContext.explicitRouting();

		return bridge;
	}

	@Override
	public ValueProcessor addBridge(IndexModelBindingContext bindingContext,
			PojoModelElement pojoModelElement, BridgeDefinition<?> definition) {
		return doAddBridge( bindingContext, pojoModelElement, definition );
	}

	@Override
	public ValueProcessor addFunctionBridge(IndexModelBindingContext bindingContext,
			PojoModelElement pojoModelElement, Class<?> sourceType,
			BeanReference<? extends FunctionBridge<?, ?>> bridgeReference,
			String fieldName, FieldModelContributor contributor) {

		BeanReference<? extends FunctionBridge<?, ?>> defaultedReference = bridgeReference;
		if ( isEmpty( defaultedReference ) ) {
			defaultedReference = bridgeReferenceResolver.resolveFunctionBridgeForType( sourceType );
		}

		FunctionBridge<?, ?> bridge = bridgeFactory.createFunctionBridge( defaultedReference );

		// TODO check that the bridge is suitable for the given sourceType?

		return doAddFunctionBridge( bindingContext, pojoModelElement, bridge, fieldName, contributor );
	}

	private boolean isEmpty(BeanReference<?> reference) {
		return reference == null || reference.getName() == null && reference.getType() == null;
	}

	private <A extends Annotation> ValueProcessor doAddBridge(IndexModelBindingContext bindingContext,
			PojoModelElement pojoModelElement, BridgeDefinition<A> definition) {
		A annotation = definition.get();
		@SuppressWarnings("unchecked")
		Class<A> annotationType = (Class<A>) annotation.annotationType();
		BeanReference<? extends Bridge<?>> reference = bridgeReferenceResolver.resolveBridgeForAnnotationType( annotationType );

		Bridge<?> bridge = bridgeFactory.createBridge( reference, annotation );

		// FIXME if all fields are filtered out, we should ignore the processor
		bridge.contribute( bindingContext.getSchemaElement(), pojoModelElement, bindingContext.getSearchModel() );

		return new BridgeValueProcessor( bridge );
	}

	private <T, R> ValueProcessor doAddFunctionBridge(IndexModelBindingContext bindingContext,
			PojoModelElement pojoModelElement, FunctionBridge<T, R> bridge,
			String fieldName, FieldModelContributor contributor) {
		PojoModelElementAccessor<? extends T> pojoModelElementAccessor = getReferenceForBridge( pojoModelElement, bridge );
		return doAddFunctionBridge( bindingContext, pojoModelElementAccessor, bridge, fieldName, contributor );
	}

	@SuppressWarnings("unchecked")
	private <T> PojoModelElementAccessor<? extends T> getReferenceForBridge(PojoModelElement pojoModelElement, FunctionBridge<T, ?> bridge) {
		return FunctionBridgeUtil.inferParameterType( bridge )
				.map( c -> pojoModelElement.createAccessor( c ) )
				.orElse( (PojoModelElementAccessor<T>) pojoModelElement.createAccessor() );
	}

	private <T, R> ValueProcessor doAddFunctionBridge(IndexModelBindingContext bindingContext,
			PojoModelElementAccessor<? extends T> pojoModelElementAccessor,
			FunctionBridge<T, R> bridge, String fieldName, FieldModelContributor contributor) {
		FieldModelContext fieldContext = bindingContext.getSchemaElement().field( fieldName );

		// First give the bridge a chance to contribute to the model
		TypedFieldModelContext<R> typedFieldContext = bridge.bind( fieldContext );
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
