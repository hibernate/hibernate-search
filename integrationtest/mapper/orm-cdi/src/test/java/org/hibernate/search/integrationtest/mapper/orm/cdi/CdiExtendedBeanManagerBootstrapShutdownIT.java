/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.cdi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.extension.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.ExceptionMatcherBuilder;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.extension.ExpectedLog4jLog;
import org.hibernate.search.util.impl.test.extension.StaticCounters;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.logging.log4j.Level;

/**
 * Check that Hibernate Search will be able to boot and shut down
 * when using an {@link org.hibernate.resource.beans.container.spi.ExtendedBeanManager} in Hibernate ORM.
 */
@TestForIssue(jiraKey = { "HSEARCH-3938" })
class CdiExtendedBeanManagerBootstrapShutdownIT {

	@RegisterExtension
	public final BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public final OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@RegisterExtension
	public final StaticCounters counters = StaticCounters.create();

	@RegisterExtension
	public final ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	private final StubExtendedBeanManager extendedBeanManager = new StubExtendedBeanManager();

	@AfterEach
	void tearDown() {
		extendedBeanManager.cleanUp();
	}

	@Test
	void successfulBoot() {
		List<BeanHolder<DependentBean>> retrievedBeans = new ArrayList<>();

		backendMock.onCreate( context -> {
			BeanHolder<DependentBean> retrievedBean = context.beanResolver().resolve( BeanReference.of( DependentBean.class ) );
			retrievedBeans.add( retrievedBean );
		} );
		backendMock.onStop( () -> {
			for ( BeanHolder<DependentBean> retrievedBean : retrievedBeans ) {
				retrievedBean.close();
			}
		} );

		try ( @SuppressWarnings("unused")
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withProperty( AvailableSettings.JAKARTA_CDI_BEAN_MANAGER, extendedBeanManager )
				.setup( IndexedEntity.class ) ) {
			// Hibernate Search should not have booted yet.
			backendMock.verifyExpectationsMet();
			assertThat( retrievedBeans ).isEmpty();

			// But once the bean manager is ready...
			backendMock.expectAnySchema( IndexedEntity.NAME );
			extendedBeanManager.simulateBoot( DependentBean.class );

			// Hibernate Search should have booted.
			backendMock.verifyExpectationsMet();
			assertThat( retrievedBeans )
					.hasSize( 1 )
					.element( 0 ).isNotNull()
					.extracting( BeanHolder::get ).isNotNull();
			assertThat( StaticCounters.get().get( DependentBean.KEYS.instantiate ) ).isEqualTo( 1 );
			assertThat( StaticCounters.get().get( DependentBean.KEYS.postConstruct ) ).isEqualTo( 1 );
			assertThat( StaticCounters.get().get( DependentBean.KEYS.preDestroy ) ).isEqualTo( 0 );

			// We should be able to use Hibernate Search
			try ( Session session = sessionFactory.openSession() ) {
				backendMock.expectSearchObjects( IndexedEntity.NAME, StubSearchWorkBehavior.empty() );
				Search.session( session ).search( IndexedEntity.class )
						.where( f -> f.matchAll() )
						.fetchAll();
				backendMock.verifyExpectationsMet();
			}

			// The bean manager shuts down.
			extendedBeanManager.simulateShutdown();

			// Hibernate Search should have shut down.
			assertThat( StaticCounters.get().get( DependentBean.KEYS.preDestroy ) ).isEqualTo( 1 );
		}
	}

	@Test
	void failedBoot() {
		List<BeanHolder<DependentBean>> retrievedBeans = new ArrayList<>();

		SearchException bootFailedException = new SearchException( "Simulated boot failure" );

		backendMock.onCreate( context -> {
			BeanHolder<DependentBean> retrievedBean = context.beanResolver().resolve( BeanReference.of( DependentBean.class ) );
			retrievedBeans.add( retrievedBean );
			throw bootFailedException;
		} );
		backendMock.onStop( () -> {
			for ( BeanHolder<DependentBean> retrievedBean : retrievedBeans ) {
				retrievedBean.close();
			}
		} );

		try ( @SuppressWarnings("unused")
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withProperty( AvailableSettings.JAKARTA_CDI_BEAN_MANAGER, extendedBeanManager )
				.setup( IndexedEntity.class ) ) {
			// Hibernate Search should not have booted yet.
			backendMock.verifyExpectationsMet();
			assertThat( retrievedBeans ).isEmpty();

			// But once the bean manager is ready...
			assertThatThrownBy( () -> extendedBeanManager.simulateBoot( DependentBean.class ) )
					// Hibernate Search should have attempted to boot, but failed.
					.satisfies( FailureReportUtils.hasFailureReport()
							.defaultBackendContext()
							.failure( bootFailedException.getMessage() ) );

			// Hibernate Search should have started to boot, then shut down.
			backendMock.verifyExpectationsMet();
			assertThat( retrievedBeans )
					.hasSize( 1 )
					.element( 0 ).isNotNull()
					.extracting( BeanHolder::get ).isNotNull();

			assertThat( StaticCounters.get().get( DependentBean.KEYS.instantiate ) ).isEqualTo( 1 );
			assertThat( StaticCounters.get().get( DependentBean.KEYS.postConstruct ) ).isEqualTo( 1 );
			assertThat( StaticCounters.get().get( DependentBean.KEYS.preDestroy ) ).isEqualTo( 0 );

			// Attempts to use Hibernate Search should throw an exception.
			try ( Session session = sessionFactory.openSession() ) {
				assertThatThrownBy( () -> Search.session( session ).search( IndexedEntity.class )
						.where( f -> f.matchAll() )
						.fetchAll() )
						.hasMessageContaining( "Hibernate Search was not initialized" );
			}

			// The bean manager's shutdown event should be ignored.
			extendedBeanManager.simulateShutdown();
			backendMock.verifyExpectationsMet();
			assertThat( StaticCounters.get().get( DependentBean.KEYS.preDestroy ) ).isEqualTo( 1 );
		}
	}

	@Test
	void cancelledBoot() {
		List<BeanHolder<DependentBean>> retrievedBeans = new ArrayList<>();

		backendMock.onCreate( context -> {
			BeanHolder<DependentBean> retrievedBean = context.beanResolver().resolve( BeanReference.of( DependentBean.class ) );
			retrievedBeans.add( retrievedBean );
		} );
		backendMock.onStop( () -> {
			for ( BeanHolder<DependentBean> retrievedBean : retrievedBeans ) {
				retrievedBean.close();
			}
		} );

		try ( @SuppressWarnings("unused")
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withProperty( AvailableSettings.JAKARTA_CDI_BEAN_MANAGER, extendedBeanManager )
				.setup( IndexedEntity.class ) ) {
			// Hibernate Search should not have booted yet.
			backendMock.verifyExpectationsMet();
			assertThat( retrievedBeans ).isEmpty();

			// The extended bean manager fails to boot and cancels Hibernate Search's boot...
			extendedBeanManager.simulateCancelledBoot( DependentBean.class );

			// Hibernate Search still should not have booted.
			backendMock.verifyExpectationsMet();
			assertThat( retrievedBeans ).isEmpty();

			// Attempts to use Hibernate Search should throw an exception.
			try ( Session session = sessionFactory.openSession() ) {
				assertThatThrownBy( () -> Search.session( session ).search( IndexedEntity.class )
						.where( f -> f.matchAll() )
						.fetchAll() )
						.hasMessageContaining( "Hibernate Search was not initialized" );
			}
		}
	}

	@Test
	void failedShutdown() {
		List<BeanHolder<DependentBean>> retrievedBeans = new ArrayList<>();

		SearchException bootFailedException = new SearchException( "Simulated shutdown failure" );

		backendMock.onCreate( context -> {
			BeanHolder<DependentBean> retrievedBean = context.beanResolver().resolve( BeanReference.of( DependentBean.class ) );
			retrievedBeans.add( retrievedBean );
		} );
		backendMock.onStop( () -> {
			for ( BeanHolder<DependentBean> retrievedBean : retrievedBeans ) {
				retrievedBean.close();
			}
			throw bootFailedException;
		} );

		try ( @SuppressWarnings("unused")
		SessionFactory sessionFactory = ormSetupHelper.start()
				.withProperty( AvailableSettings.JAKARTA_CDI_BEAN_MANAGER, extendedBeanManager )
				.setup( IndexedEntity.class ) ) {
			// Hibernate Search should not have booted yet.
			backendMock.verifyExpectationsMet();
			assertThat( retrievedBeans ).isEmpty();

			// But once the bean manager is ready...
			backendMock.expectAnySchema( IndexedEntity.NAME );
			extendedBeanManager.simulateBoot( DependentBean.class );

			// Hibernate Search should have booted.
			backendMock.verifyExpectationsMet();
			assertThat( retrievedBeans )
					.hasSize( 1 )
					.element( 0 ).isNotNull()
					.extracting( BeanHolder::get ).isNotNull();
			assertThat( StaticCounters.get().get( DependentBean.KEYS.instantiate ) ).isEqualTo( 1 );
			assertThat( StaticCounters.get().get( DependentBean.KEYS.postConstruct ) ).isEqualTo( 1 );
			assertThat( StaticCounters.get().get( DependentBean.KEYS.preDestroy ) ).isEqualTo( 0 );

			// We should be able to use Hibernate Search
			try ( Session session = sessionFactory.openSession() ) {
				backendMock.expectSearchObjects( IndexedEntity.NAME, StubSearchWorkBehavior.empty() );
				Search.session( session ).search( IndexedEntity.class )
						.where( f -> f.matchAll() )
						.fetchAll();
				backendMock.verifyExpectationsMet();
			}

			// The bean manager shuts down.
			// Hibernate Search should fail to shut down, and report the failure through the logs.
			logged.expectEvent( Level.ERROR,
					ExceptionMatcherBuilder.isException( SearchException.class )
							.withMessage( "Hibernate Search encountered failures during shutdown" )
							.withSuppressed( bootFailedException )
							.build(),
					"Unable to shut down Hibernate Search" );
			extendedBeanManager.simulateShutdown();

			assertThat( StaticCounters.get().get( DependentBean.KEYS.preDestroy ) ).isEqualTo( 1 );
		}
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed(index = IndexedEntity.NAME)
	public static final class IndexedEntity {

		static final String NAME = "indexed";

		@Id
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Dependent
	public static class DependentBean {
		public static final CounterKeys KEYS = new CounterKeys();

		protected DependentBean() {
			StaticCounters.get().increment( KEYS.instantiate );
		}

		@PostConstruct
		public void postConstruct() {
			StaticCounters.get().increment( KEYS.postConstruct );
		}

		@PreDestroy
		public void preDestroy() {
			StaticCounters.get().increment( KEYS.preDestroy );
		}
	}

	private static class CounterKeys {
		final StaticCounters.Key instantiate = StaticCounters.createKey();
		final StaticCounters.Key postConstruct = StaticCounters.createKey();
		final StaticCounters.Key preDestroy = StaticCounters.createKey();
	}
}
