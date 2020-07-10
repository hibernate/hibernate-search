/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.EngineSpiSettings;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurationContext;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.engine.environment.classpath.spi.ServiceResolver;
import org.hibernate.search.engine.testsupport.util.AbstractConfigurationPropertySourcePartialMock;
import org.hibernate.search.util.common.SearchException;

import org.junit.Test;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;

@SuppressWarnings({ "unchecked", "rawtypes" }) // Raw types are the only way to mock parameterized types with EasyMock
public class ConfiguredBeanResolverTest extends EasyMockSupport {

	private final ServiceResolver serviceResolverMock = createMock( ServiceResolver.class );
	private final BeanProvider beanProviderMock = createMock( BeanProvider.class );
	private final ConfigurationPropertySource configurationSourceMock =
			partialMockBuilder( AbstractConfigurationPropertySourcePartialMock.class ).mock();

	@Test
	public void resolve_withoutBeanConfigurer() {
		// Setup
		resetAll();
		expect( serviceResolverMock.loadJavaServices( BeanConfigurer.class ) )
				.andStubReturn( Collections.emptyList() );
		expect( configurationSourceMock.get( EngineSpiSettings.Radicals.BEAN_CONFIGURERS ) )
				.andStubReturn( Optional.empty() );
		replayAll();
		BeanResolver beanResolver =
				new ConfiguredBeanResolver( serviceResolverMock, beanProviderMock, configurationSourceMock );
		verifyAll();

		BeanHolder<Type1> type1BeanHolder = BeanHolder.of( new Type1() );
		BeanHolder<Type2> type2BeanHolder = BeanHolder.of( new Type2() );
		BeanHolder<Type3> type3BeanHolder1 = BeanHolder.of( new Type3() );
		BeanHolder<Type3> type3BeanHolder2 = BeanHolder.of( new Type3() );

		// resolve(Class)
		resetAll();
		expect( beanProviderMock.forType( Type1.class ) ).andReturn( type1BeanHolder );
		replayAll();
		assertThat( beanResolver.resolve( Type1.class ) ).isSameAs( type1BeanHolder );
		verifyAll();

		// resolve(Class) through BeanReference
		resetAll();
		expect( beanProviderMock.forType( Type1.class ) ).andReturn( type1BeanHolder );
		replayAll();
		assertThat( beanResolver.resolve( BeanReference.of( Type1.class ) ) ).isSameAs( type1BeanHolder );
		verifyAll();

		// resolve(Class, String)
		resetAll();
		expect( beanProviderMock.forTypeAndName( Type2.class, "someName" ) ).andReturn( type2BeanHolder );
		replayAll();
		assertThat( beanResolver.resolve( Type2.class, "someName" ) ).isSameAs( type2BeanHolder );
		verifyAll();

		// resolve(Class, String) through BeanReference
		resetAll();
		expect( beanProviderMock.forTypeAndName( Type2.class, "someName" ) ).andReturn( type2BeanHolder );
		replayAll();
		assertThat( beanResolver.resolve( BeanReference.of( Type2.class, "someName" ) ) ).isSameAs( type2BeanHolder );
		verifyAll();

		// resolve(List<BeanReference>)
		resetAll();
		expect( beanProviderMock.forType( Type3.class ) ).andReturn( type3BeanHolder1 );
		expect( beanProviderMock.forTypeAndName( Type3.class, "someOtherName" ) ).andReturn( type3BeanHolder2 );
		replayAll();
		BeanHolder<List<Type3>> beans = beanResolver.resolve(
				Arrays.asList( BeanReference.of( Type3.class ), BeanReference.of( Type3.class, "someOtherName" ) )
		);
		verifyAll();
		assertThat( beans.get() )
				.containsExactly( type3BeanHolder1.get(), type3BeanHolder2.get() );
	}

	@Test
	public void resolve_withBeanConfigurer() {
		// Setup
		BeanConfigurer beanConfigurer1Mock = createMock( BeanConfigurer.class );
		BeanConfigurer beanConfigurer2Mock = createMock( BeanConfigurer.class );

		BeanReference<Type1> beanReference1Mock = createMock( BeanReference.class );
		BeanReference<Type2> beanReference2Mock = createMock( BeanReference.class );
		BeanReference<Type3> beanReference3Mock = createMock( BeanReference.class );
		BeanReference<Type3> beanReference4Mock = createMock( BeanReference.class );

		resetAll();
		expect( serviceResolverMock.loadJavaServices( BeanConfigurer.class ) )
				.andReturn( Collections.singletonList( beanConfigurer1Mock ) );
		expect( configurationSourceMock.get( EngineSpiSettings.Radicals.BEAN_CONFIGURERS ) )
				.andReturn( (Optional) Optional.of( Collections.singletonList( beanConfigurer2Mock ) ) );
		beanConfigurer1Mock.configure( EasyMock.anyObject() );
		expectLastCall().andAnswer( () -> {
			BeanConfigurationContext context = (BeanConfigurationContext) EasyMock.getCurrentArguments()[0];
			context.define( Type1.class, beanReference1Mock );
			context.define( Type2.class, "someName", beanReference2Mock );
			context.define( Type3.class, beanReference3Mock );
			return null;
		} );
		beanConfigurer2Mock.configure( EasyMock.anyObject() );
		expectLastCall().andAnswer( () -> {
			BeanConfigurationContext context = (BeanConfigurationContext) EasyMock.getCurrentArguments()[0];
			context.define( Type3.class, "someOtherName", beanReference4Mock );
			return null;
		} );
		replayAll();
		BeanResolver beanResolver =
				new ConfiguredBeanResolver( serviceResolverMock, beanProviderMock, configurationSourceMock );
		verifyAll();

		BeanHolder<Type1> type1BeanHolder = BeanHolder.of( new Type1() );
		BeanHolder<Type2> type2BeanHolder = BeanHolder.of( new Type2() );
		BeanHolder<Type3> type3BeanHolder1 = BeanHolder.of( new Type3() );
		BeanHolder<Type3> type3BeanHolder2 = BeanHolder.of( new Type3() );

		// resolve(Class)
		resetAll();
		expect( beanProviderMock.forType( Type1.class ) )
				.andThrow( new SearchException( "cannot find Type1" ) );
		expect( beanReference1Mock.resolve( EasyMock.anyObject() ) ).andReturn( type1BeanHolder );
		replayAll();
		assertThat( beanResolver.resolve( Type1.class ) ).isSameAs( type1BeanHolder );
		verifyAll();

		// resolve(Class, String)
		resetAll();
		expect( beanProviderMock.forTypeAndName( Type2.class, "someName" ) )
				.andThrow( new SearchException( "cannot find Type2#someName" ) );
		expect( beanReference2Mock.resolve( EasyMock.anyObject() ) ).andReturn( type2BeanHolder );
		replayAll();
		assertThat( beanResolver.resolve( Type2.class, "someName" ) ).isSameAs( type2BeanHolder );
		verifyAll();

		// resolve(List<BeanReference>)
		resetAll();
		expect( beanProviderMock.forType( Type3.class ) )
				.andThrow( new SearchException( "cannot find Type3" ) );
		expect( beanProviderMock.forTypeAndName( Type3.class, "someOtherName" ) )
				.andThrow( new SearchException( "cannot find Type3#someOtherName" ) );
		expect( beanReference3Mock.resolve( EasyMock.anyObject() ) ).andReturn( type3BeanHolder1 );
		expect( beanReference4Mock.resolve( EasyMock.anyObject() ) ).andReturn( type3BeanHolder2 );
		replayAll();
		BeanHolder<List<Type3>> beans = beanResolver.resolve(
				Arrays.asList( BeanReference.of( Type3.class ), BeanReference.of( Type3.class, "someOtherName" ) )
		);
		verifyAll();
		assertThat( beans.get() )
				.containsExactly( type3BeanHolder1.get(), type3BeanHolder2.get() );
	}

	@Test
	public void resolveRole() {
		BeanConfigurer beanConfigurer1Mock = createMock( BeanConfigurer.class );
		BeanConfigurer beanConfigurer2Mock = createMock( BeanConfigurer.class );

		BeanReference<Type3> beanReference1Mock = createMock( BeanReference.class );
		BeanReference<Type3> beanReference2Mock = createMock( BeanReference.class );
		BeanReference<Type3> beanReference3Mock = createMock( BeanReference.class );

		resetAll();
		expect( serviceResolverMock.loadJavaServices( BeanConfigurer.class ) )
				.andReturn( Collections.singletonList( beanConfigurer1Mock ) );
		expect( configurationSourceMock.get( EngineSpiSettings.Radicals.BEAN_CONFIGURERS ) )
				.andReturn( (Optional) Optional.of( Collections.singletonList( beanConfigurer2Mock ) ) );
		beanConfigurer1Mock.configure( EasyMock.anyObject() );
		expectLastCall().andAnswer( () -> {
			BeanConfigurationContext context = (BeanConfigurationContext) EasyMock.getCurrentArguments()[0];
			context.define( Type3.class, beanReference1Mock );
			context.assignRole( RoleType.class, BeanReference.of( Type3.class ) );
			return null;
		} );
		beanConfigurer2Mock.configure( EasyMock.anyObject() );
		expectLastCall().andAnswer( () -> {
			BeanConfigurationContext context = (BeanConfigurationContext) EasyMock.getCurrentArguments()[0];
			context.define( Type3.class, "someName", beanReference2Mock );
			context.define( Type3.class, "someOtherName", beanReference3Mock );
			context.assignRole( RoleType.class, BeanReference.of( Type3.class, "someNameWithNoAssignedBeanFactory" ) );
			context.assignRole( RoleType.class, BeanReference.of( Type3.class, "someOtherName" ) );
			return null;
		} );
		replayAll();
		BeanResolver beanResolver =
				new ConfiguredBeanResolver( serviceResolverMock, beanProviderMock, configurationSourceMock );
		verifyAll();

		BeanHolder<Type3> type3BeanHolder1 = BeanHolder.of( new Type3() );
		BeanHolder<Type3> type3BeanHolder2 = BeanHolder.of( new Type3() );
		BeanHolder<Type3> type3BeanHolder3 = BeanHolder.of( new Type3() );

		// resolveRole
		resetAll();
		expect( beanProviderMock.forType( Type3.class ) )
				.andThrow( new SearchException( "cannot find Type3" ) );
		expect( beanReference1Mock.resolve( EasyMock.anyObject() ) ).andReturn( type3BeanHolder1 );
		expect( beanProviderMock.forTypeAndName( Type3.class, "someNameWithNoAssignedBeanFactory" ) )
				.andReturn( type3BeanHolder2 );
		expect( beanProviderMock.forTypeAndName( Type3.class, "someOtherName" ) )
				.andThrow( new SearchException( "cannot find Type3#someOtherName" ) );
		expect( beanReference3Mock.resolve( EasyMock.anyObject() ) ).andReturn( type3BeanHolder3 );
		replayAll();
		BeanHolder<List<RoleType>> beansWithRole = beanResolver.resolveRole( RoleType.class );
		verifyAll();
		assertThat( beansWithRole.get() )
				.containsExactlyInAnyOrder( type3BeanHolder1.get(), type3BeanHolder2.get(), type3BeanHolder3.get() );

		// Roles should ignore inheritance
		resetAll();
		replayAll();
		BeanHolder<List<Object>> beansWithObjectRole = beanResolver.resolveRole( Object.class );
		verifyAll();
		assertThat( beansWithObjectRole.get() ).isEmpty();

		// Unassigned roles should result in an empty list
		resetAll();
		replayAll();
		BeanHolder<List<NonRoleType>> beansWithNonRole = beanResolver.resolveRole( NonRoleType.class );
		verifyAll();
		assertThat( beansWithNonRole.get() ).isEmpty();

		// Assigned roles should not affect the behavior of resolve()
		resetAll();
		expect( beanProviderMock.forType( RoleType.class ) ).andReturn( (BeanHolder) type3BeanHolder3 );
		replayAll();
		assertThat( beanResolver.resolve( RoleType.class ) ).isSameAs( type3BeanHolder3 );
		verifyAll();
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