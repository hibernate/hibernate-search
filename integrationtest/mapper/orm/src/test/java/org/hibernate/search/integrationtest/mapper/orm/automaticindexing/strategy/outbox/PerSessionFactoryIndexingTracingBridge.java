/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.strategy.outbox;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.mapper.orm.HibernateOrmExtension;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

public class PerSessionFactoryIndexingTracingBridge implements TypeBridge<Object> {

	public static final String SESSION_FACTORY_COUNTER_KEY_PROPERTY = "HSEARCH_IT_sessionFactory_counterKey";
	public static final String FAKE_FIELD_NAME = "fakeField";
	public static final Class<?> FAKE_FIELD_TYPE = String.class;

	public static class Binder implements TypeBinder {
		@Override
		public void bind(TypeBindingContext context) {
			context.dependencies().useRootOnly();
			// We need to declare a field so that the bridge is not ignored.
			context.indexSchemaElement().field( FAKE_FIELD_NAME, f -> f.as( FAKE_FIELD_TYPE ) );

			context.bridge( new PerSessionFactoryIndexingTracingBridge() );
		}
	}

	@Override
	public void write(DocumentElement target, Object bridgedElement, TypeBridgeWriteContext context) {
		StaticCounters.Key counterKey = (StaticCounters.Key) context.extension( HibernateOrmExtension.get() )
				.session().getSessionFactory()
				.getProperties().get( SESSION_FACTORY_COUNTER_KEY_PROPERTY );
		StaticCounters.get().increment( counterKey );
	}
}
