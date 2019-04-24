/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import java.util.List;
import java.util.stream.Collectors;

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
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

/**
 * Test automatic indexing based on Hibernate ORM entity events when
 * {@link TypeBridge}s or {@link PropertyBridge}s are involved
 * and rely on explicit dependency declaration.
 */
@TestForIssue(jiraKey = "HSEARCH-3297")
public class AutomaticIndexingBridgeExplicitDependenciesIT extends AbstractAutomaticIndexingBridgeIT {

	@Override
	protected Class<? extends TypeBridge> getContainingEntityTypeBridgeClass() {
		return ContainingEntityTypeBridge.class;
	}

	@Override
	protected Class<? extends PropertyBridge> getContainingEntitySingleValuedPropertyBridgeClass() {
		return ContainingEntitySingleValuedPropertyBridge.class;
	}

	@Override
	protected Class<? extends PropertyBridge> getContainingEntityMultiValuedPropertyBridgeClass() {
		return ContainingEntityMultiValuedPropertyBridge.class;
	}

	public static class ContainingEntityTypeBridge implements TypeBridge {

		private IndexObjectFieldReference typeBridgeObjectFieldReference;
		private IndexFieldReference<String> directFieldReference;
		private IndexObjectFieldReference childObjectFieldReference;
		private IndexFieldReference<String> includedInTypeBridgeFieldReference;

		@Override
		public void bind(TypeBridgeBindingContext context) {
			context.getDependencies()
					.use( "directField" )
					.use( "association1.containedSingle.includedInTypeBridge" );

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
			ContainingEntity castedBridgedElement = (ContainingEntity) bridgedElement;

			DocumentElement typeBridgeObjectField = target.addObject( typeBridgeObjectFieldReference );
			typeBridgeObjectField.addValue(
					directFieldReference,
					castedBridgedElement.getDirectField()
			);

			ContainingEntity child = castedBridgedElement.getAssociation1();
			DocumentElement childObjectField = typeBridgeObjectField.addObject( childObjectFieldReference );

			ContainedEntity containedSingle = child == null ? null : child.getContainedSingle();
			childObjectField.addValue(
					includedInTypeBridgeFieldReference,
					containedSingle == null ? null : containedSingle.getIncludedInTypeBridge()
			);
		}
	}

	public static class ContainingEntitySingleValuedPropertyBridge implements PropertyBridge {

		private IndexObjectFieldReference propertyBridgeObjectFieldReference;
		private IndexFieldReference<String> includedInPropertyBridgeFieldReference;

		@Override
		public void bind(PropertyBridgeBindingContext context) {
			context.getDependencies()
					.use( "containedSingle.includedInSingleValuedPropertyBridge" );

			IndexSchemaObjectField propertyBridgeObjectField = context.getIndexSchemaElement().objectField( "singleValuedPropertyBridge" );
			propertyBridgeObjectFieldReference = propertyBridgeObjectField.toReference();
			includedInPropertyBridgeFieldReference = propertyBridgeObjectField.field(
					"includedInSingleValuedPropertyBridge", f -> f.asString()
			)
					.toReference();
		}

		@Override
		public void write(DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context) {
			ContainingEntity castedBridgedElement = (ContainingEntity) bridgedElement;

			DocumentElement propertyBridgeObjectField = target.addObject( propertyBridgeObjectFieldReference );

			ContainedEntity containedSingle = castedBridgedElement == null ? null : castedBridgedElement.getContainedSingle();
			propertyBridgeObjectField.addValue(
					includedInPropertyBridgeFieldReference,
					containedSingle == null ? null : containedSingle.getIncludedInSingleValuedPropertyBridge()
			);
		}
	}

	public static class ContainingEntityMultiValuedPropertyBridge implements PropertyBridge {

		private IndexObjectFieldReference propertyBridgeObjectFieldReference;
		private IndexFieldReference<String> includedInPropertyBridgeFieldReference;

		@Override
		public void bind(PropertyBridgeBindingContext context) {
			context.getDependencies()
					.use( "containedSingle.includedInMultiValuedPropertyBridge" );

			IndexSchemaObjectField propertyBridgeObjectField = context.getIndexSchemaElement().objectField( "multiValuedPropertyBridge" );
			propertyBridgeObjectFieldReference = propertyBridgeObjectField.toReference();
			includedInPropertyBridgeFieldReference = propertyBridgeObjectField.field(
					"includedInMultiValuedPropertyBridge", f -> f.asString()
			)
					.toReference();
		}

		@Override
		public void write(DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context) {
			List<ContainingEntity> castedBridgedElement = (List<ContainingEntity>) bridgedElement;

			DocumentElement propertyBridgeObjectField = target.addObject( propertyBridgeObjectFieldReference );

			String concatenatedValue;
			if ( castedBridgedElement == null || castedBridgedElement.isEmpty() ) {
				concatenatedValue = null;
			}
			else {
				concatenatedValue = castedBridgedElement.stream()
						.map( ContainingEntity::getContainedSingle )
						.map( ContainedEntity::getIncludedInMultiValuedPropertyBridge )
						.collect( Collectors.joining( " " ) );
			}
			propertyBridgeObjectField.addValue(
					includedInPropertyBridgeFieldReference,
					concatenatedValue
			);
		}
	}
}
