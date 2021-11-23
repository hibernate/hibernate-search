/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.automaticindexing;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.logging.impl.Log;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaDataNode;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.SimpleSessionFactoryBuilder;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.assertj.core.api.AbstractIntegerAssert;
import org.assertj.core.api.InstanceOfAssertFactories;
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
				// For some reason the Eclipse compiler, ECJ, will throw a NullPointerException
				// if we use a lambda here.
				// This happens only in the -jakarta module, though, and only on CI,
				// and even then only on the main build, not on PRs!
				// I've been unable to reproduce the problem locally.
				new Consumer<SimpleSessionFactoryBuilder>() {
					@Override
					public void accept(SimpleSessionFactoryBuilder builder) {
						builder.setProperty(
								HibernateOrmMapperSettings.MAPPING_CONFIGURER,
								new HibernateOrmSearchMappingConfigurer() {
									@Override
									public void configure(HibernateOrmMappingConfigurationContext context) {
										context.programmaticMapping().type( Object.class )
												.binder( new Binder( counterKey ) );
									}
								}
						);
					}
				},
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

	public int indexingCountForSessionFactory(int i) {
		log.debugf( "Count of indexing operations for session factory %d: %s", i, i );
		return counters.get( counterKeys.get( i ) );
	}

	public List<Integer> indexingCountForEachSessionFactory() {
		List<Integer> counts = counterKeys.stream().map( counters::get ).collect( Collectors.toList() );
		log.debugf( "Count of indexing operations for each session factory: %s", counts );
		return counts;
	}

	public AbstractIntegerAssert<?> assertIndexingCountForSessionFactory(int i) {
		List<Integer> countForEach = indexingCountForEachSessionFactory();
		return assertThat( countForEach )
				.element( i, InstanceOfAssertFactories.INTEGER )
				.as( "Count of indexing operations for session factory %d (count for each factory: " + countForEach + ")", i );
	}

	public ListAssert<Integer> assertIndexingCountForEachSessionFactory() {
		return assertThat( indexingCountForEachSessionFactory() )
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
