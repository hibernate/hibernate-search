/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.util.common.SearchException;

import org.junit.jupiter.api.Test;

import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@SuppressWarnings({ "unchecked", "rawtypes" }) // Raw types are the only way to mock parameterized types
class ConfigurationPropertyBeanReferenceTest {

	@Mock
	private ConfigurationPropertySource sourceMock;
	@Mock(strictness = Mock.Strictness.LENIENT, answer = Answers.CALLS_REAL_METHODS)
	private BeanResolver beanResolverMock;

	@Test
	void withDefault() {
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

		// String value - no prefix
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( "name" ) );
		when( beanResolverMock.resolve( StubBean.class, "name", BeanRetrieval.ANY ) )
				.thenReturn( expectedAsStubBean );
		result = property.get( sourceMock ).resolve( beanResolverMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expectedAsStubBean );

		// String value - 'bean:*'
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( "bean:name" ) );
		when( beanResolverMock.resolve( StubBean.class, "name", BeanRetrieval.BEAN ) )
				.thenReturn( expectedAsStubBean );
		result = property.get( sourceMock ).resolve( beanResolverMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expectedAsStubBean );

		// String value - 'class:*'
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( "class:name" ) );
		when( beanResolverMock.resolve( StubBean.class, "name", BeanRetrieval.CLASS ) )
				.thenReturn( expectedAsStubBean );
		result = property.get( sourceMock ).resolve( beanResolverMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expectedAsStubBean );

		// String value - 'constructor:*'
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( "constructor:name" ) );
		when( beanResolverMock.resolve( StubBean.class, "name", BeanRetrieval.CONSTRUCTOR ) )
				.thenReturn( expectedAsStubBean );
		result = property.get( sourceMock ).resolve( beanResolverMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expectedAsStubBean );

		// Class value
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( StubBeanImpl1.class ) );
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
	void withoutDefault() {
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

		// String value - no prefix
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( "name" ) );
		when( beanResolverMock.resolve( StubBean.class, "name", BeanRetrieval.ANY ) )
				.thenReturn( expectedAsStubBean );
		reference = property.get( sourceMock );
		assertThat( reference ).isNotEmpty();
		result = reference.get().resolve( beanResolverMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expectedAsStubBean );

		// String value - 'bean:*'
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( "bean:name" ) );
		when( beanResolverMock.resolve( StubBean.class, "name", BeanRetrieval.BEAN ) )
				.thenReturn( expectedAsStubBean );
		reference = property.get( sourceMock );
		assertThat( reference ).isNotEmpty();
		result = reference.get().resolve( beanResolverMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expectedAsStubBean );

		// String value - 'class:*'
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( "class:name" ) );
		when( beanResolverMock.resolve( StubBean.class, "name", BeanRetrieval.CLASS ) )
				.thenReturn( expectedAsStubBean );
		reference = property.get( sourceMock );
		assertThat( reference ).isNotEmpty();
		result = reference.get().resolve( beanResolverMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expectedAsStubBean );

		// String value - 'constructor:*'
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( "constructor:name" ) );
		when( beanResolverMock.resolve( StubBean.class, "name", BeanRetrieval.CONSTRUCTOR ) )
				.thenReturn( expectedAsStubBean );
		reference = property.get( sourceMock );
		assertThat( reference ).isNotEmpty();
		result = reference.get().resolve( beanResolverMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expectedAsStubBean );

		// Class value
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( StubBeanImpl1.class ) );
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
	void multiValued() {
		String key = "multiValued";
		OptionalConfigurationProperty<List<BeanReference<? extends StubBean>>> property =
				ConfigurationProperty.forKey( key ).asBeanReference( StubBean.class )
						.multivalued()
						.build();

		BeanHolder<StubBeanImpl1> expected1 = BeanHolder.of( new StubBeanImpl1() );
		BeanHolder<StubBean> expected1AsStubBean = BeanHolder.of( new StubBeanImpl1() );
		BeanHolder<StubBeanImpl2> expected2 = BeanHolder.of( new StubBeanImpl2() );
		BeanHolder<StubBean> expected2AsStubBean = BeanHolder.of( new StubBeanImpl2() );
		BeanHolder<StubBean> expected3AsStubBean = BeanHolder.of( new StubBeanImpl2() );
		BeanHolder<StubBean> expected4AsStubBean = BeanHolder.of( new StubBeanImpl2() );
		BeanHolder<StubBean> expected5AsStubBean = BeanHolder.of( new StubBeanImpl2() );
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
		when( sourceMock.get( key ) )
				.thenReturn( (Optional) Optional.of( "name1,name2,bean:name3,class:name4,constructor:name5" ) );
		when( beanResolverMock.resolve( StubBean.class, "name1", BeanRetrieval.ANY ) )
				.thenReturn( expected1AsStubBean );
		when( beanResolverMock.resolve( StubBean.class, "name2", BeanRetrieval.ANY ) )
				.thenReturn( expected2AsStubBean );
		when( beanResolverMock.resolve( StubBean.class, "name3", BeanRetrieval.BEAN ) )
				.thenReturn( expected3AsStubBean );
		when( beanResolverMock.resolve( StubBean.class, "name4", BeanRetrieval.CLASS ) )
				.thenReturn( expected4AsStubBean );
		when( beanResolverMock.resolve( StubBean.class, "name5", BeanRetrieval.CONSTRUCTOR ) )
				.thenReturn( expected5AsStubBean );
		result = property.getAndMap( sourceMock, beanResolverMock::resolve );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isNotEmpty();
		assertThat( result.get().get() ).containsExactly( expected1AsStubBean.get(), expected2AsStubBean.get(),
				expected3AsStubBean.get(), expected4AsStubBean.get(), expected5AsStubBean.get() );

		// Class value - one (not wrapped in a collection)
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( StubBeanImpl1.class ) );
		when( beanResolverMock.resolve( StubBeanImpl1.class, BeanRetrieval.ANY ) )
				.thenReturn( expected1 );
		result = property.getAndMap( sourceMock, beanResolverMock::resolve );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isNotEmpty();
		assertThat( result.get().get() ).containsExactly( expected1.get() );

		// Class value - one (wrapped in a collection)
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

		// BeanReference value - one (not wrapped in a collection)
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of(
				BeanReference.of( StubBeanImpl1.class, "name" )
		) );
		when( beanResolverMock.resolve( StubBeanImpl1.class, "name", BeanRetrieval.ANY ) )
				.thenReturn( expected1 );
		result = property.getAndMap( sourceMock, beanResolverMock::resolve );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isNotEmpty();
		assertThat( result.get().get() ).containsExactly( expected1.get() );

		// BeanReference value - one (wrapped in a collection)
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
	void invalidType() {
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
	void invalidReference() {
		String key = "invalidReference";
		String resolvedKey = "some.prefix." + key;
		OptionalConfigurationProperty<BeanReference<? extends StubBean>> property =
				ConfigurationProperty.forKey( key ).asBeanReference( StubBean.class )
						.build();

		String propertyValue = "name";
		SimulatedFailure simulatedFailure = new SimulatedFailure( "some message" );

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
				.hasMessageContainingAll(
						"Invalid value for configuration property '" + resolvedKey
								+ "': '" + propertyValue + "'.",
						simulatedFailure.getMessage()
				);
		verifyNoOtherSourceInteractionsAndReset();
	}

	@Test
	void multiValued_invalidReference() {
		String key = "multiValued_invalidReference";
		String resolvedKey = "some.prefix." + key;
		OptionalConfigurationProperty<List<BeanReference<? extends StubBean>>> property =
				ConfigurationProperty.forKey( key ).asBeanReference( StubBean.class )
						.multivalued()
						.build();

		BeanHolder<StubBean> bean1Mock = mock( BeanHolder.class );

		String propertyValue = "name1,name2";
		SimulatedFailure simulatedFailure = new SimulatedFailure( "some message" );

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
								+ "': '" + propertyValue + "'.",
						"Invalid multi value: expected either a single value of the correct type, a Collection, or a String",
						"interpreting as a single value failed with the following exception",
						simulatedFailure.getMessage()
				);
		verify( bean1Mock ).close(); // Expect the first bean holder to be closed
		verifyNoMoreInteractions( bean1Mock );
		verifyNoOtherSourceInteractionsAndReset();
	}

	@Test
	void invalidBeanRetrieval() {
		String key = "invalidBeanRetrieval";
		String resolvedKey = "some.prefix." + key;
		OptionalConfigurationProperty<BeanReference<? extends StubBean>> property =
				ConfigurationProperty.forKey( key ).asBeanReference( StubBean.class )
						.build();

		String propertyValue = "notABeanRetrieval:name";

		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( propertyValue ) );
		when( sourceMock.resolve( key ) ).thenReturn( Optional.of( resolvedKey ) );
		assertThatThrownBy( () -> property.getAndMap( sourceMock, beanResolverMock::resolve ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid value for configuration property '" + resolvedKey
								+ "': '" + propertyValue + "'.",
						"Invalid bean reference: 'notABeanRetrieval:name'.",
						"The reference is prefixed with 'notABeanRetrieval:', which is not a valid bean retrieval prefix.",
						"If you want to reference a bean by name, and the name contains a colon, use 'bean:notABeanRetrieval:name'.",
						"Otherwise, use a valid bean retrieval prefix among the following:"
								+ " [builtin:, bean:, class:, constructor:, any:]."
				);
		verifyNoOtherSourceInteractionsAndReset();

		// Check that prefixing with 'bean:' solves the problem
		BeanHolder<StubBean> expectedAsStubBean = BeanHolder.of( new StubBeanImpl1() );
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( "bean:" + propertyValue ) );
		when( sourceMock.resolve( key ) ).thenReturn( Optional.of( resolvedKey ) );
		when( beanResolverMock.resolve( StubBean.class, "notABeanRetrieval:name", BeanRetrieval.BEAN ) )
				.thenReturn( expectedAsStubBean );
		Optional<BeanHolder<? extends StubBean>> result = property.getAndMap( sourceMock, beanResolverMock::resolve );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).contains( expectedAsStubBean );
	}

	@Test
	void substitute_function() {
		String key = "substitute_function";
		ConfigurationProperty<Optional<BeanReference<? extends StubBean>>> property =
				ConfigurationProperty.forKey( key ).asBeanReference( StubBean.class )
						.substitute( v -> {
							if ( v instanceof MyEnum ) {
								switch ( (MyEnum) v ) {
									case VALUE1:
										return "bean:name";
									case VALUE2:
										return StubBeanImpl1.class;
									case VALUE3:
										return BeanReference.of( StubBeanImpl1.class, "name" );
								}
							}
							return v;
						} )
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

		// Enum substituted with String value - 'bean:*'
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( MyEnum.VALUE1 ) );
		when( beanResolverMock.resolve( StubBean.class, "name", BeanRetrieval.BEAN ) )
				.thenReturn( expectedAsStubBean );
		reference = property.get( sourceMock );
		assertThat( reference ).isNotEmpty();
		result = reference.get().resolve( beanResolverMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expectedAsStubBean );

		// Enum substituted with Class value
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( MyEnum.VALUE2 ) );
		when( beanResolverMock.resolve( StubBeanImpl1.class, BeanRetrieval.ANY ) )
				.thenReturn( expected );
		reference = property.get( sourceMock );
		assertThat( reference ).isNotEmpty();
		result = reference.get().resolve( beanResolverMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expected );

		// Enum substituted with BeanReference value
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( MyEnum.VALUE3 ) );
		when( beanResolverMock.resolve( StubBeanImpl1.class, "name", BeanRetrieval.ANY ) )
				.thenReturn( expected );
		reference = property.get( sourceMock );
		assertThat( reference ).isNotEmpty();
		result = reference.get().resolve( beanResolverMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expected );

		// String value - 'bean:*'
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( "bean:name" ) );
		when( beanResolverMock.resolve( StubBean.class, "name", BeanRetrieval.BEAN ) )
				.thenReturn( expectedAsStubBean );
		reference = property.get( sourceMock );
		assertThat( reference ).isNotEmpty();
		result = reference.get().resolve( beanResolverMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expectedAsStubBean );

		// Class value
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( StubBeanImpl1.class ) );
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
	void substitute_literals() {
		String key = "substitute_literals";
		ConfigurationProperty<Optional<BeanReference<? extends StubBean>>> property =
				ConfigurationProperty.forKey( key ).asBeanReference( StubBean.class )
						.substitute( MyEnum.VALUE1, "bean:name" )
						.substitute( MyEnum.VALUE2, StubBeanImpl1.class )
						.substitute( MyEnum.VALUE3, BeanReference.of( StubBeanImpl1.class, "name" ) )
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

		// Enum substituted with String value - 'bean:*'
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( MyEnum.VALUE1 ) );
		when( beanResolverMock.resolve( StubBean.class, "name", BeanRetrieval.BEAN ) )
				.thenReturn( expectedAsStubBean );
		reference = property.get( sourceMock );
		assertThat( reference ).isNotEmpty();
		result = reference.get().resolve( beanResolverMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expectedAsStubBean );

		// Enum substituted with Class value
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( MyEnum.VALUE2 ) );
		when( beanResolverMock.resolve( StubBeanImpl1.class, BeanRetrieval.ANY ) )
				.thenReturn( expected );
		reference = property.get( sourceMock );
		assertThat( reference ).isNotEmpty();
		result = reference.get().resolve( beanResolverMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expected );

		// Enum substituted with BeanReference value
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( MyEnum.VALUE3 ) );
		when( beanResolverMock.resolve( StubBeanImpl1.class, "name", BeanRetrieval.ANY ) )
				.thenReturn( expected );
		reference = property.get( sourceMock );
		assertThat( reference ).isNotEmpty();
		result = reference.get().resolve( beanResolverMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expected );

		// String value - 'bean:*'
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( "bean:name" ) );
		when( beanResolverMock.resolve( StubBean.class, "name", BeanRetrieval.BEAN ) )
				.thenReturn( expectedAsStubBean );
		reference = property.get( sourceMock );
		assertThat( reference ).isNotEmpty();
		result = reference.get().resolve( beanResolverMock );
		verifyNoOtherSourceInteractionsAndReset();
		assertThat( result ).isEqualTo( expectedAsStubBean );

		// Class value
		when( sourceMock.get( key ) ).thenReturn( (Optional) Optional.of( StubBeanImpl1.class ) );
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

	private void verifyNoOtherSourceInteractionsAndReset() {
		verifyNoMoreInteractions( sourceMock );
		reset( sourceMock, beanResolverMock );
	}

	@SafeVarargs
	private static <T> Collection<T> createCollection(T... values) {
		// Don't expose a List, that would be too easy.
		// Instead, wrap the list into a collection.
		return Collections.unmodifiableCollection( Arrays.asList( values ) );
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
		public SimulatedFailure(String message) {
			super( message );
		}
	}

	enum MyEnum {
		VALUE1,
		VALUE2,
		VALUE3;
	}

}
