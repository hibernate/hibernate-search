/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.propertybridge.ormcontext;

import org.hibernate.Session;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.mapper.orm.HibernateOrmExtension;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;

public class MyDataPropertyBinder implements PropertyBinder {

	@Override
	public void bind(PropertyBindingContext context) {
		context.dependencies()
				.useRootOnly();

		context.bridge( new Bridge(
				context.indexSchemaElement().field( "myData", f -> f.asString() ).toReference()
		) );
	}

	//tag::include[]
	private static class Bridge implements PropertyBridge<Object> {

		private final IndexFieldReference<String> field;

		private Bridge(IndexFieldReference<String> field) {
			this.field = field;
		}

		@Override
		public void write(DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context) {
			Session session = context.extension( HibernateOrmExtension.get() ) // <1>
					.session(); // <2>
			// ... do something with the session ...
			//end::include[]
			/*
			 * I don't know what to do with the session here,
			 * so I'm just going to extract data from it.
			 * This is silly, but at least it allows us to check the session was successfully retrieved.
			 */
			MyData dataFromSession = (MyData) session.getProperties().get( "test.data.indexed" );
			target.addValue( field, dataFromSession.name() );
			//tag::include[]
		}
	}
	//end::include[]
}
