/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBridgeBindingContext;
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
	protected Class<? extends TypeBridge> getContainingEntityTypeBridgeClass() {
		return ContainingEntityTypeBridge.class;
	}

	@Override
	protected Class<? extends PropertyBridge> getContainingEntityPropertyBridgeClass() {
		return ContainingEntityPropertyBridge.class;
	}

	public static class ContainingEntityTypeBridge implements TypeBridge {

		private PojoElementAccessor<String> directFieldSourceAccessor;
		private PojoElementAccessor<String> includedInTypeBridgeFieldSourceAccessor;
		private IndexObjectFieldReference typeBridgeObjectFieldReference;
		private IndexFieldReference<String> directFieldReference;
		private IndexObjectFieldReference childObjectFieldReference;
		private IndexFieldReference<String> includedInTypeBridgeFieldReference;

		@Override
		public void bind(TypeBridgeBindingContext context) {
			PojoModelType bridgedElement = context.getBridgedElement();
			directFieldSourceAccessor = bridgedElement.property( "directField" )
					.createAccessor( String.class );
			includedInTypeBridgeFieldSourceAccessor = bridgedElement.property( "child" )
					.property( "containedSingle" )
					.property( "includedInTypeBridge" )
					.createAccessor( String.class );
			IndexSchemaObjectField typeBridgeObjectField = context.getIndexSchemaElement().objectField( "typeBridge" );
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
	}

	public static class ContainingEntityPropertyBridge implements PropertyBridge {

		private PojoElementAccessor<String> includedInPropertyBridgeSourceAccessor;
		private IndexObjectFieldReference propertyBridgeObjectFieldReference;
		private IndexFieldReference<String> includedInPropertyBridgeFieldReference;

		@Override
		public void bind(PropertyBridgeBindingContext context) {
			includedInPropertyBridgeSourceAccessor = context.getBridgedElement().property( "containedSingle" )
					.property( "includedInPropertyBridge" )
					.createAccessor( String.class );
			IndexSchemaObjectField propertyBridgeObjectField = context.getIndexSchemaElement().objectField( "propertyBridge" );
			propertyBridgeObjectFieldReference = propertyBridgeObjectField.toReference();
			includedInPropertyBridgeFieldReference = propertyBridgeObjectField.field(
					"includedInPropertyBridge", f -> f.asString()
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
	}
}
