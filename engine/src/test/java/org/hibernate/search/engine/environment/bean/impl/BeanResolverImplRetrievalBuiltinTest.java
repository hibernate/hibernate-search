/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.EngineSpiSettings;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;
import org.hibernate.search.engine.environment.classpath.spi.ServiceResolver;
import org.hibernate.search.util.common.SearchException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

public class BeanResolverImplRetrievalBuiltinTest {

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	@Mock
	private ClassResolver classResolverMock;

	@Mock
	private ServiceResolver serviceResolverMock;

	@Mock
	private BeanProvider beanManagerBeanProviderMock;

	@Mock
	private ConfigurationPropertySource configurationSourceMock;

	@Mock
	private BeanReference<InternalType1> type1InternalBeanFactoryMock;
	@Mock
	private BeanReference<InternalType2> type2InternalBeanFactoryMock;
	@Mock
	private BeanReference<InternalType3> type3InternalBean1FactoryMock;
	@Mock
	private BeanReference<InternalType3> type3InternalBean2FactoryMock;

	private BeanResolver beanResolver;

	@Before
	// Raw types are the only way to set the return value for a wildcard return type (Optional<?>)
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setup() {
		BeanConfigurer beanConfigurer1 = context -> {
			context.define( InternalType1.class, type1InternalBeanFactoryMock );
			context.define( InternalType2.class, "someName", type2InternalBeanFactoryMock );
			context.define( InternalType3.class, "someOtherName1", type3InternalBean1FactoryMock );
		};
		BeanConfigurer beanConfigurer2 = context -> {
			context.define( InternalType3.class, "someOtherName2", type3InternalBean2FactoryMock );
		};

		when( serviceResolverMock.loadJavaServices( BeanConfigurer.class ) )
				.thenReturn( Collections.singletonList( beanConfigurer1 ) );
		when( configurationSourceMock.withMask( any() ) )
				.thenCallRealMethod();
		when( configurationSourceMock.withFallback( any() ) )
				.thenCallRealMethod();
		when( configurationSourceMock.get( EngineSpiSettings.BEAN_CONFIGURERS ) )
				.thenReturn( (Optional) Optional.of( Collections.singletonList( beanConfigurer2 ) ) );
		beanResolver = BeanResolverImpl.create( classResolverMock, serviceResolverMock, beanManagerBeanProviderMock,
				configurationSourceMock );
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	public void resolve_matchingConfiguredBeans() {
		BeanHolder<InternalType1> type1BeanHolder = BeanHolder.of( new InternalType1() );
		BeanHolder<InternalType2> type2BeanHolder = BeanHolder.of( new InternalType2() );
		BeanHolder<InternalType3> type3BeanHolder1 = BeanHolder.of( new InternalType3() );
		BeanHolder<InternalType3> type3BeanHolder2 = BeanHolder.of( new InternalType3() );

		// resolve(Class)
		when( type1InternalBeanFactoryMock.resolve( any() ) ).thenReturn( type1BeanHolder );
		assertThat( beanResolver.resolve( InternalType1.class, BeanRetrieval.BUILTIN ) ).isSameAs( type1BeanHolder );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class) through BeanReference
		when( type1InternalBeanFactoryMock.resolve( any() ) ).thenReturn( type1BeanHolder );
		assertThat( beanResolver.resolve( BeanReference.of( InternalType1.class, BeanRetrieval.BUILTIN ) ) )
				.isSameAs( type1BeanHolder );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String)
		when( type2InternalBeanFactoryMock.resolve( any() ) ).thenReturn( type2BeanHolder );
		assertThat( beanResolver.resolve( InternalType2.class, "someName", BeanRetrieval.BUILTIN ) )
				.isSameAs( type2BeanHolder );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String) through BeanReference
		when( type2InternalBeanFactoryMock.resolve( any() ) ).thenReturn( type2BeanHolder );
		assertThat( beanResolver.resolve( BeanReference.of( InternalType2.class, "someName", BeanRetrieval.BUILTIN ) ) )
				.isSameAs( type2BeanHolder );
		verifyNoOtherInteractionsAndReset();

		// resolve(List<BeanReference>)
		when( type3InternalBean1FactoryMock.resolve( any() ) ).thenReturn( type3BeanHolder1 );
		when( type3InternalBean2FactoryMock.resolve( any() ) ).thenReturn( type3BeanHolder2 );
		BeanHolder<List<InternalType3>> beans = beanResolver.resolve(
				Arrays.asList( BeanReference.of( InternalType3.class, "someOtherName1", BeanRetrieval.BUILTIN ),
						BeanReference.of( InternalType3.class, "someOtherName2", BeanRetrieval.BUILTIN ) )
		);
		verifyNoOtherInteractionsAndReset();
		assertThat( beans.get() )
				.containsExactly( type3BeanHolder1.get(), type3BeanHolder2.get() );
	}

	@Test
	public void resolve_noMatch() {
		// resolve(Class)
		assertThatThrownBy( () -> beanResolver.resolve( InvalidType.class, BeanRetrieval.BUILTIN ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to resolve bean reference to type '" + InvalidType.class.getName() + "'",
						"No beans defined for type", "in Hibernate Search's internal registry" );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class) through BeanReference
		assertThatThrownBy( () -> beanResolver.resolve( BeanReference.of( InvalidType.class, BeanRetrieval.BUILTIN ) ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to resolve bean reference to type '" + InvalidType.class.getName() + "'",
						"No beans defined for type", "in Hibernate Search's internal registry" );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String)
		assertThatThrownBy( () -> beanResolver.resolve( InvalidType.class, "someName", BeanRetrieval.BUILTIN ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to resolve bean reference to type '" + InvalidType.class.getName()
						+ "' and name 'someName'",
						"No beans defined for type", "in Hibernate Search's internal registry" );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String) through BeanReference
		assertThatThrownBy(
				() -> beanResolver.resolve( BeanReference.of( InvalidType.class, "someName", BeanRetrieval.BUILTIN ) ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to resolve bean reference to type '" + InvalidType.class.getName()
						+ "' and name 'someName'",
						"No beans defined for type", "in Hibernate Search's internal registry" );
		verifyNoOtherInteractionsAndReset();

		// resolve(List<BeanReference>)
		assertThatThrownBy( () -> beanResolver.resolve(
				Arrays.asList( BeanReference.of( InvalidType.class, BeanRetrieval.BUILTIN ),
						BeanReference.of( Object.class, InvalidType.class.getName(), BeanRetrieval.BUILTIN ) )
		) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to resolve bean reference to type '" + InvalidType.class.getName() + "'",
						"No beans defined for type", "in Hibernate Search's internal registry" );
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	public void resolve_configuredBeanFactoryFailure() {
		RuntimeException beanFactoryFailure = new RuntimeException( "internal failure in factory" );

		// resolve(Class)
		when( type1InternalBeanFactoryMock.resolve( any() ) ).thenThrow( beanFactoryFailure );
		assertThatThrownBy( () -> beanResolver.resolve( InternalType1.class, BeanRetrieval.BUILTIN ) )
				.isSameAs( beanFactoryFailure );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String)
		when( type2InternalBeanFactoryMock.resolve( any() ) ).thenThrow( beanFactoryFailure );
		when( beanManagerBeanProviderMock.forTypeAndName( InternalType2.class, "someName" ) )
				.thenThrow( beanFactoryFailure );
		assertThatThrownBy( () -> beanResolver.resolve( InternalType2.class, "someName", BeanRetrieval.BUILTIN ) )
				.isSameAs( beanFactoryFailure );
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	public void resolve_ambiguousInternalBean() {
		// resolve(Class)
		assertThatThrownBy( () -> beanResolver.resolve( InternalType3.class, BeanRetrieval.BUILTIN ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to resolve bean reference to type '" + InternalType3.class.getName() + "'",
						"Ambiguous bean reference to type '" + InternalType3.class.getName() + "'",
						"multiple beans are explicitly defined for this type" );
		verifyNoOtherInteractionsAndReset();
	}

	private void verifyNoOtherInteractionsAndReset() {
		verifyNoMoreInteractions( classResolverMock, serviceResolverMock, beanManagerBeanProviderMock,
				configurationSourceMock,
				type1InternalBeanFactoryMock, type2InternalBeanFactoryMock,
				type3InternalBean1FactoryMock, type3InternalBean2FactoryMock );
		reset( classResolverMock, serviceResolverMock, beanManagerBeanProviderMock, configurationSourceMock,
				type1InternalBeanFactoryMock, type2InternalBeanFactoryMock,
				type3InternalBean1FactoryMock, type3InternalBean2FactoryMock );
	}

	private static class InternalType1 {
		// No public, no-arg constructor
		private InternalType1() {
		}
	}

	private static class InternalType2 {
		// No public, no-arg constructor
		private InternalType2() {
		}
	}

	private static class InternalType3 {
		// No public, no-arg constructor
		private InternalType3() {
		}
	}

	private static class InvalidType {
		// No public, no-arg constructor
		private InvalidType() {
		}
	}

}
