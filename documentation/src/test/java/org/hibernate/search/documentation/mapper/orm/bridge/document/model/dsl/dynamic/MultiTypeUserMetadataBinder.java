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
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;

public class MultiTypeUserMetadataBinder implements PropertyBinder {

	//tag::bind[]
	@Override
	public void bind(PropertyBindingContext context) {
		context.getDependencies()
				/* ... (declaration of dependencies, not relevant) ... */
				//end::bind[]
				.useRootOnly();
		//tag::bind[]

		IndexSchemaElement schemaElement = context.getIndexSchemaElement();

		IndexSchemaObjectField userMetadataField =
				schemaElement.objectField( "multiTypeUserMetadata" ); // <1>

		userMetadataField.fieldTemplate( // <2>
				"userMetadataValueTemplate_int", // <3>
				f -> f.asInteger().sortable( Sortable.YES ) // <4>
		)
				.matchingPathGlob( "*_int" ); // <5>

		userMetadataField.fieldTemplate( // <6>
				"userMetadataValueTemplate_default",
				f -> f.asString().analyzer( "english" )
		);

		context.setBridge( new Bridge( userMetadataField.toReference() ) );
	}
	//end::bind[]

	private static class Bridge implements PropertyBridge {

		private final IndexObjectFieldReference userMetadataFieldReference;

		private Bridge(IndexObjectFieldReference userMetadataFieldReference) {
			this.userMetadataFieldReference = userMetadataFieldReference;
		}

		//tag::write[]
		@Override
		public void write(DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context) {
			Map<String, Object> userMetadata = (Map<String, Object>) bridgedElement;

			DocumentElement indexedUserMetadata = target.addObject( userMetadataFieldReference ); // <1>

			for ( Map.Entry<String, Object> entry : userMetadata.entrySet() ) {
				String fieldName = entry.getKey();
				Object fieldValue = entry.getValue();
				indexedUserMetadata.addValue( fieldName, fieldValue ); // <2>
			}
		}
		//end::write[]
	}
}
