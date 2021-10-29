/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing.coordination.databasepolling;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.coordination.databasepolling.logging.impl.Log;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaDataNode;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.assertj.core.api.AbstractIntegerAssert;
import org.assertj.core.api.ListAssert;

public class PerSessionFactoryIndexingCountHelper {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String FAKE_FIELD_NAME = "fakeField";
	private static final Class<?> FAKE_FIELD_TYPE = String.class;

	private final StaticCounters counters;
	private final List<StaticCounters.Key> counterKeys = new ArrayList<>();
	private final List<SessionFactory> sessionFactories = new ArrayList<>();

	public PerSessionFactoryIndexingCountHelper(StaticCounters counters) {
		this.counters = counters;
	}

	public void expectSchema(StubIndexSchemaDataNode.Builder builder) {
		builder.field( FAKE_FIELD_NAME, FAKE_FIELD_TYPE );
	}

	public OrmSetupHelper.SetupContext bind(OrmSetupHelper.SetupContext context) {
		StaticCounters.Key counterKey = StaticCounters.createKey();
		return context.withConfiguration(
				builder -> builder.setProperty( HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						new HibernateOrmSearchMappingConfigurer() {
							@Override
							public void configure(HibernateOrmMappingConfigurationContext context) {
								context.programmaticMapping().type( Object.class )
										.binder( new Binder( counterKey ) );
							}
						} ),
				sessionFactory -> {
					counterKeys.add( counterKey );
					sessionFactories.add( sessionFactory );
				}
		);
	}

	public SessionFactory sessionFactory(int index) {
		return sessionFactories.get( index );
	}

	public AbstractIntegerAssert<?> assertIndexingCountAcrossAllSessionFactories() {
		int sum = 0;
		for ( StaticCounters.Key counterKey : counterKeys ) {
			sum += counters.get( counterKey );
		}
		log.debugf( "Count of indexing operations across all session factories: %s", sum );
		return assertThat( sum )
				.as( "Count of indexing operations across all session factories" );
	}

	public AbstractIntegerAssert<?> assertIndexingCountForSessionFactory(int i) {
		int count = counters.get( counterKeys.get( i ) );
		log.debugf( "Count of indexing operations for session factory %d: %s", i, count );
		return assertThat( count )
				.as( "Count of indexing operations for session factory %d", i );
	}

	public ListAssert<Integer> assertIndexingCountForEachSessionFactory() {
		List<Integer> counts = counterKeys.stream().map( counters::get ).collect( Collectors.toList() );
		log.debugf( "Count of indexing operations for each session factory: %s", counts );
		return assertThat( counts )
				.as( "Count of indexing operations for each session factory" );
	}

	public static class Binder implements TypeBinder {
		private final StaticCounters.Key counterKey;

		public Binder(StaticCounters.Key counterKey) {
			this.counterKey = counterKey;
		}

		@Override
		public void bind(TypeBindingContext context) {
			context.dependencies().useRootOnly();
			// We need to declare a field so that the bridge is not ignored.
			context.indexSchemaElement().field( FAKE_FIELD_NAME, f -> f.as( FAKE_FIELD_TYPE ) );

			context.bridge( new Bridge( counterKey ) );
		}
	}

	public static class Bridge implements TypeBridge<Object> {
		private final StaticCounters.Key counterKey;

		public Bridge(StaticCounters.Key counterKey) {
			this.counterKey = counterKey;
		}

		@Override
		public void write(DocumentElement target, Object bridgedElement, TypeBridgeWriteContext context) {
			StaticCounters.get().increment( counterKey );
		}
	}
}
