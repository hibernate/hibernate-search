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

public class BeanResolverImplBaseTest {

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
	@Mock
	private BeanReference<RoleType> roleInternalBean1FactoryMock;
	@Mock
	private BeanReference<RoleType> roleInternalBean2FactoryMock;
	@Mock
	private BeanReference<RoleType> roleInternalBean3FactoryMock;
	@Mock
	private BeanReference<RoleType> roleInternalBean4FactoryMock;

	private BeanResolver beanResolver;

	@Before
	// Raw types are the only way to set the return value for a wildcard return type (Optional<?>)
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setup() {
		BeanConfigurer beanConfigurer1 = context -> {
			context.define( InternalType1.class, type1InternalBeanFactoryMock );
			context.define( InternalType2.class, "someName", type2InternalBeanFactoryMock );
			context.define( InternalType3.class, "someOtherName1", type3InternalBean1FactoryMock );

			context.define( RoleType.class, roleInternalBean1FactoryMock );
		};
		BeanConfigurer beanConfigurer2 = context -> {
			context.define( InternalType3.class, "someOtherName2", type3InternalBean2FactoryMock );

			context.define( RoleType.class, "someName", roleInternalBean2FactoryMock );
			context.define( RoleType.class, "someOtherName", roleInternalBean3FactoryMock );
			context.define( RoleType.class, roleInternalBean4FactoryMock );
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
		assertThat( beanResolver.resolve( InternalType1.class, BeanRetrieval.ANY ) ).isSameAs( type1BeanHolder );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class) through BeanReference
		when( type1InternalBeanFactoryMock.resolve( any() ) ).thenReturn( type1BeanHolder );
		assertThat( beanResolver.resolve( BeanReference.of( InternalType1.class ) ) ).isSameAs( type1BeanHolder );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String)
		when( type2InternalBeanFactoryMock.resolve( any() ) ).thenReturn( type2BeanHolder );
		assertThat( beanResolver.resolve( InternalType2.class, "someName", BeanRetrieval.ANY ) )
				.isSameAs( type2BeanHolder );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String) through BeanReference
		when( type2InternalBeanFactoryMock.resolve( any() ) ).thenReturn( type2BeanHolder );
		assertThat( beanResolver.resolve( BeanReference.of( InternalType2.class, "someName" ) ) )
				.isSameAs( type2BeanHolder );
		verifyNoOtherInteractionsAndReset();

		// resolve(List<BeanReference>)
		when( type3InternalBean1FactoryMock.resolve( any() ) ).thenReturn( type3BeanHolder1 );
		when( type3InternalBean2FactoryMock.resolve( any() ) ).thenReturn( type3BeanHolder2 );
		BeanHolder<List<InternalType3>> beans = beanResolver.resolve(
				Arrays.asList( BeanReference.of( InternalType3.class, "someOtherName1" ),
						BeanReference.of( InternalType3.class, "someOtherName2" ) )
		);
		verifyNoOtherInteractionsAndReset();
		assertThat( beans.get() )
				.containsExactly( type3BeanHolder1.get(), type3BeanHolder2.get() );
	}

	@Test
	public void resolve_matchingBeanManager_beanName() {
		BeanHolder<BeanManagerType1> type1BeanHolder = BeanHolder.of( new BeanManagerType1() );
		BeanHolder<BeanManagerType2> type2BeanHolder = BeanHolder.of( new BeanManagerType2() );
		BeanHolder<BeanManagerType3> type3BeanHolder1 = BeanHolder.of( new BeanManagerType3() );
		BeanHolder<BeanManagerType3> type3BeanHolder2 = BeanHolder.of( new BeanManagerType3() );

		// resolve(Class)
		when( beanManagerBeanProviderMock.forType( BeanManagerType1.class ) ).thenReturn( type1BeanHolder );
		assertThat( beanResolver.resolve( BeanManagerType1.class, BeanRetrieval.ANY ) ).isSameAs( type1BeanHolder );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class) through BeanReference
		when( beanManagerBeanProviderMock.forType( BeanManagerType1.class ) ).thenReturn( type1BeanHolder );
		assertThat( beanResolver.resolve( BeanReference.of( BeanManagerType1.class ) ) ).isSameAs( type1BeanHolder );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String)
		when( beanManagerBeanProviderMock.forTypeAndName( BeanManagerType2.class, "someName" ) )
				.thenReturn( type2BeanHolder );
		assertThat( beanResolver.resolve( BeanManagerType2.class, "someName", BeanRetrieval.ANY ) )
				.isSameAs( type2BeanHolder );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String) through BeanReference
		when( beanManagerBeanProviderMock.forTypeAndName( BeanManagerType2.class, "someName" ) )
				.thenReturn( type2BeanHolder );
		assertThat( beanResolver.resolve( BeanReference.of( BeanManagerType2.class, "someName" ) ) )
				.isSameAs( type2BeanHolder );
		verifyNoOtherInteractionsAndReset();

		// resolve(List<BeanReference>)
		when( beanManagerBeanProviderMock.forType( BeanManagerType3.class ) )
				.thenReturn( type3BeanHolder1 );
		when( beanManagerBeanProviderMock.forTypeAndName( BeanManagerType3.class, "someOtherName" ) )
				.thenReturn( type3BeanHolder2 );
		BeanHolder<List<BeanManagerType3>> beans = beanResolver.resolve(
				Arrays.asList( BeanReference.of( BeanManagerType3.class ),
						BeanReference.of( BeanManagerType3.class, "someOtherName" ) )
		);
		verifyNoOtherInteractionsAndReset();
		assertThat( beans.get() )
				.containsExactly( type3BeanHolder1.get(), type3BeanHolder2.get() );
	}

	@Test
	public void resolve_matchingBeanManager_className() {
		BeanHolder<BeanManagerType1> type1BeanHolder = BeanHolder.of( new BeanManagerType1() );
		BeanHolder<BeanManagerType2> type2BeanHolder = BeanHolder.of( new BeanManagerType2() );
		BeanHolder<BeanManagerType3> type3BeanHolder1 = BeanHolder.of( new BeanManagerType3() );
		BeanHolder<BeanManagerType3> type3BeanHolder2 = BeanHolder.of( new BeanManagerType3() );

		// resolve(Class)
		when( beanManagerBeanProviderMock.forType( BeanManagerType1.class ) ).thenReturn( type1BeanHolder );
		assertThat( beanResolver.resolve( BeanManagerType1.class, BeanRetrieval.ANY ) ).isSameAs( type1BeanHolder );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class) through BeanReference
		when( beanManagerBeanProviderMock.forType( BeanManagerType1.class ) ).thenReturn( type1BeanHolder );
		assertThat( beanResolver.resolve( BeanReference.of( BeanManagerType1.class ) ) )
				.isSameAs( type1BeanHolder );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String)
		when( beanManagerBeanProviderMock.forTypeAndName( Object.class, BeanManagerType2.class.getName() ) )
				.thenThrow( new BeanNotFoundException( "not found in beanManager" ) );
		doReturn( BeanManagerType2.class ).when( classResolverMock ).classForName( BeanManagerType2.class.getName() );
		when( beanManagerBeanProviderMock.forType( BeanManagerType2.class ) )
				.thenReturn( type2BeanHolder );
		assertThat( beanResolver.resolve( Object.class, BeanManagerType2.class.getName(), BeanRetrieval.ANY ) )
				.isSameAs( type2BeanHolder );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String) through BeanReference
		when( beanManagerBeanProviderMock.forTypeAndName( Object.class, BeanManagerType2.class.getName() ) )
				.thenThrow( new BeanNotFoundException( "not found in beanManager" ) );
		doReturn( BeanManagerType2.class ).when( classResolverMock ).classForName( BeanManagerType2.class.getName() );
		when( beanManagerBeanProviderMock.forType( BeanManagerType2.class ) )
				.thenReturn( type2BeanHolder );
		assertThat( beanResolver.resolve( BeanReference.of( Object.class, BeanManagerType2.class.getName() ) ) )
				.isSameAs( type2BeanHolder );
		verifyNoOtherInteractionsAndReset();

		// resolve(List<BeanReference>)
		when( beanManagerBeanProviderMock.forType( BeanManagerType3.class ) )
				.thenReturn( type3BeanHolder1 )
				.thenReturn( type3BeanHolder2 );
		when( beanManagerBeanProviderMock.forTypeAndName( BeanManagerType3.class, BeanManagerType3.class.getName() ) )
				.thenThrow( new BeanNotFoundException( "not found in beanManager" ) );
		doReturn( BeanManagerType3.class ).when( classResolverMock ).classForName( BeanManagerType3.class.getName() );
		BeanHolder<List<BeanManagerType3>> beans = beanResolver.resolve(
				Arrays.asList( BeanReference.of( BeanManagerType3.class ),
						BeanReference.of( BeanManagerType3.class, BeanManagerType3.class.getName() ) )
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
		assertThat( beanResolver.resolve( ReflectionType1.class, BeanRetrieval.ANY ) )
				.extracting( BeanHolder::get ).isInstanceOf( ReflectionType1.class );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class) through BeanReference
		when( beanManagerBeanProviderMock.forType( ReflectionType1.class ) )
				.thenThrow( beanManagerNotFoundException );
		assertThat( beanResolver.resolve( BeanReference.of( ReflectionType1.class ) ) )
				.extracting( BeanHolder::get ).isInstanceOf( ReflectionType1.class );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String)
		when( beanManagerBeanProviderMock.forTypeAndName( Object.class, ReflectionType2.class.getName() ) )
				.thenThrow( beanManagerNotFoundException );
		doReturn( ReflectionType2.class ).when( classResolverMock ).classForName( ReflectionType2.class.getName() );
		when( beanManagerBeanProviderMock.forType( ReflectionType2.class ) )
				.thenThrow( beanManagerNotFoundException );
		assertThat( beanResolver.resolve( Object.class, ReflectionType2.class.getName(), BeanRetrieval.ANY ) )
				.extracting( BeanHolder::get ).isInstanceOf( ReflectionType2.class );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String) through BeanReference
		when( beanManagerBeanProviderMock.forTypeAndName( Object.class, ReflectionType2.class.getName() ) )
				.thenThrow( beanManagerNotFoundException );
		doReturn( ReflectionType2.class ).when( classResolverMock ).classForName( ReflectionType2.class.getName() );
		when( beanManagerBeanProviderMock.forType( ReflectionType2.class ) )
				.thenThrow( beanManagerNotFoundException );
		assertThat( beanResolver.resolve( BeanReference.of( Object.class, ReflectionType2.class.getName() ) ) )
				.extracting( BeanHolder::get ).isInstanceOf( ReflectionType2.class );
		verifyNoOtherInteractionsAndReset();

		// resolve(List<BeanReference>)
		when( beanManagerBeanProviderMock.forType( ReflectionType3.class ) )
				.thenThrow( beanManagerNotFoundException );
		when( beanManagerBeanProviderMock.forTypeAndName( Object.class, ReflectionType3.class.getName() ) )
				.thenThrow( beanManagerNotFoundException );
		doReturn( ReflectionType3.class ).when( classResolverMock ).classForName( ReflectionType3.class.getName() );
		BeanHolder<List<Object>> beans = beanResolver.resolve(
				Arrays.asList( BeanReference.of( ReflectionType3.class ),
						BeanReference.of( Object.class, ReflectionType3.class.getName() ) )
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
		assertThatThrownBy( () -> beanResolver.resolve( InvalidType.class, BeanRetrieval.ANY ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to resolve bean reference to type '" + InvalidType.class.getName() + "'",
						"No beans defined for type", "in Hibernate Search's internal registry",
						beanManagerNotFoundException.getMessage(),
						"missing constructor" )
				.hasSuppressedException( beanManagerNotFoundException );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class) through BeanReference
		when( beanManagerBeanProviderMock.forType( InvalidType.class ) )
				.thenThrow( beanManagerNotFoundException );
		assertThatThrownBy( () -> beanResolver.resolve( BeanReference.of( InvalidType.class ) ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to resolve bean reference to type '" + InvalidType.class.getName() + "'",
						"No beans defined for type", "in Hibernate Search's internal registry",
						beanManagerNotFoundException.getMessage(),
						"missing constructor" )
				.hasSuppressedException( beanManagerNotFoundException );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String)
		when( beanManagerBeanProviderMock.forTypeAndName( InvalidType.class, "someName" ) )
				.thenThrow( beanManagerNotFoundException );
		when( classResolverMock.classForName( "someName" ) )
				.thenThrow( classNotFoundException );
		assertThatThrownBy( () -> beanResolver.resolve( InvalidType.class, "someName", BeanRetrieval.ANY ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to resolve bean reference to type '" + InvalidType.class.getName()
						+ "' and name 'someName'",
						"No beans defined for type", "in Hibernate Search's internal registry",
						beanManagerNotFoundException.getMessage(),
						classNotFoundException.getMessage() )
				.hasSuppressedException( beanManagerNotFoundException );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String) through BeanReference
		when( beanManagerBeanProviderMock.forTypeAndName( InvalidType.class, "someName" ) )
				.thenThrow( beanManagerNotFoundException );
		when( classResolverMock.classForName( "someName" ) )
				.thenThrow( classNotFoundException );
		assertThatThrownBy( () -> beanResolver.resolve( BeanReference.of( InvalidType.class, "someName" ) ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to resolve bean reference to type '" + InvalidType.class.getName()
						+ "' and name 'someName'",
						"No beans defined for type", "in Hibernate Search's internal registry",
						beanManagerNotFoundException.getMessage(),
						classNotFoundException.getMessage() )
				.hasSuppressedException( beanManagerNotFoundException );
		verifyNoOtherInteractionsAndReset();

		// resolve(List<BeanReference>)
		when( beanManagerBeanProviderMock.forType( InvalidType.class ) )
				.thenThrow( beanManagerNotFoundException );
		when( beanManagerBeanProviderMock.forTypeAndName( Object.class, InvalidType.class.getName() ) )
				.thenThrow( beanManagerNotFoundException );
		when( classResolverMock.classForName( InvalidType.class.getName() ) )
				.thenThrow( classNotFoundException );
		assertThatThrownBy( () -> beanResolver.resolve(
				Arrays.asList( BeanReference.of( InvalidType.class ),
						BeanReference.of( Object.class, InvalidType.class.getName() ) )
		) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to resolve bean reference to type '" + InvalidType.class.getName() + "'",
						"No beans defined for type", "in Hibernate Search's internal registry",
						beanManagerNotFoundException.getMessage(),
						"missing constructor" )
				.hasSuppressedException( beanManagerNotFoundException );
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	public void resolve_configuredBeanFactoryFailure() {
		RuntimeException beanFactoryFailure = new RuntimeException( "internal failure in factory" );

		// resolve(Class)
		when( type1InternalBeanFactoryMock.resolve( any() ) ).thenThrow( beanFactoryFailure );
		assertThatThrownBy( () -> beanResolver.resolve( InternalType1.class, BeanRetrieval.ANY ) )
				.isSameAs( beanFactoryFailure );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String)
		when( type2InternalBeanFactoryMock.resolve( any() ) ).thenThrow( beanFactoryFailure );
		when( beanManagerBeanProviderMock.forTypeAndName( InternalType2.class, "someName" ) )
				.thenThrow( beanFactoryFailure );
		assertThatThrownBy( () -> beanResolver.resolve( InternalType2.class, "someName", BeanRetrieval.ANY ) )
				.isSameAs( beanFactoryFailure );
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	public void resolve_beanManagerFailure() {
		RuntimeException beanManagerFailure = new RuntimeException( "internal failure in provider" );

		// resolve(Class)
		when( beanManagerBeanProviderMock.forType( BeanManagerType1.class ) ).thenThrow( beanManagerFailure );
		assertThatThrownBy( () -> beanResolver.resolve( BeanManagerType1.class, BeanRetrieval.ANY ) )
				.isSameAs( beanManagerFailure );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String)
		when( beanManagerBeanProviderMock.forTypeAndName( BeanManagerType2.class, "someName" ) )
				.thenThrow( beanManagerFailure );
		assertThatThrownBy( () -> beanResolver.resolve( BeanManagerType2.class, "someName", BeanRetrieval.ANY ) )
				.isSameAs( beanManagerFailure );
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	public void resolve_ambiguousInternalBean() {
		BeanNotFoundException beanManagerNotFoundException = new BeanNotFoundException( "cannot find from beanManager" );

		// resolve(Class)
		when( beanManagerBeanProviderMock.forType( InternalType3.class ) )
				.thenThrow( beanManagerNotFoundException );
		assertThatThrownBy( () -> beanResolver.resolve( InternalType3.class, BeanRetrieval.ANY ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to resolve bean reference to type '" + InternalType3.class.getName() + "'",
						"Ambiguous bean reference to type '" + InternalType3.class.getName() + "'",
						"multiple beans are explicitly defined for this type",
						beanManagerNotFoundException.getMessage(),
						"missing constructor" )
				.hasSuppressedException( beanManagerNotFoundException );
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	public void resolveRole() {
		BeanHolder<RoleType> beanHolder1 = BeanHolder.of( new InternalType3() );
		BeanHolder<RoleType> beanHolder2 = BeanHolder.of( new InternalType3() );
		BeanHolder<RoleType> beanHolder3 = BeanHolder.of( new InternalType3() );
		BeanHolder<RoleType> beanHolder4 = BeanHolder.of( new InternalType3() );

		// resolveRole
		when( roleInternalBean1FactoryMock.resolve( any() ) ).thenReturn( beanHolder1 );
		when( roleInternalBean2FactoryMock.resolve( any() ) ).thenReturn( beanHolder2 );
		when( roleInternalBean3FactoryMock.resolve( any() ) ).thenReturn( beanHolder3 );
		when( roleInternalBean4FactoryMock.resolve( any() ) ).thenReturn( beanHolder4 );
		List<BeanReference<RoleType>> beanReferencesWithRole = beanResolver.allConfiguredForRole( RoleType.class );
		BeanHolder<List<RoleType>> beansWithRole = beanResolver.resolve( beanReferencesWithRole );
		verifyNoOtherInteractionsAndReset();
		assertThat( beansWithRole.get() )
				.containsExactlyInAnyOrder( beanHolder1.get(), beanHolder2.get(), beanHolder3.get(), beanHolder4.get() );

		// Roles should ignore inheritance
		List<BeanReference<Object>> beanReferencesWithObjectRole = beanResolver.allConfiguredForRole( Object.class );
		BeanHolder<List<Object>> beansWithObjectRole = beanResolver.resolve( beanReferencesWithObjectRole );
		verifyNoOtherInteractionsAndReset();
		assertThat( beansWithObjectRole.get() ).isEmpty();

		// Unassigned roles should result in an empty list
		List<BeanReference<NonRoleType>> beanReferencesWithNonRole = beanResolver.allConfiguredForRole( NonRoleType.class );
		BeanHolder<List<NonRoleType>> beansWithNonRole = beanResolver.resolve( beanReferencesWithNonRole );
		verifyNoOtherInteractionsAndReset();
		assertThat( beansWithNonRole.get() ).isEmpty();
	}

	private void verifyNoOtherInteractionsAndReset() {
		verifyNoMoreInteractions( classResolverMock, serviceResolverMock, beanManagerBeanProviderMock,
				configurationSourceMock, configurationSourceMock,
				type1InternalBeanFactoryMock, type2InternalBeanFactoryMock,
				type3InternalBean1FactoryMock, type3InternalBean2FactoryMock,
				roleInternalBean1FactoryMock, roleInternalBean2FactoryMock,
				roleInternalBean3FactoryMock, roleInternalBean4FactoryMock );
		reset( classResolverMock, serviceResolverMock, beanManagerBeanProviderMock, configurationSourceMock,
				type1InternalBeanFactoryMock, type2InternalBeanFactoryMock,
				type3InternalBean1FactoryMock, type3InternalBean2FactoryMock,
				roleInternalBean1FactoryMock, roleInternalBean2FactoryMock,
				roleInternalBean3FactoryMock, roleInternalBean4FactoryMock );
	}

	private interface RoleType {
	}

	private interface NonRoleType {
	}

	private static class InternalType1 implements RoleType {
		// No public, no-arg constructor
		private InternalType1() {
		}
	}

	private static class InternalType2 implements RoleType {
		// No public, no-arg constructor
		private InternalType2() {
		}
	}

	private static class InternalType3 implements RoleType {
		// No public, no-arg constructor
		private InternalType3() {
		}
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
