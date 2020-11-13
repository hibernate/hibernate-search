/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;

import org.junit.Rule;
import org.junit.Test;

import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

@SuppressWarnings({"unchecked", "rawtypes"}) // Raw types are the only way to mock parameterized types
public class ConfigurationPropertyBeanReferenceTest {

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	@Mock
	private ConfigurationPropertySource sourceMock;
	@Mock(lenient = true, answer = Answers.CALLS_REAL_METHODS)
	private BeanResolver beanResolverMock;

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
		when( sourceMock.get( key ) ).thenReturn( Optional.empty() );
		when( beanResolverMock.resolve( StubBean.class, "theDefault", BeanRetrieval.ANY ) )
				.thenReturn( expectedAsStubBean );
		result = property.get( sourceMock ).resolve( beanResolverMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expectedAsStubBean );

		// String value
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( "name" ) );
		when( beanResolverMock.resolve( StubBean.class, "name", BeanRetrieval.ANY ) )
				.thenReturn( expectedAsStubBean );
		result = property.get( sourceMock ).resolve( beanResolverMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expectedAsStubBean );

		// Class value
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( BeanReference.of( StubBeanImpl1.class ) ) );
		when( beanResolverMock.resolve( StubBeanImpl1.class, BeanRetrieval.ANY ) )
				.thenReturn( expected );
		result = property.get( sourceMock ).resolve( beanResolverMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expected );

		// BeanReference value
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( BeanReference.of( StubBeanImpl1.class, "name" ) ) );
		when( beanResolverMock.resolve( StubBeanImpl1.class, "name", BeanRetrieval.ANY ) )
				.thenReturn( expected );
		result = property.get( sourceMock ).resolve( beanResolverMock );
		verifyNoOtherSourceInteractionsAndReset();
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
		when( sourceMock.get( key ) ).thenReturn( Optional.empty() );
		reference = property.get( sourceMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( reference ).isEmpty();

		// String value
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( "name" ) );
		when( beanResolverMock.resolve( StubBean.class, "name", BeanRetrieval.ANY ) )
				.thenReturn( expectedAsStubBean );
		reference = property.get( sourceMock );
		assertThat( reference ).isNotEmpty();
		result = reference.get().resolve( beanResolverMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expectedAsStubBean );

		// Class value
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( BeanReference.of( StubBeanImpl1.class ) ) );
		when( beanResolverMock.resolve( StubBeanImpl1.class, BeanRetrieval.ANY ) )
				.thenReturn( expected );
		reference = property.get( sourceMock );
		assertThat( reference ).isNotEmpty();
		result = reference.get().resolve( beanResolverMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expected );

		// BeanReference value
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( BeanReference.of( StubBeanImpl1.class, "name" ) ) );
		when( beanResolverMock.resolve( StubBeanImpl1.class, "name", BeanRetrieval.ANY ) )
				.thenReturn( expected );
		reference = property.get( sourceMock );
		assertThat( reference ).isNotEmpty();
		result = reference.get().resolve( beanResolverMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expected );
	}

	@Test
	public void multiValued() {
		String key = "multiValued";
		OptionalConfigurationProperty<List<BeanReference<? extends StubBean>>> property =
				ConfigurationProperty.forKey( key ).asBeanReference( StubBean.class )
						.multivalued()
						.build();

		BeanHolder<StubBeanImpl1> expected1 = BeanHolder.of( new StubBeanImpl1() );
		BeanHolder<StubBean> expected1AsStubBean = BeanHolder.of( new StubBeanImpl1() );
		BeanHolder<StubBeanImpl2> expected2 = BeanHolder.of( new StubBeanImpl2() );
		BeanHolder<StubBean> expected2AsStubBean = BeanHolder.of( new StubBeanImpl2() );
		Optional<BeanHolder<List<StubBean>>> result;

		// No value
		when( sourceMock.get( key ) ).thenReturn( Optional.empty() );
		result = property.getAndMap( sourceMock, beanResolverMock::resolve );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEmpty();

		// String value - one
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( "name" ) );
		when( beanResolverMock.resolve( StubBean.class, "name", BeanRetrieval.ANY ) )
				.thenReturn( expected1AsStubBean );
		result = property.getAndMap( sourceMock, beanResolverMock::resolve );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isNotEmpty();
		assertThat( result.get().get() ).containsExactly( expected1AsStubBean.get() );

		// String value - multiple
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( "name1,name2" ) );
		when( beanResolverMock.resolve( StubBean.class, "name1", BeanRetrieval.ANY ) )
				.thenReturn( expected1AsStubBean );
		when( beanResolverMock.resolve( StubBean.class, "name2", BeanRetrieval.ANY ) )
				.thenReturn( expected2AsStubBean );
		result = property.getAndMap( sourceMock, beanResolverMock::resolve );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isNotEmpty();
		assertThat( result.get().get() ).containsExactly( expected1AsStubBean.get(), expected2AsStubBean.get() );

		// Class value - one
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of(
				createCollection( StubBeanImpl1.class )
		) );
		when( beanResolverMock.resolve( StubBeanImpl1.class, BeanRetrieval.ANY ) )
				.thenReturn( expected1 );
		result = property.getAndMap( sourceMock, beanResolverMock::resolve );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isNotEmpty();
		assertThat( result.get().get() ).containsExactly( expected1.get() );

		// Class value - multiple
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of(
				createCollection( StubBeanImpl1.class, StubBeanImpl2.class )
		) );
		when( beanResolverMock.resolve( StubBeanImpl1.class, BeanRetrieval.ANY ) )
				.thenReturn( expected1 );
		when( beanResolverMock.resolve( StubBeanImpl2.class, BeanRetrieval.ANY ) )
				.thenReturn( expected2 );
		result = property.getAndMap( sourceMock, beanResolverMock::resolve );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isNotEmpty();
		assertThat( result.get().get() ).containsExactly( expected1.get(), expected2.get() );

		// BeanReference value - one
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of(
				createCollection( BeanReference.of( StubBeanImpl1.class, "name" ) )
		) );
		when( beanResolverMock.resolve( StubBeanImpl1.class, "name", BeanRetrieval.ANY ) )
				.thenReturn( expected1 );
		result = property.getAndMap( sourceMock, beanResolverMock::resolve );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isNotEmpty();
		assertThat( result.get().get() ).containsExactly( expected1.get() );

		// BeanReference value - multiple
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of(
				createCollection(
						BeanReference.of( StubBeanImpl1.class, "name1" ),
						BeanReference.of( StubBeanImpl2.class, "name2" )
				)
		) );
		when( beanResolverMock.resolve( StubBeanImpl1.class, "name1", BeanRetrieval.ANY ) )
				.thenReturn( expected1 );
		when( beanResolverMock.resolve( StubBeanImpl2.class, "name2", BeanRetrieval.ANY ) )
				.thenReturn( expected2 );
		result = property.getAndMap( sourceMock, beanResolverMock::resolve );
		verifyNoOtherSourceInteractionsAndReset();
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
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( invalidTypeValue ) );
		when( sourceMock.resolve( key ) ).thenReturn( Optional.of( resolvedKey ) );
		assertThatThrownBy( () -> property.get( sourceMock ) )
				.hasMessageContaining(
						"Invalid value for configuration property '" + resolvedKey
								+ "': '" + invalidTypeValue + "'."
				)
				.hasMessageContaining(
						"Invalid BeanReference value: expected an instance of '" + StubBean.class.getName()
								+ "', BeanReference, String or Class"
				);
		verifyNoOtherSourceInteractionsAndReset();
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

		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( propertyValue ) );
		when( sourceMock.resolve( key ) ).thenReturn( Optional.of( resolvedKey ) );
		when( beanResolverMock.resolve( StubBean.class, "name", BeanRetrieval.ANY ) )
				.thenThrow( simulatedFailure );
		assertThatThrownBy(
				() -> {
					property.getAndMap( sourceMock, beanResolverMock::resolve );
				}
		)
				.hasCause( simulatedFailure )
				.hasMessageContaining(
						"Invalid value for configuration property '" + resolvedKey
								+ "': '" + propertyValue + "'."
				);
		verifyNoOtherSourceInteractionsAndReset();
	}

	@Test
	public void multiValued_invalidReference() {
		String key = "multiValued_invalidReference";
		String resolvedKey = "some.prefix." + key;
		OptionalConfigurationProperty<List<BeanReference<? extends StubBean>>> property =
				ConfigurationProperty.forKey( key ).asBeanReference( StubBean.class )
						.multivalued()
						.build();

		BeanHolder<StubBean> bean1Mock = mock( BeanHolder.class );

		String propertyValue = "name1,name2";
		SimulatedFailure simulatedFailure = new SimulatedFailure();

		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( propertyValue ) );
		when( sourceMock.resolve( key ) ).thenReturn( Optional.of( resolvedKey ) );
		when( beanResolverMock.resolve( StubBean.class, "name1", BeanRetrieval.ANY ) )
				.thenReturn( bean1Mock );
		when( beanResolverMock.resolve( StubBean.class, "name2", BeanRetrieval.ANY ) )
				.thenThrow( simulatedFailure );
		assertThatThrownBy(
				() -> {
					property.getAndMap( sourceMock, beanResolverMock::resolve );
				}
		)
				.hasCause( simulatedFailure )
				.hasMessageContaining(
						"Invalid value for configuration property '" + resolvedKey
								+ "': '" + propertyValue + "'."
				);
		verify( bean1Mock ).close(); // Expect the first bean holder to be closed
		verifyNoMoreInteractions( bean1Mock );
		verifyNoOtherSourceInteractionsAndReset();
	}

	private void verifyNoOtherSourceInteractionsAndReset() {
		verifyNoMoreInteractions( sourceMock );
		reset( sourceMock, beanResolverMock );
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
