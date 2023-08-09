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
import org.hibernate.search.engine.environment.bean.spi.BeanNotFoundException;
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

public class BeanResolverImplRetrievalClassTest {

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
	private BeanReference<BeanManagerType1> type1InternalBeanFactoryMock;
	@Mock
	private BeanReference<BeanManagerType2> type2InternalBeanFactoryMock;
	@Mock
	private BeanReference<BeanManagerType3> type3InternalBean1FactoryMock;
	@Mock
	private BeanReference<BeanManagerType3> type3InternalBean2FactoryMock;

	private BeanResolver beanResolver;

	@Before
	// Raw types are the only way to set the return value for a wildcard return type (Optional<?>)
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setup() {
		// Bean configurers should be ignored: we populate them with conflicting types to check that they are.
		BeanConfigurer beanConfigurer1 = context -> {
			context.define( BeanManagerType1.class, type1InternalBeanFactoryMock );
			context.define( BeanManagerType2.class, "someName", type2InternalBeanFactoryMock );
			context.define( BeanManagerType3.class, "someOtherName", type3InternalBean1FactoryMock );
		};
		BeanConfigurer beanConfigurer2 = context -> {
			context.define( BeanManagerType3.class, "someOtherName2", type3InternalBean2FactoryMock );
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
	public void resolve_matchingBeanManager() {
		BeanHolder<BeanManagerType1> type1BeanHolder = BeanHolder.of( new BeanManagerType1() );
		BeanHolder<BeanManagerType2> type2BeanHolder = BeanHolder.of( new BeanManagerType2() );
		BeanHolder<BeanManagerType3> type3BeanHolder1 = BeanHolder.of( new BeanManagerType3() );
		BeanHolder<BeanManagerType3> type3BeanHolder2 = BeanHolder.of( new BeanManagerType3() );

		// resolve(Class)
		when( beanManagerBeanProviderMock.forType( BeanManagerType1.class ) ).thenReturn( type1BeanHolder );
		assertThat( beanResolver.resolve( BeanManagerType1.class, BeanRetrieval.CLASS ) ).isSameAs( type1BeanHolder );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class) through BeanReference
		when( beanManagerBeanProviderMock.forType( BeanManagerType1.class ) ).thenReturn( type1BeanHolder );
		assertThat( beanResolver.resolve( BeanReference.of( BeanManagerType1.class, BeanRetrieval.CLASS ) ) )
				.isSameAs( type1BeanHolder );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String)
		doReturn( BeanManagerType2.class ).when( classResolverMock ).classForName( BeanManagerType2.class.getName() );
		when( beanManagerBeanProviderMock.forType( BeanManagerType2.class ) )
				.thenReturn( type2BeanHolder );
		assertThat( beanResolver.resolve( Object.class, BeanManagerType2.class.getName(), BeanRetrieval.CLASS ) )
				.isSameAs( type2BeanHolder );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String) through BeanReference
		doReturn( BeanManagerType2.class ).when( classResolverMock ).classForName( BeanManagerType2.class.getName() );
		when( beanManagerBeanProviderMock.forType( BeanManagerType2.class ) )
				.thenReturn( type2BeanHolder );
		assertThat( beanResolver
				.resolve( BeanReference.of( Object.class, BeanManagerType2.class.getName(), BeanRetrieval.CLASS ) ) )
				.isSameAs( type2BeanHolder );
		verifyNoOtherInteractionsAndReset();

		// resolve(List<BeanReference>)
		when( beanManagerBeanProviderMock.forType( BeanManagerType3.class ) )
				.thenReturn( type3BeanHolder1 )
				.thenReturn( type3BeanHolder2 );
		doReturn( BeanManagerType3.class ).when( classResolverMock ).classForName( BeanManagerType3.class.getName() );
		BeanHolder<List<BeanManagerType3>> beans = beanResolver.resolve(
				Arrays.asList( BeanReference.of( BeanManagerType3.class, BeanRetrieval.CLASS ),
						BeanReference.of( BeanManagerType3.class, BeanManagerType3.class.getName(), BeanRetrieval.CLASS ) )
		);
		verifyNoOtherInteractionsAndReset();
		assertThat( beans.get() )
				.containsExactly( type3BeanHolder1.get(), type3BeanHolder2.get() );
	}

	@Test
	public void resolve_matchingReflection() {
		BeanNotFoundException beanManagerNotFoundException = new BeanNotFoundException( "cannot find from beanManager" );

		// resolve(Class)
		when( beanManagerBeanProviderMock.forType( ReflectionType1.class ) )
				.thenThrow( beanManagerNotFoundException );
		assertThat( beanResolver.resolve( ReflectionType1.class, BeanRetrieval.CLASS ) )
				.extracting( BeanHolder::get ).isInstanceOf( ReflectionType1.class );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class) through BeanReference
		when( beanManagerBeanProviderMock.forType( ReflectionType1.class ) )
				.thenThrow( beanManagerNotFoundException );
		assertThat( beanResolver.resolve( BeanReference.of( ReflectionType1.class, BeanRetrieval.CLASS ) ) )
				.extracting( BeanHolder::get ).isInstanceOf( ReflectionType1.class );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String)
		doReturn( ReflectionType2.class ).when( classResolverMock ).classForName( ReflectionType2.class.getName() );
		when( beanManagerBeanProviderMock.forType( ReflectionType2.class ) )
				.thenThrow( beanManagerNotFoundException );
		assertThat( beanResolver.resolve( Object.class, ReflectionType2.class.getName(), BeanRetrieval.CLASS ) )
				.extracting( BeanHolder::get ).isInstanceOf( ReflectionType2.class );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String) through BeanReference
		doReturn( ReflectionType2.class ).when( classResolverMock ).classForName( ReflectionType2.class.getName() );
		when( beanManagerBeanProviderMock.forType( ReflectionType2.class ) )
				.thenThrow( beanManagerNotFoundException );
		assertThat(
				beanResolver.resolve( BeanReference.of( Object.class, ReflectionType2.class.getName(), BeanRetrieval.CLASS ) ) )
				.extracting( BeanHolder::get ).isInstanceOf( ReflectionType2.class );
		verifyNoOtherInteractionsAndReset();

		// resolve(List<BeanReference>)
		when( beanManagerBeanProviderMock.forType( ReflectionType3.class ) )
				.thenThrow( beanManagerNotFoundException );
		doReturn( ReflectionType3.class ).when( classResolverMock ).classForName( ReflectionType3.class.getName() );
		BeanHolder<List<Object>> beans = beanResolver.resolve(
				Arrays.asList( BeanReference.of( ReflectionType3.class, BeanRetrieval.CLASS ),
						BeanReference.of( Object.class, ReflectionType3.class.getName(), BeanRetrieval.CLASS ) )
		);
		verifyNoOtherInteractionsAndReset();
		assertThat( beans.get() )
				.hasSize( 2 )
				.allSatisfy( bean -> assertThat( bean ).isInstanceOf( ReflectionType3.class ) );
	}

	@Test
	public void resolve_noMatch() {
		BeanNotFoundException beanManagerNotFoundException = new BeanNotFoundException( "cannot find from beanManager" );
		RuntimeException classNotFoundException = new RuntimeException( "cannot find class" );

		// resolve(Class)
		when( beanManagerBeanProviderMock.forType( InvalidType.class ) )
				.thenThrow( beanManagerNotFoundException );
		assertThatThrownBy( () -> beanResolver.resolve( InvalidType.class, BeanRetrieval.CLASS ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to resolve bean reference to type '" + InvalidType.class.getName() + "'",
						beanManagerNotFoundException.getMessage(),
						"missing constructor" )
				.hasCause( beanManagerNotFoundException );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class) through BeanReference
		when( beanManagerBeanProviderMock.forType( InvalidType.class ) )
				.thenThrow( beanManagerNotFoundException );
		assertThatThrownBy( () -> beanResolver.resolve( BeanReference.of( InvalidType.class, BeanRetrieval.CLASS ) ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to resolve bean reference to type '" + InvalidType.class.getName() + "'",
						beanManagerNotFoundException.getMessage(),
						"missing constructor" )
				.hasCause( beanManagerNotFoundException );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String) => Class not found
		when( beanManagerBeanProviderMock.forTypeAndName( InvalidType.class, "someName" ) )
				.thenThrow( beanManagerNotFoundException );
		when( classResolverMock.classForName( "someName" ) )
				.thenThrow( classNotFoundException );
		assertThatThrownBy( () -> beanResolver.resolve( InvalidType.class, "someName", BeanRetrieval.CLASS ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to resolve bean reference to type '" + InvalidType.class.getName()
						+ "' and name 'someName'",
						classNotFoundException.getMessage() );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String) => Bean not not found
		doReturn( InvalidType.class ).when( classResolverMock ).classForName( InvalidType.class.getName() );
		when( beanManagerBeanProviderMock.forType( InvalidType.class ) )
				.thenThrow( beanManagerNotFoundException );
		assertThatThrownBy( () -> beanResolver.resolve( Object.class, InvalidType.class.getName(), BeanRetrieval.CLASS ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to resolve bean reference to type '" + Object.class.getName()
						+ "' and name '" + InvalidType.class.getName() + "'",
						beanManagerNotFoundException.getMessage(),
						"missing constructor" )
				.hasCause( beanManagerNotFoundException );
		verifyNoOtherInteractionsAndReset();

		// resolve(List<BeanReference>)
		when( beanManagerBeanProviderMock.forType( InvalidType.class ) )
				.thenThrow( beanManagerNotFoundException );
		doReturn( InvalidType.class ).when( classResolverMock ).classForName( InvalidType.class.getName() );
		when( beanManagerBeanProviderMock.forTypeAndName( Object.class, InvalidType.class.getName() ) )
				.thenThrow( beanManagerNotFoundException );
		assertThatThrownBy( () -> beanResolver.resolve(
				Arrays.asList( BeanReference.of( InvalidType.class, BeanRetrieval.CLASS ),
						BeanReference.of( Object.class, InvalidType.class.getName(), BeanRetrieval.CLASS ) )
		) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to resolve bean reference to type '" + InvalidType.class.getName() + "'",
						beanManagerNotFoundException.getMessage(),
						"missing constructor" )
				.hasCause( beanManagerNotFoundException );
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	public void resolve_beanManagerFailure() {
		RuntimeException beanManagerFailure = new RuntimeException( "internal failure in provider" );

		// resolve(Class)
		when( beanManagerBeanProviderMock.forType( BeanManagerType1.class ) ).thenThrow( beanManagerFailure );
		assertThatThrownBy( () -> beanResolver.resolve( BeanManagerType1.class, BeanRetrieval.CLASS ) )
				.isSameAs( beanManagerFailure );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String)
		doReturn( BeanManagerType2.class ).when( classResolverMock ).classForName( BeanManagerType2.class.getName() );
		when( beanManagerBeanProviderMock.forType( BeanManagerType2.class ) )
				.thenThrow( beanManagerFailure );
		assertThatThrownBy( () -> beanResolver.resolve( Object.class, BeanManagerType2.class.getName(), BeanRetrieval.CLASS ) )
				.isSameAs( beanManagerFailure );
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

	private static class BeanManagerType1 {
		// No public, no-arg constructor
		private BeanManagerType1() {
		}
	}

	private static class BeanManagerType2 {
		// No public, no-arg constructor
		private BeanManagerType2() {
		}
	}

	private static class BeanManagerType3 {
		// No public, no-arg constructor
		private BeanManagerType3() {
		}
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
