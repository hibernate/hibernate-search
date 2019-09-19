/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.propertybridge.ormcontext;

import org.hibernate.Session;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.mapper.orm.HibernateOrmExtension;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;

public class MyDataPropertyBinder implements PropertyBinder<MyDataPropertyBinding> {

	@Override
	public void bind(PropertyBindingContext context) {
		context.getDependencies()
				.useRootOnly();

		context.setBridge( new Bridge(
				context.getIndexSchemaElement().field( "myData", f -> f.asString() ).toReference()
		) );
	}

	//tag::include[]
	private static class Bridge implements PropertyBridge {

		private final IndexFieldReference<String> field;

		private Bridge(IndexFieldReference<String> field) {
			this.field = field;
		}

		@Override
		public void write(DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context) {
			Session session = context.extension( HibernateOrmExtension.get() ) // <1>
					.getSession(); // <2>
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
