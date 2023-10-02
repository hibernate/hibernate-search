/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.document.model.dsl.dynamic;

import java.util.Map;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;

//tag::bind[]
public class UserMetadataBinder implements PropertyBinder {

	@Override
	public void bind(PropertyBindingContext context) {
		context.dependencies()
				/* ... (declaration of dependencies, not relevant) ... */
				//end::bind[]
				.useRootOnly();
		//tag::bind[]

		IndexSchemaElement schemaElement = context.indexSchemaElement();

		IndexSchemaObjectField userMetadataField =
				schemaElement.objectField( "userMetadata" ); // <1>

		userMetadataField.fieldTemplate( // <2>
				"userMetadataValueTemplate", // <3>
				f -> f.asString().analyzer( "english" ) // <4>
		); // <5>

		context.bridge( Map.class, new UserMetadataBridge(
				userMetadataField.toReference() // <6>
		) );
	}
	//end::bind[]

	//tag::write[]
	@SuppressWarnings("rawtypes")
	private static class UserMetadataBridge implements PropertyBridge<Map> {

		private final IndexObjectFieldReference userMetadataFieldReference;

		private UserMetadataBridge(IndexObjectFieldReference userMetadataFieldReference) {
			this.userMetadataFieldReference = userMetadataFieldReference;
		}

		@Override
		public void write(DocumentElement target, Map bridgedElement, PropertyBridgeWriteContext context) {
			@SuppressWarnings("unchecked")
			Map<String, String> userMetadata = (Map<String, String>) bridgedElement;

			DocumentElement indexedUserMetadata = target.addObject( userMetadataFieldReference ); // <1>

			for ( Map.Entry<String, String> entry : userMetadata.entrySet() ) {
				String fieldName = entry.getKey();
				String fieldValue = entry.getValue();
				indexedUserMetadata.addValue( fieldName, fieldValue ); // <2>
			}
		}
	}
	//end::write[]
	//tag::bind[]
}
//end::bind[]
