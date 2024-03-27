/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.bridge;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.model.PojoElementAccessor;
import org.hibernate.search.mapper.pojo.model.PojoModelType;

/**
 * Test automatic indexing based on Hibernate ORM entity events when
 * {@link TypeBridge}s or {@link PropertyBridge}s are involved
 * and rely on POJO accessors.
 */
public class AutomaticIndexingBridgeAccessorsIT extends AbstractAutomaticIndexingBridgeIT {

	@Override
	protected TypeBinder createContainingEntityTypeBinder() {
		return new ContainingEntityTypeBridge.Binder();
	}

	@Override
	protected PropertyBinder createContainingEntitySingleValuedPropertyBinder() {
		return new ContainingEntitySingleValuedPropertyBridge.Binder();
	}

	@Override
	protected PropertyBinder createContainingEntityMultiValuedPropertyBinder() {
		return null; // Not supported with accessors
	}

	public static class ContainingEntityTypeBridge implements TypeBridge<Object> {

		private final PojoElementAccessor<String> directFieldSourceAccessor;
		private final PojoElementAccessor<String> includedInTypeBridgeFieldSourceAccessor;
		private final IndexObjectFieldReference typeBridgeObjectFieldReference;
		private final IndexFieldReference<String> directFieldReference;
		private final IndexObjectFieldReference childObjectFieldReference;
		private final IndexFieldReference<String> includedInTypeBridgeFieldReference;

		private ContainingEntityTypeBridge(TypeBindingContext context) {
			PojoModelType bridgedElement = context.bridgedElement();
			directFieldSourceAccessor = bridgedElement.property( "directField" )
					.createAccessor( String.class );
			includedInTypeBridgeFieldSourceAccessor = bridgedElement.property( "association1" )
					.property( "containedSingle" )
					.property( "includedInTypeBridge" )
					.createAccessor( String.class );
			IndexSchemaObjectField typeBridgeObjectField = context.indexSchemaElement().objectField( "typeBridge" );
			typeBridgeObjectFieldReference = typeBridgeObjectField.toReference();
			directFieldReference = typeBridgeObjectField.field( "directField", f -> f.asString() )
					.toReference();
			IndexSchemaObjectField childObjectField = typeBridgeObjectField.objectField( "child" );
			childObjectFieldReference = childObjectField.toReference();
			includedInTypeBridgeFieldReference = childObjectField.field(
					"includedInTypeBridge", f -> f.asString()
			)
					.toReference();
		}

		@Override
		public void write(DocumentElement target, Object bridgedElement, TypeBridgeWriteContext context) {
			DocumentElement typeBridgeObjectField = target.addObject( typeBridgeObjectFieldReference );
			typeBridgeObjectField.addValue( directFieldReference, directFieldSourceAccessor.read( bridgedElement ) );
			DocumentElement childObjectField = typeBridgeObjectField.addObject( childObjectFieldReference );
			childObjectField.addValue(
					includedInTypeBridgeFieldReference, includedInTypeBridgeFieldSourceAccessor.read( bridgedElement )
			);
		}

		public static class Binder implements TypeBinder {
			@Override
			public void bind(TypeBindingContext context) {
				context.bridge( new ContainingEntityTypeBridge( context ) );
			}
		}
	}

	public static class ContainingEntitySingleValuedPropertyBridge implements PropertyBridge<Object> {

		private final PojoElementAccessor<String> includedInPropertyBridgeSourceAccessor;
		private final IndexObjectFieldReference propertyBridgeObjectFieldReference;
		private final IndexFieldReference<String> includedInPropertyBridgeFieldReference;

		private ContainingEntitySingleValuedPropertyBridge(PropertyBindingContext context) {
			includedInPropertyBridgeSourceAccessor = context.bridgedElement().property( "containedSingle" )
					.property( "includedInSingleValuedPropertyBridge" )
					.createAccessor( String.class );
			IndexSchemaObjectField propertyBridgeObjectField =
					context.indexSchemaElement().objectField( "singleValuedPropertyBridge" );
			propertyBridgeObjectFieldReference = propertyBridgeObjectField.toReference();
			includedInPropertyBridgeFieldReference = propertyBridgeObjectField.field(
					"includedInSingleValuedPropertyBridge", f -> f.asString()
			)
					.toReference();
		}

		@Override
		public void write(DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context) {
			DocumentElement propertyBridgeObjectField = target.addObject( propertyBridgeObjectFieldReference );
			propertyBridgeObjectField.addValue(
					includedInPropertyBridgeFieldReference, includedInPropertyBridgeSourceAccessor.read(
							bridgedElement )
			);
		}

		public static class Binder implements PropertyBinder {
			@Override
			public void bind(PropertyBindingContext context) {
				context.bridge( new ContainingEntitySingleValuedPropertyBridge( context ) );
			}
		}
	}
}
