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
import static org.mockito.Mockito.doReturn;
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

public class BeanResolverImplRetrievalConstructorTest {

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
	private BeanReference<ReflectionType1> type1InternalBeanFactoryMock;
	@Mock
	private BeanReference<ReflectionType2> type2InternalBeanFactoryMock;
	@Mock
	private BeanReference<ReflectionType3> type3InternalBean1FactoryMock;
	@Mock
	private BeanReference<ReflectionType3> type3InternalBean2FactoryMock;

	private BeanResolver beanResolver;

	@Before
	// Raw types are the only way to set the return value for a wildcard return type (Optional<?>)
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setup() {
		// Bean configurers should be ignored: we populate them with conflicting types to check that they are.
		BeanConfigurer beanConfigurer1 = context -> {
			context.define( ReflectionType1.class, type1InternalBeanFactoryMock );
			context.define( ReflectionType2.class, "someName", type2InternalBeanFactoryMock );
			context.define( ReflectionType3.class, "someOtherName", type3InternalBean1FactoryMock );
		};
		BeanConfigurer beanConfigurer2 = context -> {
			context.define( ReflectionType3.class, "someOtherName2", type3InternalBean2FactoryMock );
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
	public void resolve_matchingReflection() {
		// resolve(Class)
		assertThat( beanResolver.resolve( ReflectionType1.class, BeanRetrieval.CONSTRUCTOR ) )
				.extracting( BeanHolder::get ).isInstanceOf( ReflectionType1.class );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class) through BeanReference
		assertThat( beanResolver.resolve( BeanReference.of( ReflectionType1.class, BeanRetrieval.CONSTRUCTOR ) ) )
				.extracting( BeanHolder::get ).isInstanceOf( ReflectionType1.class );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String)
		doReturn( ReflectionType2.class ).when( classResolverMock ).classForName( ReflectionType2.class.getName() );
		assertThat( beanResolver.resolve( Object.class, ReflectionType2.class.getName(), BeanRetrieval.CONSTRUCTOR ) )
				.extracting( BeanHolder::get ).isInstanceOf( ReflectionType2.class );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String) through BeanReference
		doReturn( ReflectionType2.class ).when( classResolverMock ).classForName( ReflectionType2.class.getName() );
		assertThat( beanResolver
				.resolve( BeanReference.of( Object.class, ReflectionType2.class.getName(), BeanRetrieval.CONSTRUCTOR ) ) )
				.extracting( BeanHolder::get ).isInstanceOf( ReflectionType2.class );
		verifyNoOtherInteractionsAndReset();

		// resolve(List<BeanReference>)
		doReturn( ReflectionType3.class ).when( classResolverMock ).classForName( ReflectionType3.class.getName() );
		BeanHolder<List<Object>> beans = beanResolver.resolve(
				Arrays.asList( BeanReference.of( ReflectionType3.class, BeanRetrieval.CONSTRUCTOR ),
						BeanReference.of( Object.class, ReflectionType3.class.getName(), BeanRetrieval.CONSTRUCTOR ) )
		);
		verifyNoOtherInteractionsAndReset();
		assertThat( beans.get() )
				.hasSize( 2 )
				.allSatisfy( bean -> assertThat( bean ).isInstanceOf( ReflectionType3.class ) );
	}

	@Test
	public void resolve_noMatch() {
		RuntimeException classNotFoundException = new RuntimeException( "cannot find class" );

		// resolve(Class)
		assertThatThrownBy( () -> beanResolver.resolve( InvalidType.class, BeanRetrieval.CONSTRUCTOR ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to resolve bean reference to type '" + InvalidType.class.getName() + "'",
						"missing constructor" );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class) through BeanReference
		assertThatThrownBy( () -> beanResolver.resolve( BeanReference.of( InvalidType.class, BeanRetrieval.CONSTRUCTOR ) ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to resolve bean reference to type '" + InvalidType.class.getName() + "'",
						"missing constructor" );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String) => Class not found
		when( classResolverMock.classForName( "someName" ) )
				.thenThrow( classNotFoundException );
		assertThatThrownBy( () -> beanResolver.resolve( InvalidType.class, "someName", BeanRetrieval.CONSTRUCTOR ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to resolve bean reference to type '" + InvalidType.class.getName()
						+ "' and name 'someName'",
						classNotFoundException.getMessage() );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String) => Missing constructor
		doReturn( InvalidType.class ).when( classResolverMock ).classForName( InvalidType.class.getName() );
		assertThatThrownBy(
				() -> beanResolver.resolve( InvalidType.class, InvalidType.class.getName(), BeanRetrieval.CONSTRUCTOR ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to resolve bean reference to type '" + InvalidType.class.getName()
						+ "' and name '" + InvalidType.class.getName() + "'",
						"missing constructor" );
		verifyNoOtherInteractionsAndReset();

		// resolve(List<BeanReference>)
		assertThatThrownBy( () -> beanResolver.resolve(
				Arrays.asList( BeanReference.of( InvalidType.class, BeanRetrieval.CONSTRUCTOR ),
						BeanReference.of( Object.class, InvalidType.class.getName(), BeanRetrieval.CONSTRUCTOR ) )
		) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to resolve bean reference to type '" + InvalidType.class.getName() + "'",
						"missing constructor" );
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

	public static class ReflectionType1 {
	}

	public static class ReflectionType2 {
	}

	public static class ReflectionType3 {
	}

	private static class InvalidType {
		// No public, no-arg constructor
		private InvalidType() {
		}
	}

}
