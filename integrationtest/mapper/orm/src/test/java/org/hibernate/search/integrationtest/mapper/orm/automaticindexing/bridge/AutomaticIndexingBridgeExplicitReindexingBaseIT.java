/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.bridge;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.integrationtest.mapper.orm.automaticindexing.AbstractAutomaticIndexingBridgeIT;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

/**
 * Test automatic indexing based on Hibernate ORM entity events when
 * {@link TypeBridge}s or {@link PropertyBridge}s are involved
 * and rely on explicit reindexing declaration.
 */
@TestForIssue(jiraKey = "HSEARCH-3297")
public class AutomaticIndexingBridgeExplicitReindexingBaseIT extends AbstractAutomaticIndexingBridgeIT {

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
		return new ContainingEntityMultiValuedPropertyBridge.Binder();
	}

	public static class ContainingEntityTypeBridge implements TypeBridge {

		private final IndexObjectFieldReference typeBridgeObjectFieldReference;
		private final IndexFieldReference<String> directFieldReference;
		private final IndexObjectFieldReference childObjectFieldReference;
		private final IndexFieldReference<String> includedInTypeBridgeFieldReference;

		private ContainingEntityTypeBridge(TypeBindingContext context) {
			context.dependencies()
					.use( "directField" )
					// TODO HSEARCH-3567 this is currently necessary to handle removals, but it shouldn't be necessary
					.use( "association1.containedSingle" )
					.fromOtherEntity( ContainedEntity.class, "containingAsSingle.association1InverseSide" )
							.use( "includedInTypeBridge" );

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

		public static class Binder implements TypeBinder {
			@Override
			public void bind(TypeBindingContext context) {
				context.bridge( new ContainingEntityTypeBridge( context ) );
			}
		}
	}

	public static class ContainingEntitySingleValuedPropertyBridge implements PropertyBridge {

		private final IndexObjectFieldReference propertyBridgeObjectFieldReference;
		private final IndexFieldReference<String> includedInPropertyBridgeFieldReference;

		private ContainingEntitySingleValuedPropertyBridge(PropertyBindingContext context) {
			context.dependencies()
					// TODO HSEARCH-3567 this is currently necessary to handle removals, but it shouldn't be necessary
					.use( "containedSingle" )
					.fromOtherEntity( ContainedEntity.class, "containingAsSingle" )
							.use( "includedInSingleValuedPropertyBridge" );

			IndexSchemaObjectField propertyBridgeObjectField = context.indexSchemaElement().objectField( "singleValuedPropertyBridge" );
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

		public static class Binder implements PropertyBinder {
			@Override
			public void bind(PropertyBindingContext context) {
				context.bridge( new ContainingEntitySingleValuedPropertyBridge( context ) );
			}
		}
	}

	public static class ContainingEntityMultiValuedPropertyBridge implements PropertyBridge {

		private final IndexObjectFieldReference propertyBridgeObjectFieldReference;
		private final IndexFieldReference<String> includedInPropertyBridgeFieldReference;

		private ContainingEntityMultiValuedPropertyBridge(PropertyBindingContext context) {
			context.dependencies()
					.fromOtherEntity( ContainedEntity.class, "containingAsSingle" )
					.use( "includedInMultiValuedPropertyBridge" );

			IndexSchemaObjectField propertyBridgeObjectField = context.indexSchemaElement().objectField( "multiValuedPropertyBridge" );
			propertyBridgeObjectFieldReference = propertyBridgeObjectField.toReference();
			includedInPropertyBridgeFieldReference = propertyBridgeObjectField.field(
					"includedInMultiValuedPropertyBridge", f -> f.asString()
			)
					.toReference();
		}

		@Override
		@SuppressWarnings("unchecked")
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

		public static class Binder implements PropertyBinder {
			@Override
			public void bind(PropertyBindingContext context) {
				context.bridge( new ContainingEntityMultiValuedPropertyBridge( context ) );
			}
		}
	}
}
