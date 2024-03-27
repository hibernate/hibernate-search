/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContextExtension;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.FromDocumentValueConvertContextImpl;

import org.junit.jupiter.api.Test;

import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Test that implementations of the legacy interface {@link FromDocumentFieldValueConverter}
 * can be used transparently as a {@link FromDocumentValueConverter}.
 */
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@SuppressWarnings("deprecation")
class FromDocumentFieldValueConverterTest {

	@Test
	void fromDocumentFieldValue() {
		BackendSessionContext sessionContextMock = Mockito.mock( BackendSessionContext.class );
		FromDocumentValueConvertContextImpl convertContext = new FromDocumentValueConvertContextImpl( sessionContextMock );

		FromDocumentFieldValueConvertContextExtension<String> extension =
				new FromDocumentFieldValueConvertContextExtension<String>() {
					@Override
					public Optional<String> extendOptional(FromDocumentFieldValueConvertContext original,
							BackendSessionContext sessionContext) {
						assertThat( original ).isSameAs( convertContext );
						assertThat( sessionContext ).isSameAs( sessionContextMock );
						return Optional.of( "Extended!" );
					}
				};

		FromDocumentValueConverter<Integer, String> converter = new FromDocumentFieldValueConverter<Integer, String>() {
			@Override
			public String convert(Integer value, FromDocumentFieldValueConvertContext context) {
				assertThat( context ).isNotNull();
				// Check context extension works
				assertThat( context.extension( extension ) ).isEqualTo( "Extended!" );
				return value.toString();
			}
		};

		assertThat( converter.fromDocumentValue( 42, convertContext ) )
				.isEqualTo( "42" );
	}

	@Test
	void isCompatibleWith_default() {
		class DefaultCompatibilityConverter implements FromDocumentFieldValueConverter<Integer, String> {
			@Override
			public String convert(Integer value, FromDocumentFieldValueConvertContext context) {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}

		// Variables of type FromDocumentValueConverter and not FromDocumentFieldValueConverter,
		// so that we're sure we call the right method.
		FromDocumentValueConverter<?, ?> defaultCompatibilityConverter1 = new DefaultCompatibilityConverter();
		FromDocumentValueConverter<?, ?> defaultCompatibilityConverter2 = new DefaultCompatibilityConverter();

		assertThat( defaultCompatibilityConverter1.isCompatibleWith( defaultCompatibilityConverter2 ) ).isFalse();
		assertThat( defaultCompatibilityConverter1.isCompatibleWith( defaultCompatibilityConverter1 ) ).isTrue();
	}

	@Test
	void isCompatibleWith_custom() {
		class CustomCompatibilityConverter implements FromDocumentFieldValueConverter<Integer, String> {
			@Override
			public String convert(Integer value, FromDocumentFieldValueConvertContext context) {
				throw new UnsupportedOperationException( "Should not be called" );
			}

			@Override
			public boolean isCompatibleWith(FromDocumentFieldValueConverter<?, ?> other) {
				return getClass().equals( other.getClass() );
			}
		}
		class OtherCustomCompatibilityConverter implements FromDocumentFieldValueConverter<Integer, String> {
			@Override
			public String convert(Integer value, FromDocumentFieldValueConvertContext context) {
				throw new UnsupportedOperationException( "Should not be called" );
			}

			@Override
			public boolean isCompatibleWith(FromDocumentFieldValueConverter<?, ?> other) {
				return getClass().equals( other.getClass() );
			}
		}

		// Variables of type FromDocumentValueConverter and not FromDocumentFieldValueConverter,
		// so that we're sure we call the right method.
		FromDocumentValueConverter<?, ?> customCompatibilityConverter1 = new CustomCompatibilityConverter();
		FromDocumentValueConverter<?, ?> customCompatibilityConverter2 = new CustomCompatibilityConverter();
		FromDocumentValueConverter<?, ?> otherCustomCompatibilityConverter1 = new OtherCustomCompatibilityConverter();

		assertThat( customCompatibilityConverter1.isCompatibleWith( customCompatibilityConverter2 ) )
				.isTrue();
		assertThat( customCompatibilityConverter1.isCompatibleWith( otherCustomCompatibilityConverter1 ) )
				.isFalse();
	}

}
