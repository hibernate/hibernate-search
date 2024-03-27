/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.typebridge.ormcontext;

import org.hibernate.Session;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.mapper.orm.HibernateOrmExtension;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;

public class MyEntityTypeBinder implements TypeBinder {

	@Override
	public void bind(TypeBindingContext context) {
		context.dependencies()
				.useRootOnly();

		context.bridge( new Bridge(
				context.indexSchemaElement().field( "myData", f -> f.asString() ).toReference()
		) );
	}

	//tag::include[]
	private static class Bridge implements TypeBridge<Object> {

		private final IndexFieldReference<String> field;

		private Bridge(IndexFieldReference<String> field) {
			this.field = field;
		}

		@Override
		public void write(DocumentElement target, Object bridgedElement, TypeBridgeWriteContext context) {
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
