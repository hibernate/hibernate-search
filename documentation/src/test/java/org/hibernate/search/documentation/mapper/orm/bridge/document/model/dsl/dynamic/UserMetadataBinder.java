/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.document.model.dsl.dynamic;

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
		context.getDependencies()
				/* ... (declaration of dependencies, not relevant) ... */
				//end::bind[]
				.useRootOnly();
		//tag::bind[]

		IndexSchemaElement schemaElement = context.getIndexSchemaElement();

		IndexSchemaObjectField userMetadataField =
				schemaElement.objectField( "userMetadata" ); // <1>

		userMetadataField.fieldTemplate( // <2>
				"userMetadataValueTemplate", // <3>
				f -> f.asString().analyzer( "english" ) // <4>
		); // <5>

		context.setBridge( new UserMetadataBridge( userMetadataField.toReference() ) ); // <6>
	}
	//end::bind[]

	//tag::write[]
	private static class UserMetadataBridge implements PropertyBridge {

		private final IndexObjectFieldReference userMetadataFieldReference;

		private UserMetadataBridge(IndexObjectFieldReference userMetadataFieldReference) {
			this.userMetadataFieldReference = userMetadataFieldReference;
		}

		@Override
		public void write(DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context) {
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
