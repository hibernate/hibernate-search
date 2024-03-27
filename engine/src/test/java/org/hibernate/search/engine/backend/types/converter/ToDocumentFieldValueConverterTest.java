/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContextExtension;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentValueConvertContextImpl;

import org.junit.jupiter.api.Test;

import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Test that implementations of the legacy interface {@link ToDocumentFieldValueConverter}
 * can be used transparently as a {@link ToDocumentValueConverter}.
 */
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@SuppressWarnings("deprecation")
class ToDocumentFieldValueConverterTest {

	@Test
	void fromDocumentFieldValue() {
		BackendMappingContext mappingContextMock = Mockito.mock( BackendMappingContext.class );
		ToDocumentValueConvertContextImpl convertContext = new ToDocumentValueConvertContextImpl( mappingContextMock );

		ToDocumentFieldValueConvertContextExtension<String> extension =
				new ToDocumentFieldValueConvertContextExtension<String>() {
					@Override
					public Optional<String> extendOptional(ToDocumentFieldValueConvertContext original,
							BackendMappingContext mappingContext) {
						assertThat( original ).isSameAs( convertContext );
						assertThat( mappingContext ).isSameAs( mappingContextMock );
						return Optional.of( "Extended!" );
					}
				};

		ToDocumentValueConverter<Integer, String> converter = new ToDocumentFieldValueConverter<Integer, String>() {
			@Override
			public String convert(Integer value, ToDocumentFieldValueConvertContext context) {
				assertThat( context ).isNotNull();
				// Check context extension works
				assertThat( context.extension( extension ) ).isEqualTo( "Extended!" );
				return value.toString();
			}
		};

		assertThat( converter.toDocumentValue( 42, convertContext ) )
				.isEqualTo( "42" );
	}

	@Test
	void isCompatibleWith_default() {
		class DefaultCompatibilityConverter implements ToDocumentFieldValueConverter<Integer, String> {
			@Override
			public String convert(Integer value, ToDocumentFieldValueConvertContext context) {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}

		// Variables of type ToDocumentValueConverter and not ToDocumentFieldValueConverter,
		// so that we're sure we call the right method.
		ToDocumentValueConverter<?, ?> defaultCompatibilityConverter1 = new DefaultCompatibilityConverter();
		ToDocumentValueConverter<?, ?> defaultCompatibilityConverter2 = new DefaultCompatibilityConverter();

		assertThat( defaultCompatibilityConverter1.isCompatibleWith( defaultCompatibilityConverter2 ) ).isFalse();
		assertThat( defaultCompatibilityConverter1.isCompatibleWith( defaultCompatibilityConverter1 ) ).isTrue();
	}

	@Test
	void isCompatibleWith_custom() {
		class CustomCompatibilityConverter implements ToDocumentFieldValueConverter<Integer, String> {
			@Override
			public String convert(Integer value, ToDocumentFieldValueConvertContext context) {
				throw new UnsupportedOperationException( "Should not be called" );
			}

			@Override
			public boolean isCompatibleWith(ToDocumentFieldValueConverter<?, ?> other) {
				return getClass().equals( other.getClass() );
			}
		}
		class OtherCustomCompatibilityConverter implements ToDocumentFieldValueConverter<Integer, String> {
			@Override
			public String convert(Integer value, ToDocumentFieldValueConvertContext context) {
				throw new UnsupportedOperationException( "Should not be called" );
			}

			@Override
			public boolean isCompatibleWith(ToDocumentFieldValueConverter<?, ?> other) {
				return getClass().equals( other.getClass() );
			}
		}

		// Variables of type ToDocumentValueConverter and not ToDocumentFieldValueConverter,
		// so that we're sure we call the right method.
		ToDocumentValueConverter<?, ?> customCompatibilityConverter1 = new CustomCompatibilityConverter();
		ToDocumentValueConverter<?, ?> customCompatibilityConverter2 = new CustomCompatibilityConverter();
		ToDocumentValueConverter<?, ?> otherCustomCompatibilityConverter1 = new OtherCustomCompatibilityConverter();

		assertThat( customCompatibilityConverter1.isCompatibleWith( customCompatibilityConverter2 ) )
				.isTrue();
		assertThat( customCompatibilityConverter1.isCompatibleWith( otherCustomCompatibilityConverter1 ) )
				.isFalse();
	}

}
