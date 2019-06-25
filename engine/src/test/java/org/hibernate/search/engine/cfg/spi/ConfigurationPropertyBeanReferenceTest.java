/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.testsupport.util.AbstractBeanResolverPartialMock;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Test;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;

@SuppressWarnings({"unchecked", "rawtypes"}) // Raw types are the only way to mock parameterized types with EasyMock
public class ConfigurationPropertyBeanReferenceTest extends EasyMockSupport {

	private final ConfigurationPropertySource sourceMock = createMock( ConfigurationPropertySource.class );
	private final BeanResolver beanResolverMock = partialMockBuilder( AbstractBeanResolverPartialMock.class ).createMock();

	@Test
	public void withDefault() {
		String key = "withDefault";
		ConfigurationProperty<BeanReference<? extends StubBean>> property =
				ConfigurationProperty.forKey( key ).asBeanReference( StubBean.class )
						.withDefault( BeanReference.of( StubBean.class, "theDefault" ) )
						.build();

		BeanHolder<StubBeanImpl1> expected = BeanHolder.of( new StubBeanImpl1() );
		BeanHolder<StubBean> expectedAsStubBean = BeanHolder.of( new StubBeanImpl1() );
		BeanHolder<? extends StubBean> result;

		// No value
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( Optional.empty() );
		EasyMock.expect( beanResolverMock.resolve( StubBean.class, "theDefault" ) )
				.andReturn( expectedAsStubBean );
		replayAll();
		result = property.get( sourceMock ).resolve( beanResolverMock );
		verifyAll();
		assertThat( result ).isEqualTo( expectedAsStubBean );

		// String value
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( "name" ) );
		EasyMock.expect( beanResolverMock.resolve( StubBean.class, "name" ) )
				.andReturn( expectedAsStubBean );
		replayAll();
		result = property.get( sourceMock ).resolve( beanResolverMock );
		verifyAll();
		assertThat( result ).isEqualTo( expectedAsStubBean );

		// Class value
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( BeanReference.of( StubBeanImpl1.class ) ) );
		EasyMock.expect( beanResolverMock.resolve( StubBeanImpl1.class ) )
				.andReturn( expected );
		replayAll();
		result = property.get( sourceMock ).resolve( beanResolverMock );
		verifyAll();
		assertThat( result ).isEqualTo( expected );

		// BeanReference value
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( BeanReference.of( StubBeanImpl1.class, "name" ) ) );
		EasyMock.expect( beanResolverMock.resolve( StubBeanImpl1.class, "name" ) )
				.andReturn( expected );
		replayAll();
		result = property.get( sourceMock ).resolve( beanResolverMock );
		verifyAll();
		assertThat( result ).isEqualTo( expected );
	}

	@Test
	public void withoutDefault() {
		String key = "withDefault";
		ConfigurationProperty<Optional<BeanReference<? extends StubBean>>> property =
				ConfigurationProperty.forKey( key ).asBeanReference( StubBean.class )
						.build();

		BeanHolder<StubBeanImpl1> expected = BeanHolder.of( new StubBeanImpl1() );
		BeanHolder<StubBean> expectedAsStubBean = BeanHolder.of( new StubBeanImpl1() );
		Optional<BeanReference<? extends StubBean>> reference;
		BeanHolder<? extends StubBean> result;

		// No value
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( Optional.empty() );
		replayAll();
		reference = property.get( sourceMock );
		verifyAll();
		assertThat( reference ).isEmpty();

		// String value
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( "name" ) );
		EasyMock.expect( beanResolverMock.resolve( StubBean.class, "name" ) )
				.andReturn( expectedAsStubBean );
		replayAll();
		reference = property.get( sourceMock );
		assertThat( reference ).isNotEmpty();
		result = reference.get().resolve( beanResolverMock );
		verifyAll();
		assertThat( result ).isEqualTo( expectedAsStubBean );

		// Class value
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( BeanReference.of( StubBeanImpl1.class ) ) );
		EasyMock.expect( beanResolverMock.resolve( StubBeanImpl1.class ) )
				.andReturn( expected );
		replayAll();
		reference = property.get( sourceMock );
		assertThat( reference ).isNotEmpty();
		result = reference.get().resolve( beanResolverMock );
		verifyAll();
		assertThat( result ).isEqualTo( expected );

		// BeanReference value
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( BeanReference.of( StubBeanImpl1.class, "name" ) ) );
		EasyMock.expect( beanResolverMock.resolve( StubBeanImpl1.class, "name" ) )
				.andReturn( expected );
		replayAll();
		reference = property.get( sourceMock );
		assertThat( reference ).isNotEmpty();
		result = reference.get().resolve( beanResolverMock );
		verifyAll();
		assertThat( result ).isEqualTo( expected );
	}

	@Test
	public void multiValued() {
		String key = "multiValued";
		OptionalConfigurationProperty<List<BeanReference<? extends StubBean>>> property =
				ConfigurationProperty.forKey( key ).asBeanReference( StubBean.class )
						.multivalued( Pattern.compile( " " ) )
						.build();

		BeanHolder<StubBeanImpl1> expected1 = BeanHolder.of( new StubBeanImpl1() );
		BeanHolder<StubBean> expected1AsStubBean = BeanHolder.of( new StubBeanImpl1() );
		BeanHolder<StubBeanImpl2> expected2 = BeanHolder.of( new StubBeanImpl2() );
		BeanHolder<StubBean> expected2AsStubBean = BeanHolder.of( new StubBeanImpl2() );
		Optional<BeanHolder<List<StubBean>>> result;

		// No value
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( Optional.empty() );
		replayAll();
		result = property.getAndMap( sourceMock, beanResolverMock::resolve );
		verifyAll();
		assertThat( result ).isEmpty();

		// String value - one
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( "name" ) );
		EasyMock.expect( beanResolverMock.resolve( StubBean.class, "name" ) )
				.andReturn( expected1AsStubBean );
		replayAll();
		result = property.getAndMap( sourceMock, beanResolverMock::resolve );
		verifyAll();
		assertThat( result ).isNotEmpty();
		assertThat( result.get().get() ).containsExactly( expected1AsStubBean.get() );

		// String value - multiple
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( "name1 name2" ) );
		EasyMock.expect( beanResolverMock.resolve( StubBean.class, "name1" ) )
				.andReturn( expected1AsStubBean );
		EasyMock.expect( beanResolverMock.resolve( StubBean.class, "name2" ) )
				.andReturn( expected2AsStubBean );
		replayAll();
		result = property.getAndMap( sourceMock, beanResolverMock::resolve );
		verifyAll();
		assertThat( result ).isNotEmpty();
		assertThat( result.get().get() ).containsExactly( expected1AsStubBean.get(), expected2AsStubBean.get() );

		// Class value - one
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of(
				createCollection( StubBeanImpl1.class )
		) );
		EasyMock.expect( beanResolverMock.resolve( StubBeanImpl1.class ) )
				.andReturn( expected1 );
		replayAll();
		result = property.getAndMap( sourceMock, beanResolverMock::resolve );
		verifyAll();
		assertThat( result ).isNotEmpty();
		assertThat( result.get().get() ).containsExactly( expected1.get() );

		// Class value - multiple
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of(
				createCollection( StubBeanImpl1.class, StubBeanImpl2.class )
		) );
		EasyMock.expect( beanResolverMock.resolve( StubBeanImpl1.class ) )
				.andReturn( expected1 );
		EasyMock.expect( beanResolverMock.resolve( StubBeanImpl2.class ) )
				.andReturn( expected2 );
		replayAll();
		result = property.getAndMap( sourceMock, beanResolverMock::resolve );
		verifyAll();
		assertThat( result ).isNotEmpty();
		assertThat( result.get().get() ).containsExactly( expected1.get(), expected2.get() );

		// BeanReference value - one
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of(
				createCollection( BeanReference.of( StubBeanImpl1.class, "name" ) )
		) );
		EasyMock.expect( beanResolverMock.resolve( StubBeanImpl1.class, "name" ) )
				.andReturn( expected1 );
		replayAll();
		result = property.getAndMap( sourceMock, beanResolverMock::resolve );
		verifyAll();
		assertThat( result ).isNotEmpty();
		assertThat( result.get().get() ).containsExactly( expected1.get() );

		// BeanReference value - multiple
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of(
				createCollection(
						BeanReference.of( StubBeanImpl1.class, "name1" ),
						BeanReference.of( StubBeanImpl2.class, "name2" )
				)
		) );
		EasyMock.expect( beanResolverMock.resolve( StubBeanImpl1.class, "name1" ) )
				.andReturn( expected1 );
		EasyMock.expect( beanResolverMock.resolve( StubBeanImpl2.class, "name2" ) )
				.andReturn( expected2 );
		replayAll();
		result = property.getAndMap( sourceMock, beanResolverMock::resolve );
		verifyAll();
		assertThat( result ).isNotEmpty();
		assertThat( result.get().get() ).containsExactly( expected1.get(), expected2.get() );
	}

	@Test
	public void invalidType() {
		String key = "invalidType";
		String resolvedKey = "some.prefix." + key;
		ConfigurationProperty<Optional<BeanReference<? extends StubBean>>> property =
				ConfigurationProperty.forKey( key ).asBeanReference( StubBean.class )
						.build();

		InvalidType invalidTypeValue = new InvalidType();
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( invalidTypeValue ) );
		EasyMock.expect( sourceMock.resolve( key ) ).andReturn( Optional.of( resolvedKey ) );
		replayAll();
		SubTest.expectException( () -> property.get( sourceMock ) )
				.assertThrown()
				.hasMessageContaining(
						"Unable to convert configuration property '" + resolvedKey
								+ "' with value '" + invalidTypeValue + "':"
				)
				.hasMessageContaining(
						"Invalid BeanReference value: expected an instance of '" + StubBean.class.getName()
								+ "', BeanReference, String or Class"
				);
		verifyAll();
	}

	@Test
	public void invalidReference() {
		String key = "invalidReference";
		String resolvedKey = "some.prefix." + key;
		OptionalConfigurationProperty<BeanReference<? extends StubBean>> property =
				ConfigurationProperty.forKey( key ).asBeanReference( StubBean.class )
						.build();

		String propertyValue = "name";
		SimulatedFailure simulatedFailure = new SimulatedFailure();

		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( propertyValue ) );
		EasyMock.expect( sourceMock.resolve( key ) ).andReturn( Optional.of( resolvedKey ) );
		EasyMock.expect( beanResolverMock.resolve( StubBean.class, "name" ) )
				.andThrow( simulatedFailure );
		replayAll();
		SubTest.expectException(
				() -> {
					property.getAndMap( sourceMock, beanResolverMock::resolve );
				}
		)
				.assertThrown()
				.hasCause( simulatedFailure )
				.hasMessageContaining(
						"Unable to convert configuration property '" + resolvedKey
								+ "' with value '" + propertyValue + "':"
				);
		verifyAll();
	}

	@Test
	public void multiValued_invalidReference() {
		String key = "multiValued_invalidReference";
		String resolvedKey = "some.prefix." + key;
		OptionalConfigurationProperty<List<BeanReference<? extends StubBean>>> property =
				ConfigurationProperty.forKey( key ).asBeanReference( StubBean.class )
						.multivalued( Pattern.compile( " " ) )
						.build();

		BeanHolder<StubBean> bean1Mock = createMock( BeanHolder.class );

		String propertyValue = "name1 name2";
		SimulatedFailure simulatedFailure = new SimulatedFailure();

		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( propertyValue ) );
		EasyMock.expect( sourceMock.resolve( key ) ).andReturn( Optional.of( resolvedKey ) );
		EasyMock.expect( beanResolverMock.resolve( StubBean.class, "name1" ) )
				.andReturn( bean1Mock );
		EasyMock.expect( beanResolverMock.resolve( StubBean.class, "name2" ) )
				.andThrow( simulatedFailure );
		bean1Mock.close(); // Expect the first bean holder to be closed
		replayAll();
		SubTest.expectException(
				() -> {
					property.getAndMap( sourceMock, beanResolverMock::resolve );
				}
		)
				.assertThrown()
				.hasCause( simulatedFailure )
				.hasMessageContaining(
						"Unable to convert configuration property '" + resolvedKey
								+ "' with value '" + propertyValue + "':"
				);
		verifyAll();
	}

	@SafeVarargs
	private static <T> Collection<T> createCollection(T... values) {
		// Don't create a List, that would be too easy.
		Collection<T> collection = new LinkedHashSet<>();
		Collections.addAll( collection, values );
		return collection;
	}

	private interface StubBean {
	}

	private class StubBeanImpl1 implements StubBean {
	}

	private class StubBeanImpl2 implements StubBean {
	}

	private static class InvalidType {
		@Override
		public String toString() {
			return getClass().getSimpleName();
		}
	}

	private class SimulatedFailure extends RuntimeException {
	}

}
