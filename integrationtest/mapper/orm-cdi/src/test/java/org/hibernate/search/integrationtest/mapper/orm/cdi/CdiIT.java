/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.cdi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.InstanceOfAssertFactories;

/**
 * Check that Hibernate Search will be able to get CDI beans from the BeanResolver thanks to the ORM integration,
 * when CDI is enabled in Hibernate ORM.
 * <p>
 * We test this by retrieving custom beans from the backend mock,
 * then checking the CDI-defined hooks (@PostConstruct and @PreDestroy) have been called
 * exactly as many times as expected.
 */
@TestForIssue(jiraKey = { "HSEARCH-1316", "HSEARCH-3171" })
@PortedFromSearch5(original = {
		"org.hibernate.search.test.integration.wildfly.cdi.CDIInjectionIT",
		"org.hibernate.search.test.integration.wildfly.cdi.CDIInjectionLifecycleEventsIT"
})
public class CdiIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Rule
	public StaticCounters counters = new StaticCounters();

	private SeContainer cdiContainer;

	@Before
	public void setup() {
		final SeContainerInitializer cdiInitializer = SeContainerInitializer.newInstance()
				.disableDiscovery()
				.addBeanClasses(
						InjectedBean.class,
						UnnamedSingletonBean.class,
						NamedSingletonBean.class,
						UnnamedDependentBean.class,
						NamedDependentBean.class
				);
		this.cdiContainer = cdiInitializer.initialize();
	}

	@After
	public void tearDown() {
		if ( cdiContainer != null ) {
			cdiContainer.close();
		}
	}

	@Test
	public void singleton_byType() {
		doTest(
				ExpectedScope.SINGLETON, UnnamedSingletonBean.KEYS,
				BeanReference.of( UnnamedSingletonBean.class )
		);
	}

	@Test
	public void singleton_byName() {
		doTest(
				ExpectedScope.SINGLETON, NamedSingletonBean.KEYS,
				BeanReference.of( InterfaceDefinedByMapper.class, NamedSingletonBean.NAME )
		);
	}

	@Test
	public void dependent_byType() {
		doTest(
				ExpectedScope.DEPENDENT, UnnamedDependentBean.KEYS,
				BeanReference.of( UnnamedDependentBean.class )
		);
	}

	@Test
	public void dependent_byName() {
		doTest(
				ExpectedScope.DEPENDENT, NamedDependentBean.KEYS,
				BeanReference.of( InterfaceDefinedByMapper.class, NamedDependentBean.NAME )
		);
	}

	private <T> void doTest(ExpectedScope expectedScope, CounterKeys counterKeys, BeanReference<T> reference) {
		List<BeanHolder<T>> retrievedBeans = new ArrayList<>();

		backendMock.onCreate( context -> {
			BeanHolder<T> retrievedBean1 = context.getBeanResolver().resolve( reference );
			retrievedBeans.add( retrievedBean1 );
			BeanHolder<T> retrievedBean2 = context.getBeanResolver().resolve( reference );
			retrievedBeans.add( retrievedBean2 );
		} );
		backendMock.onStop( () -> {
			for ( BeanHolder<T> retrievedBean : retrievedBeans ) {
				retrievedBean.close();
			}
		} );
		backendMock.expectAnySchema( IndexedEntity.NAME );

		int expectedInstances = ExpectedScope.SINGLETON.equals( expectedScope ) ? 1 : 2;

		try ( @SuppressWarnings("unused") SessionFactory sessionFactory = ormSetupHelper.start()
				.withProperty( AvailableSettings.CDI_BEAN_MANAGER, cdiContainer.getBeanManager() )
				.setup( IndexedEntity.class ) ) {
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
			case DEPENDENT:
				// Since we closed the BeanHolders when the backend was stopped,
				// @PreDestroy should now have been executed on each instance.
				assertThat( StaticCounters.get().get( counterKeys.preDestroy ) ).isEqualTo( expectedInstances );
				break;
			case SINGLETON:
			default:
				assertThat( StaticCounters.get().get( counterKeys.preDestroy ) ).isEqualTo( 0 );
				break;
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

	public interface InterfaceDefinedByMapper {

	}

	@Dependent
	public static class InjectedBean {
	}

	public abstract static class AbstractBeanBase
			implements InterfaceDefinedByMapper {
		@Inject
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

	@Dependent
	public static class UnnamedDependentBean extends AbstractBeanBase {
		public static final CounterKeys KEYS = new CounterKeys();

		@Override
		protected CounterKeys getCounterKeys() {
			return KEYS;
		}
	}

	@Dependent
	@Named(NamedDependentBean.NAME)
	public static class NamedDependentBean extends AbstractBeanBase {
		public static final String NAME = "NameOfDependentBean";
		public static final CounterKeys KEYS = new CounterKeys();

		@Override
		protected CounterKeys getCounterKeys() {
			return KEYS;
		}
	}

	@Singleton
	public static class UnnamedSingletonBean extends AbstractBeanBase {
		public static final CounterKeys KEYS = new CounterKeys();

		@Override
		protected CounterKeys getCounterKeys() {
			return KEYS;
		}
	}

	@Singleton
	@Named(NamedSingletonBean.NAME)
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
		DEPENDENT
	}
}
