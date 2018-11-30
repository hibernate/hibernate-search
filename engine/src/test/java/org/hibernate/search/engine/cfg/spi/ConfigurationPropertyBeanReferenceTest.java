/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanProvider;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Test;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;

public class ConfigurationPropertyBeanReferenceTest extends EasyMockSupport {

	private final ConfigurationPropertySource sourceMock = createMock( ConfigurationPropertySource.class );
	private final BeanProvider beanProviderMock = createMock( BeanProvider.class );

	@Test
	public void withDefault() {
		String key = "withDefault";
		ConfigurationProperty<BeanReference<? extends StubBean>> property =
				ConfigurationProperty.forKey( key ).asBeanReference( StubBean.class )
						.withDefault( BeanReference.of( StubBean.class, "theDefault" ) )
						.build();

		StubBeanImpl1 expected = new StubBeanImpl1();
		StubBean result;

		// No value
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( Optional.empty() );
		EasyMock.expect( beanProviderMock.getBean( StubBean.class, "theDefault" ) )
				.andReturn( expected );
		replayAll();
		result = property.get( sourceMock ).getBean( beanProviderMock );
		verifyAll();
		assertThat( result ).isEqualTo( expected );

		// String value
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( "name" ) );
		EasyMock.expect( beanProviderMock.getBean( StubBean.class, "name" ) )
				.andReturn( expected );
		replayAll();
		result = property.get( sourceMock ).getBean( beanProviderMock );
		verifyAll();
		assertThat( result ).isEqualTo( expected );

		// Class value
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( BeanReference.of( StubBeanImpl1.class ) ) );
		EasyMock.expect( beanProviderMock.getBean( StubBeanImpl1.class ) )
				.andReturn( expected );
		replayAll();
		result = property.get( sourceMock ).getBean( beanProviderMock );
		verifyAll();
		assertThat( result ).isEqualTo( expected );

		// BeanReference value
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( BeanReference.of( StubBeanImpl1.class, "name" ) ) );
		EasyMock.expect( beanProviderMock.getBean( StubBeanImpl1.class, "name" ) )
				.andReturn( expected );
		replayAll();
		result = property.get( sourceMock ).getBean( beanProviderMock );
		verifyAll();
		assertThat( result ).isEqualTo( expected );
	}

	@Test
	public void withoutDefault() {
		String key = "withDefault";
		ConfigurationProperty<Optional<BeanReference<? extends StubBean>>> property =
				ConfigurationProperty.forKey( key ).asBeanReference( StubBean.class )
						.build();

		StubBeanImpl1 expected = new StubBeanImpl1();
		Optional<BeanReference<? extends StubBean>> reference;
		StubBean result;

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
		EasyMock.expect( beanProviderMock.getBean( StubBean.class, "name" ) )
				.andReturn( expected );
		replayAll();
		reference = property.get( sourceMock );
		assertThat( reference ).isNotEmpty();
		result = reference.get().getBean( beanProviderMock );
		verifyAll();
		assertThat( result ).isEqualTo( expected );

		// Class value
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( BeanReference.of( StubBeanImpl1.class ) ) );
		EasyMock.expect( beanProviderMock.getBean( StubBeanImpl1.class ) )
				.andReturn( expected );
		replayAll();
		reference = property.get( sourceMock );
		assertThat( reference ).isNotEmpty();
		result = reference.get().getBean( beanProviderMock );
		verifyAll();
		assertThat( result ).isEqualTo( expected );

		// BeanReference value
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( BeanReference.of( StubBeanImpl1.class, "name" ) ) );
		EasyMock.expect( beanProviderMock.getBean( StubBeanImpl1.class, "name" ) )
				.andReturn( expected );
		replayAll();
		reference = property.get( sourceMock );
		assertThat( reference ).isNotEmpty();
		result = reference.get().getBean( beanProviderMock );
		verifyAll();
		assertThat( result ).isEqualTo( expected );
	}

	@Test
	public void multiValued() {
		String key = "multiValued";
		ConfigurationProperty<Optional<List<BeanReference<? extends StubBean>>>> property =
				ConfigurationProperty.forKey( key ).asBeanReference( StubBean.class )
						.multivalued( Pattern.compile( " " ) )
						.build();

		StubBeanImpl1 expected1 = new StubBeanImpl1();
		StubBeanImpl2 expected2 = new StubBeanImpl2();
		Optional<List<BeanReference<? extends StubBean>>> references;
		List<StubBean> result;

		// No value
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( Optional.empty() );
		replayAll();
		references = property.get( sourceMock );
		verifyAll();
		assertThat( references ).isEmpty();

		// String value - one
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( "name" ) );
		EasyMock.expect( beanProviderMock.getBean( StubBean.class, "name" ) )
				.andReturn( expected1 );
		replayAll();
		references = property.get( sourceMock );
		assertThat( references ).isNotEmpty();
		result = getAllBeans( references.get(), beanProviderMock );
		verifyAll();
		assertThat( result ).containsExactly( expected1 );

		// String value - multiple
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of( "name1 name2" ) );
		EasyMock.expect( beanProviderMock.getBean( StubBean.class, "name1" ) )
				.andReturn( expected1 );
		EasyMock.expect( beanProviderMock.getBean( StubBean.class, "name2" ) )
				.andReturn( expected2 );
		replayAll();
		references = property.get( sourceMock );
		assertThat( references ).isNotEmpty();
		result = getAllBeans( references.get(), beanProviderMock );
		verifyAll();
		assertThat( result ).containsExactly( expected1, expected2 );

		// Class value - one
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of(
				createCollection( StubBeanImpl1.class )
		) );
		EasyMock.expect( beanProviderMock.getBean( StubBeanImpl1.class ) )
				.andReturn( expected1 );
		replayAll();
		references = property.get( sourceMock );
		assertThat( references ).isNotEmpty();
		result = getAllBeans( references.get(), beanProviderMock );
		verifyAll();
		assertThat( result ).containsExactly( expected1 );

		// Class value - multiple
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of(
				createCollection( StubBeanImpl1.class, StubBeanImpl2.class )
		) );
		EasyMock.expect( beanProviderMock.getBean( StubBeanImpl1.class ) )
				.andReturn( expected1 );
		EasyMock.expect( beanProviderMock.getBean( StubBeanImpl2.class ) )
				.andReturn( expected2 );
		replayAll();
		references = property.get( sourceMock );
		assertThat( references ).isNotEmpty();
		result = getAllBeans( references.get(), beanProviderMock );
		verifyAll();
		assertThat( result ).containsExactly( expected1, expected2 );

		// BeanReference value - one
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of(
				createCollection( BeanReference.of( StubBeanImpl1.class, "name" ) )
		) );
		EasyMock.expect( beanProviderMock.getBean( StubBeanImpl1.class, "name" ) )
				.andReturn( expected1 );
		replayAll();
		references = property.get( sourceMock );
		assertThat( references ).isNotEmpty();
		result = getAllBeans( references.get(), beanProviderMock );
		verifyAll();
		assertThat( result ).containsExactly( expected1 );

		// BeanReference value - multiple
		resetAll();
		EasyMock.expect( sourceMock.get( key ) ).andReturn( (Optional) Optional.of(
				createCollection(
						BeanReference.of( StubBeanImpl1.class, "name1" ),
						BeanReference.of( StubBeanImpl2.class, "name2" )
				)
		) );
		EasyMock.expect( beanProviderMock.getBean( StubBeanImpl1.class, "name1" ) )
				.andReturn( expected1 );
		EasyMock.expect( beanProviderMock.getBean( StubBeanImpl2.class, "name2" ) )
				.andReturn( expected2 );
		replayAll();
		references = property.get( sourceMock );
		assertThat( references ).isNotEmpty();
		result = getAllBeans( references.get(), beanProviderMock );
		verifyAll();
		assertThat( result ).containsExactly( expected1, expected2 );
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

	@SafeVarargs
	private static <T> Collection<T> createCollection(T... values) {
		// Don't create a List, that would be too easy.
		Collection<T> collection = new LinkedHashSet<>();
		Collections.addAll( collection, values );
		return collection;
	}

	private static <T> List<T> getAllBeans(List<BeanReference<? extends T>> beanReferences,
			BeanProvider beanProviderMock) {
		List<T> beans = new ArrayList<>();
		for ( BeanReference<? extends T> beanReference : beanReferences ) {
			beans.add( beanReference.getBean( beanProviderMock ) );
		}
		return beans;
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

}
