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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.EngineSpiSettings;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.engine.environment.classpath.spi.ServiceResolver;
import org.hibernate.search.util.common.SearchException;

import org.junit.Rule;
import org.junit.Test;

import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

@SuppressWarnings({ "unchecked", "rawtypes" }) // Raw types are the only way to mock parameterized types
public class ConfiguredBeanResolverTest {

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	@Mock
	private ServiceResolver serviceResolverMock;

	@Mock(answer = Answers.CALLS_REAL_METHODS)
	private BeanProvider beanProviderMock;

	@Mock(answer = Answers.CALLS_REAL_METHODS)
	private ConfigurationPropertySource configurationSourceMock;

	private final List<BeanReference<?>> beanReferenceMocks = new ArrayList<>();

	@Test
	public void resolve_withoutBeanConfigurer() {
		// Setup
		when( serviceResolverMock.loadJavaServices( BeanConfigurer.class ) )
				.thenReturn( Collections.emptyList() );
		when( configurationSourceMock.get( EngineSpiSettings.Radicals.BEAN_CONFIGURERS ) )
				.thenReturn( Optional.empty() );
		BeanResolver beanResolver =
				new ConfiguredBeanResolver( serviceResolverMock, beanProviderMock, configurationSourceMock );
		verifyNoOtherInteractionsAndReset();

		BeanHolder<Type1> type1BeanHolder = BeanHolder.of( new Type1() );
		BeanHolder<Type2> type2BeanHolder = BeanHolder.of( new Type2() );
		BeanHolder<Type3> type3BeanHolder1 = BeanHolder.of( new Type3() );
		BeanHolder<Type3> type3BeanHolder2 = BeanHolder.of( new Type3() );

		// resolve(Class)
		when( beanProviderMock.forType( Type1.class ) ).thenReturn( type1BeanHolder );
		assertThat( beanResolver.resolve( Type1.class ) ).isSameAs( type1BeanHolder );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class) through BeanReference
		when( beanProviderMock.forType( Type1.class ) ).thenReturn( type1BeanHolder );
		assertThat( beanResolver.resolve( BeanReference.of( Type1.class ) ) ).isSameAs( type1BeanHolder );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String)
		when( beanProviderMock.forTypeAndName( Type2.class, "someName" ) ).thenReturn( type2BeanHolder );
		assertThat( beanResolver.resolve( Type2.class, "someName" ) ).isSameAs( type2BeanHolder );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String) through BeanReference
		when( beanProviderMock.forTypeAndName( Type2.class, "someName" ) ).thenReturn( type2BeanHolder );
		assertThat( beanResolver.resolve( BeanReference.of( Type2.class, "someName" ) ) ).isSameAs( type2BeanHolder );
		verifyNoOtherInteractionsAndReset();

		// resolve(List<BeanReference>)
		when( beanProviderMock.forType( Type3.class ) ).thenReturn( type3BeanHolder1 );
		when( beanProviderMock.forTypeAndName( Type3.class, "someOtherName" ) ).thenReturn( type3BeanHolder2 );
		BeanHolder<List<Type3>> beans = beanResolver.resolve(
				Arrays.asList( BeanReference.of( Type3.class ), BeanReference.of( Type3.class, "someOtherName" ) )
		);
		verifyNoOtherInteractionsAndReset();
		assertThat( beans.get() )
				.containsExactly( type3BeanHolder1.get(), type3BeanHolder2.get() );
	}

	@Test
	public void resolve_withBeanConfigurer() {
		// Setup
		BeanReference<Type1> beanReference1Mock = beanReferenceMock();
		BeanReference<Type2> beanReference2Mock = beanReferenceMock();
		BeanReference<Type3> beanReference3Mock = beanReferenceMock();
		BeanReference<Type3> beanReference4Mock = beanReferenceMock();

		BeanConfigurer beanConfigurer1 = context -> {
			context.define( Type1.class, beanReference1Mock );
			context.define( Type2.class, "someName", beanReference2Mock );
			context.define( Type3.class, "someOtherName1", beanReference3Mock );
		};
		BeanConfigurer beanConfigurer2 = context -> {
			context.define( Type3.class, "someOtherName2", beanReference4Mock );
		};

		when( serviceResolverMock.loadJavaServices( BeanConfigurer.class ) )
				.thenReturn( Collections.singletonList( beanConfigurer1 ) );
		when( configurationSourceMock.get( EngineSpiSettings.Radicals.BEAN_CONFIGURERS ) )
				.thenReturn( (Optional) Optional.of( Collections.singletonList( beanConfigurer2 ) ) );
		BeanResolver beanResolver =
				new ConfiguredBeanResolver( serviceResolverMock, beanProviderMock, configurationSourceMock );
		verifyNoOtherInteractionsAndReset();

		BeanHolder<Type1> type1BeanHolder = BeanHolder.of( new Type1() );
		BeanHolder<Type2> type2BeanHolder = BeanHolder.of( new Type2() );
		BeanHolder<Type3> type3BeanHolder1 = BeanHolder.of( new Type3() );
		BeanHolder<Type3> type3BeanHolder2 = BeanHolder.of( new Type3() );

		// resolve(Class)
		when( beanProviderMock.forType( Type1.class ) )
				.thenThrow( new SearchException( "cannot find Type1" ) );
		when( beanReference1Mock.resolve( any() ) ).thenReturn( type1BeanHolder );
		assertThat( beanResolver.resolve( Type1.class ) ).isSameAs( type1BeanHolder );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String)
		when( beanProviderMock.forTypeAndName( Type2.class, "someName" ) )
				.thenThrow( new SearchException( "cannot find Type2#someName" ) );
		when( beanReference2Mock.resolve( any() ) ).thenReturn( type2BeanHolder );
		assertThat( beanResolver.resolve( Type2.class, "someName" ) ).isSameAs( type2BeanHolder );
		verifyNoOtherInteractionsAndReset();

		// resolve(List<BeanReference>)
		when( beanProviderMock.forTypeAndName( Type3.class, "someOtherName1" ) )
				.thenThrow( new SearchException( "cannot find Type3#someOtherName" ) );
		when( beanProviderMock.forTypeAndName( Type3.class, "someOtherName2" ) )
				.thenThrow( new SearchException( "cannot find Type3#someOtherName2" ) );
		when( beanReference3Mock.resolve( any() ) ).thenReturn( type3BeanHolder1 );
		when( beanReference4Mock.resolve( any() ) ).thenReturn( type3BeanHolder2 );
		BeanHolder<List<Type3>> beans = beanResolver.resolve(
				Arrays.asList( BeanReference.of( Type3.class, "someOtherName1" ), BeanReference.of( Type3.class, "someOtherName2" ) )
		);
		verifyNoOtherInteractionsAndReset();
		assertThat( beans.get() )
				.containsExactly( type3BeanHolder1.get(), type3BeanHolder2.get() );
	}

	@Test
	public void resolve_noBean() {
		// Setup
		when( serviceResolverMock.loadJavaServices( BeanConfigurer.class ) )
				.thenReturn( Collections.emptyList() );
		when( configurationSourceMock.get( EngineSpiSettings.Radicals.BEAN_CONFIGURERS ) )
				.thenReturn( (Optional) Optional.empty() );
		BeanResolver beanResolver =
				new ConfiguredBeanResolver( serviceResolverMock, beanProviderMock, configurationSourceMock );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class)
		SearchException providerType1NotFound = new SearchException( "cannot find Type1" );
		when( beanProviderMock.forType( Type1.class ) ).thenThrow( providerType1NotFound );
		assertThatThrownBy( () -> beanResolver.resolve( Type1.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Cannot resolve bean reference to type '" + Type1.class.getName() + "'",
						" cannot find Type1" )
				.hasCauseReference( providerType1NotFound );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String)
		SearchException providerType2NotFound = new SearchException( "cannot find Type2#someName" );
		when( beanProviderMock.forTypeAndName( Type2.class, "someName" ) )
				.thenThrow( providerType2NotFound );
		assertThatThrownBy( () -> beanResolver.resolve( Type2.class, "someName" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Cannot resolve bean reference to type '" + Type2.class.getName() + "' and name 'someName'",
						"cannot find Type2#someName" )
				.hasCauseReference( providerType2NotFound );
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	public void resolve_withBeanConfigurer_providerFailure() {
		// Setup
		BeanReference<Type1> beanReference1Mock = beanReferenceMock();
		BeanReference<Type1> beanReference2Mock = beanReferenceMock();

		BeanConfigurer beanConfigurer1 = context -> {
			context.define( Type1.class, beanReference1Mock );
		};
		BeanConfigurer beanConfigurer2 = context -> {
			context.define( Type1.class, "someName", beanReference2Mock );
		};

		when( serviceResolverMock.loadJavaServices( BeanConfigurer.class ) )
				.thenReturn( Collections.singletonList( beanConfigurer1 ) );
		when( configurationSourceMock.get( EngineSpiSettings.Radicals.BEAN_CONFIGURERS ) )
				.thenReturn( (Optional) Optional.of( Collections.singletonList( beanConfigurer2 ) ) );
		BeanResolver beanResolver =
				new ConfiguredBeanResolver( serviceResolverMock, beanProviderMock, configurationSourceMock );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class)
		RuntimeException providerFailure = new RuntimeException( "internal failure in provider" );
		when( beanProviderMock.forType( Type1.class ) ).thenThrow( providerFailure );
		assertThatThrownBy( () -> beanResolver.resolve( Type1.class ) ).isSameAs( providerFailure );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String)
		when( beanProviderMock.forTypeAndName( Type2.class, "someName" ) ).thenThrow( providerFailure );
		assertThatThrownBy( () -> beanResolver.resolve( Type2.class, "someName" ) )
				.isSameAs( providerFailure );
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	public void resolve_withBeanConfigurer_noProviderBean_configuredBeanFailure() {
		// Setup
		BeanReference<Type1> beanReference1Mock = beanReferenceMock();
		BeanReference<Type2> beanReference2Mock = beanReferenceMock();

		BeanConfigurer beanConfigurer1 = context -> {
			context.define( Type1.class, beanReference1Mock );
		};
		BeanConfigurer beanConfigurer2 = context -> {
			context.define( Type2.class, "someName", beanReference2Mock );
		};

		when( serviceResolverMock.loadJavaServices( BeanConfigurer.class ) )
				.thenReturn( Collections.singletonList( beanConfigurer1 ) );
		when( configurationSourceMock.get( EngineSpiSettings.Radicals.BEAN_CONFIGURERS ) )
				.thenReturn( (Optional) Optional.of( Collections.singletonList( beanConfigurer2 ) ) );
		BeanResolver beanResolver =
				new ConfiguredBeanResolver( serviceResolverMock, beanProviderMock, configurationSourceMock );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class)
		SearchException providerType1NotFound = new SearchException( "cannot find Type1" );
		RuntimeException configuredBeanType1Failed = new RuntimeException( "configured bean failed for Type1" );
		when( beanProviderMock.forType( Type1.class ) ).thenThrow( providerType1NotFound );
		when( beanReference1Mock.resolve( any() ) ).thenThrow( configuredBeanType1Failed );
		assertThatThrownBy( () -> beanResolver.resolve( Type1.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Cannot resolve bean reference to type '" + Type1.class.getName() + "'",
						"cannot find Type1", "configured bean failed for Type1" )
				.hasCauseReference( providerType1NotFound )
				.hasSuppressedException( configuredBeanType1Failed );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class, String)
		SearchException providerType2NotFound = new SearchException( "provider cannot find Type2#someName" );
		RuntimeException configuredBeanType2Failed = new RuntimeException( "configured bean failed for Type2#someName" );
		when( beanProviderMock.forTypeAndName( Type2.class, "someName" ) )
				.thenThrow( providerType2NotFound );
		when( beanReference2Mock.resolve( any() ) ).thenThrow( configuredBeanType2Failed );
		assertThatThrownBy( () -> beanResolver.resolve( Type2.class, "someName" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Cannot resolve bean reference to type '" + Type2.class.getName() + "' and name 'someName'",
						"provider cannot find Type2#someName",
						"configured bean failed for Type2#someName" )
				.hasCauseReference( providerType2NotFound )
				.hasSuppressedException( configuredBeanType2Failed );
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	public void resolve_withBeanConfigurer_multipleBeans() {
		// Setup
		BeanReference<Type1> beanReference1Mock = beanReferenceMock();
		BeanReference<Type1> beanReference2Mock = beanReferenceMock();

		BeanConfigurer beanConfigurer1 = context -> {
			context.define( Type1.class, beanReference1Mock );
		};
		BeanConfigurer beanConfigurer2 = context -> {
			context.define( Type1.class, "someName", beanReference2Mock );
		};

		when( serviceResolverMock.loadJavaServices( BeanConfigurer.class ) )
				.thenReturn( Collections.singletonList( beanConfigurer1 ) );
		when( configurationSourceMock.get( EngineSpiSettings.Radicals.BEAN_CONFIGURERS ) )
				.thenReturn( (Optional) Optional.of( Collections.singletonList( beanConfigurer2 ) ) );
		BeanResolver beanResolver =
				new ConfiguredBeanResolver( serviceResolverMock, beanProviderMock, configurationSourceMock );
		verifyNoOtherInteractionsAndReset();

		// resolve(Class)
		when( beanProviderMock.forType( Type1.class ) )
				.thenThrow( new SearchException( "cannot find Type1" ) );
		assertThatThrownBy( () -> beanResolver.resolve( Type1.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Cannot resolve bean reference to type '" + Type1.class.getName() + "'",
						"cannot find Type1",
						"Multiple beans registered for type '" + Type1.class.getName() + "'" );
		verifyNoOtherInteractionsAndReset();
	}

	@Test
	public void resolveRole() {
		BeanReference<RoleType> beanReference1Mock = beanReferenceMock();
		BeanReference<RoleType> beanReference2Mock = beanReferenceMock();
		BeanReference<RoleType> beanReference3Mock = beanReferenceMock();
		BeanReference<RoleType> beanReference4Mock = beanReferenceMock();

		BeanConfigurer beanConfigurer1 = context -> {
			context.define( RoleType.class, beanReference1Mock );
		};
		BeanConfigurer beanConfigurer2 = context -> {
			context.define( RoleType.class, "someName", beanReference2Mock );
			context.define( RoleType.class, "someOtherName", beanReference3Mock );
			context.define( RoleType.class, beanReference4Mock );
		};

		when( serviceResolverMock.loadJavaServices( BeanConfigurer.class ) )
				.thenReturn( Collections.singletonList( beanConfigurer1 ) );
		when( configurationSourceMock.get( EngineSpiSettings.Radicals.BEAN_CONFIGURERS ) )
				.thenReturn( (Optional) Optional.of( Collections.singletonList( beanConfigurer2 ) ) );
		BeanResolver beanResolver =
				new ConfiguredBeanResolver( serviceResolverMock, beanProviderMock, configurationSourceMock );
		verifyNoOtherInteractionsAndReset();

		BeanHolder<RoleType> beanHolder1 = BeanHolder.of( new Type3() );
		BeanHolder<RoleType> beanHolder2 = BeanHolder.of( new Type3() );
		BeanHolder<RoleType> beanHolder3 = BeanHolder.of( new Type3() );
		BeanHolder<RoleType> beanHolder4 = BeanHolder.of( new Type3() );

		// resolveRole
		when( beanReference1Mock.resolve( any() ) ).thenReturn( beanHolder1 );
		when( beanReference2Mock.resolve( any() ) ).thenReturn( beanHolder2 );
		when( beanReference3Mock.resolve( any() ) ).thenReturn( beanHolder3 );
		when( beanReference4Mock.resolve( any() ) ).thenReturn( beanHolder4 );
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
		verifyNoMoreInteractions( serviceResolverMock, beanProviderMock, configurationSourceMock );
		if ( !beanReferenceMocks.isEmpty() ) {
			verifyNoMoreInteractions( beanReferenceMocks.toArray() );
		}
		reset( serviceResolverMock, beanProviderMock, configurationSourceMock );
		if ( !beanReferenceMocks.isEmpty() ) {
			reset( beanReferenceMocks.toArray() );
		}
	}

	private <T> BeanReference<T> beanReferenceMock() {
		BeanReference<T> mock = mock( BeanReference.class );
		beanReferenceMocks.add( mock );
		return mock;
	}

	private static class Type1 {
	}

	private static class Type2 {
	}

	private interface RoleType {
	}

	private interface NonRoleType {
	}

	private static class Type3 implements RoleType {
	}

}