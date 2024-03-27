/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.spring.beanresolution;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.BackendMappingHandle;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.DatabaseContainer;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.HibernateOrmMappingHandle;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.extension.StaticCounters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.stereotype.Component;

/**
 * Check that Hibernate Search will be able to get Spring beans from the BeanResolver thanks to the ORM integration.
 * <p>
 * We test this by retrieving custom beans from the backend mock,
 * then checking the Spring-defined hooks (@PostConstruct and @PreDestroy) have been called
 * exactly as many times as expected.
 */
@TestForIssue(jiraKey = { "HSEARCH-1316", "HSEARCH-3171" })
class SpringBeanResolutionIT {

	@Configuration
	@EnableAutoConfiguration
	@EntityScan
	@ComponentScan(basePackageClasses = SpringBeanResolutionIT.class)
	public static class SpringConfig {
		private final CompletableFuture<BackendMappingHandle> mappingHandlePromise = new CompletableFuture<>();

		@Bean
		public HibernatePropertiesCustomizer backendMockPropertiesCustomizer(ApplicationContext applicationContext) {
			BackendMock backendMock = applicationContext.getEnvironment()
					.getProperty( "test.backendMock", BackendMock.class );
			return hibernateProperties -> hibernateProperties.put( "hibernate.search.backend.type",
					backendMock.factory( mappingHandlePromise ) );
		}

		@EventListener(ApplicationReadyEvent.class)
		public void initBackendMappingHandle(ApplicationReadyEvent event) {
			mappingHandlePromise
					.complete( new HibernateOrmMappingHandle( event.getApplicationContext().getBean( SessionFactory.class ) ) );
		}
	}

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StaticCounters counters = StaticCounters.create();

	@Test
	void singleton_byType() {
		doTest(
				ExpectedScope.SINGLETON, UnnamedSingletonBean.KEYS,
				BeanReference.of( UnnamedSingletonBean.class )
		);
	}

	@Test
	void singleton_byName() {
		doTest(
				ExpectedScope.SINGLETON, NamedSingletonBean.KEYS,
				BeanReference.of( InterfaceDefinedByMapper.class, NamedSingletonBean.NAME )
		);
	}

	@Test
	void prototype_byType() {
		doTest(
				ExpectedScope.PROTOTYPE, UnnamedPrototypeBean.KEYS,
				BeanReference.of( UnnamedPrototypeBean.class )
		);
	}

	@Test
	void prototype_byName() {
		doTest(
				ExpectedScope.PROTOTYPE, NamedPrototypeBean.KEYS,
				BeanReference.of( InterfaceDefinedByMapper.class, NamedPrototypeBean.NAME )
		);
	}

	private <T> void doTest(ExpectedScope expectedScope, CounterKeys counterKeys, BeanReference<T> reference) {
		List<BeanHolder<T>> retrievedBeans = new ArrayList<>();

		backendMock.onCreate( context -> {
			BeanHolder<T> retrievedBean1 = context.beanResolver().resolve( reference );
			retrievedBeans.add( retrievedBean1 );
			BeanHolder<T> retrievedBean2 = context.beanResolver().resolve( reference );
			retrievedBeans.add( retrievedBean2 );
		} );
		backendMock.onStop( () -> {
			for ( BeanHolder<T> retrievedBean : retrievedBeans ) {
				retrievedBean.close();
			}
		} );
		backendMock.expectAnySchema( IndexedEntity.NAME );

		int expectedInstances = ExpectedScope.SINGLETON.equals( expectedScope ) ? 1 : 2;

		try ( @SuppressWarnings("unused")
		ConfigurableApplicationContext applicationContext = startApplication() ) {
			applicationContext.getBean( EntityManagerFactory.class ).getMetamodel();
			backendMock.verifyExpectationsMet();

			assertThat( retrievedBeans )
					.hasSize( 2 )
					.doesNotContainNull()
					.extracting( BeanHolder::get )
					.doesNotContainNull()
					// All created beans should have been injected
					.allSatisfy( bean -> assertThat( bean )
							.asInstanceOf( InstanceOfAssertFactories.type( AbstractBeanBase.class ) )
							.extracting( "injectedBean" )
							.isNotNull()
					);

			assertThat( StaticCounters.get().get( counterKeys.instantiate ) ).isEqualTo( expectedInstances );
			assertThat( StaticCounters.get().get( counterKeys.postConstruct ) ).isEqualTo( expectedInstances );
			assertThat( StaticCounters.get().get( counterKeys.preDestroy ) ).isEqualTo( 0 );
		}

		switch ( expectedScope ) {
			case PROTOTYPE:
				// Spring does not call @PreDestroy on prototype beans
				// See https://docs.spring.io/spring-framework/docs/5.3.1/reference/html/core.html#beans-factory-scopes-prototype
				assertThat( StaticCounters.get().get( counterKeys.preDestroy ) ).isEqualTo( 0 );
				break;
			case SINGLETON:
			default:
				assertThat( StaticCounters.get().get( counterKeys.preDestroy ) ).isEqualTo( 1 );
				break;
		}
	}

	private ConfigurableApplicationContext startApplication() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put( "test.backendMock", backendMock );

		DatabaseContainer.Configuration configuration = DatabaseContainer.configuration();
		MockEnvironment environment = new MockEnvironment();
		environment.withProperty( "JDBC_DRIVER", configuration.driver() );
		environment.withProperty( "JDBC_URL", configuration.url() );
		environment.withProperty( "JDBC_USERNAME", configuration.user() );
		environment.withProperty( "JDBC_PASSWORD", configuration.pass() );

		return new SpringApplicationBuilder( SpringConfig.class )
				.web( WebApplicationType.NONE )
				.environment( environment )
				.properties( properties )
				.run();
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

	public interface InterfaceDefinedByMapper {

	}

	@Component
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public static class InjectedBean {
	}

	public abstract static class AbstractBeanBase
			implements InterfaceDefinedByMapper {
		@Autowired
		private InjectedBean injectedBean;

		protected AbstractBeanBase() {
			StaticCounters.get().increment( getCounterKeys().instantiate );
		}

		@PostConstruct
		public void postConstruct() {
			StaticCounters.get().increment( getCounterKeys().postConstruct );
		}

		@PreDestroy
		public void preDestroy() {
			StaticCounters.get().increment( getCounterKeys().preDestroy );
		}

		protected abstract CounterKeys getCounterKeys();
	}

	@Component
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public static class UnnamedPrototypeBean extends AbstractBeanBase {
		public static final CounterKeys KEYS = new CounterKeys();

		@Override
		protected CounterKeys getCounterKeys() {
			return KEYS;
		}
	}

	@Component(NamedPrototypeBean.NAME)
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public static class NamedPrototypeBean extends AbstractBeanBase {
		public static final String NAME = "NameOfDependentBean";
		public static final CounterKeys KEYS = new CounterKeys();

		@Override
		protected CounterKeys getCounterKeys() {
			return KEYS;
		}
	}

	@Component
	public static class UnnamedSingletonBean extends AbstractBeanBase {
		public static final CounterKeys KEYS = new CounterKeys();

		@Override
		protected CounterKeys getCounterKeys() {
			return KEYS;
		}
	}

	@Component(NamedSingletonBean.NAME)
	public static class NamedSingletonBean extends AbstractBeanBase {
		public static final String NAME = "NameOfSingletonBean";
		public static final CounterKeys KEYS = new CounterKeys();

		@Override
		protected CounterKeys getCounterKeys() {
			return KEYS;
		}
	}

	private static class CounterKeys {
		final StaticCounters.Key instantiate = StaticCounters.createKey();
		final StaticCounters.Key postConstruct = StaticCounters.createKey();
		final StaticCounters.Key preDestroy = StaticCounters.createKey();
	}

	private enum ExpectedScope {
		SINGLETON,
		PROTOTYPE
	}
}
