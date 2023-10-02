/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.outboxpolling.testsupport.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.mapper.orm.HibernateOrmExtension;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.orm.outboxpolling.logging.impl.Log;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaDataNode;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.SimpleSessionFactoryBuilder;
import org.hibernate.search.util.impl.test.extension.StaticCounters;

import org.assertj.core.api.AbstractIntegerAssert;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ListAssert;

public class PerSessionFactoryIndexingCountHelper {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String FAKE_FIELD_NAME = "fakeField";
	private static final Class<?> FAKE_FIELD_TYPE = String.class;

	private final StaticCounters counters;
	private final List<StaticCountersKeys> counterKeys = new ArrayList<>();
	private final List<SessionFactory> sessionFactories = new ArrayList<>();

	public PerSessionFactoryIndexingCountHelper(StaticCounters counters) {
		this.counters = counters;
	}

	public void expectSchema(StubIndexSchemaDataNode.Builder builder) {
		builder.field( FAKE_FIELD_NAME, FAKE_FIELD_TYPE );
	}

	public OrmSetupHelper.SetupContext bind(OrmSetupHelper.SetupContext context) {
		StaticCountersKeys counterKeysForSessionFactory = new StaticCountersKeys();
		return context.withConfiguration(
				// For some reason the Eclipse compiler, ECJ, will throw a NullPointerException
				// if we use a lambda here.
				// This happens only in the -jakarta/-orm6 modules, though, and only on CI,
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
												.binder( new Binder( counterKeysForSessionFactory ) );
									}
								}
						);
					}
				},
				sessionFactory -> {
					counterKeys.add( counterKeysForSessionFactory );
					sessionFactories.add( sessionFactory );
				}
		);
	}

	public SessionFactory sessionFactory(int index) {
		return sessionFactories.get( index );
	}

	public IndexingCountsAccessor indexingCounts() {
		return new IndexingCountsAccessor( null );
	}

	public IndexingCountsAccessor indexingCounts(String tenantId) {
		return new IndexingCountsAccessor( tenantId );
	}

	public class IndexingCountsAccessor {
		private final String tenantId;

		private IndexingCountsAccessor(String tenantId) {
			this.tenantId = tenantId;
		}

		public AbstractIntegerAssert<?> assertAcrossAllSessionFactories() {
			int sum = 0;
			for ( StaticCountersKeys counterKeys : counterKeys ) {
				sum += counters.get( counterKeys.forTenantId( tenantId ) );
			}
			log.debugf( "Count of indexing operations across all session factories for tenant ID <%s>: %s", tenantId, sum );
			return assertThat( sum )
					.as( "Count of indexing operations across all session factories for tenant ID <%s>", tenantId );
		}

		public int forSessionFactory(int i) {
			int count = counters.get( counterKeys.get( i ).forTenantId( tenantId ) );
			log.debugf( "Count of indexing operations for session factory %d for tenant ID <%s>: %s",
					(Integer) i, tenantId, (Integer) count );
			return count;
		}

		public List<Integer> forEachSessionFactory() {
			List<Integer> counts = counterKeys.stream().map( keys -> keys.forTenantId( tenantId ) )
					.map( counters::get ).collect( Collectors.toList() );
			log.debugf( "Count of indexing operations for each session factory for tenant ID <%s>: %s", tenantId, counts );
			return counts;
		}

		public AbstractIntegerAssert<?> assertForSessionFactory(int i) {
			List<Integer> countForEach = forEachSessionFactory();
			return assertThat( countForEach )
					.element( i, InstanceOfAssertFactories.INTEGER )
					.as( "Count of indexing operations for session factory %d for tenant ID <%s> (count for each factory: %s)",
							i, tenantId, countForEach );
		}

		public ListAssert<Integer> assertForEachSessionFactory() {
			return assertThat( forEachSessionFactory() )
					.as( "Count of indexing operations for each session factory for tenant ID <%s>", tenantId );
		}

	}

	private static class StaticCountersKeys {
		private final Map<String, StaticCounters.Key> keys = new ConcurrentHashMap<>();

		Collection<StaticCounters.Key> all() {
			return keys.values();
		}

		StaticCounters.Key forTenantId(String tenantId) {
			return keys.computeIfAbsent( tenantId == null ? "" : tenantId, ignored -> StaticCounters.createKey() );
		}
	}

	public static class Binder implements TypeBinder {
		private final StaticCountersKeys counterKeys;

		public Binder(StaticCountersKeys counterKeys) {
			this.counterKeys = counterKeys;
		}

		@Override
		public void bind(TypeBindingContext context) {
			context.dependencies().useRootOnly();
			// We need to declare a field so that the bridge is not ignored.
			context.indexSchemaElement().field( FAKE_FIELD_NAME, f -> f.as( FAKE_FIELD_TYPE ) );

			context.bridge( new Bridge( counterKeys ) );
		}
	}

	public static class Bridge implements TypeBridge<Object> {
		private final StaticCountersKeys counterKeys;

		public Bridge(StaticCountersKeys counterKeys) {
			this.counterKeys = counterKeys;
		}

		@Override
		public void write(DocumentElement target, Object bridgedElement, TypeBridgeWriteContext context) {
			String tenantId = context.extension( HibernateOrmExtension.get() ).session().getTenantIdentifier();
			StaticCounters.get().increment( counterKeys.forTenantId( tenantId ) );
		}
	}
}
