/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.valuebridge.ormcontext;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.HibernateOrmExtension;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

//tag::include[]
public class MyDataValueBridge implements ValueBridge<MyData, String> {

	@Override
	public String toIndexedValue(MyData value, ValueBridgeToIndexedValueContext context) {
		SessionFactory sessionFactory = context.extension( HibernateOrmExtension.get() ) // <1>
				.sessionFactory(); // <2>
		// ... do something with the factory ...
		//end::include[]
		/*
		 * I don't know what to do with the session factory here,
		 * so I'm just going to extract data from it.
		 * This is silly, but at least it allows us to check the session factory was successfully retrieved.
		 */
		MyData dataFromSessionFactory = (MyData) sessionFactory.getProperties().get( "test.data.indexed" );
		return dataFromSessionFactory.name();
		//tag::include[]
	}

	@Override
	public MyData fromIndexedValue(String value, ValueBridgeFromIndexedValueContext context) {
		Session session = context.extension( HibernateOrmExtension.get() ) // <3>
				.session(); // <4>
		// ... do something with the session ...
		//end::include[]
		/*
		 * I don't know what to do with the session here,
		 * so I'm just going to extract data from it.
		 * This is silly, but at least it allows us to check the session was successfully retrieved.
		 */
		MyData dataFromSession = (MyData) session.getProperties().get( "test.data.projected" );
		return dataFromSession;
		//tag::include[]
	}
}
//end::include[]
