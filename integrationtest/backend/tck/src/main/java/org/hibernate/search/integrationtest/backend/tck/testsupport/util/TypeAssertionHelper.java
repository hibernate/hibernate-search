/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.function.BiPredicate;

import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.BigDecimalFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.BigIntegerFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.DoubleFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FloatFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.OffsetTimeFieldTypeDescriptor;

public abstract class TypeAssertionHelper<F, T> {

	private TypeAssertionHelper() {
	}

	public abstract Class<T> getJavaClass();

	public abstract T create(F fieldValue);

	public void assertSameAggregation(T value1, F value2) {
		assertThat( value1 ).isEqualTo( create( value2 ) );
	}

	public boolean isSame(F a, F b) {
		return Objects.equals( a, b );
	}

	public static <F> TypeAssertionHelper<F, F> identity(FieldTypeDescriptor<F, ?> typeDescriptor) {
		return new TypeAssertionHelper<F, F>() {
			@Override
			public Class<F> getJavaClass() {
				return typeDescriptor.getJavaType();
			}

			@Override
			public F create(F fieldValue) {
				return fieldValue;
			}
		};
	}

	@SuppressWarnings("rawtypes")
	public static <F> TypeAssertionHelper<F, ValueWrapper> wrapper(FieldTypeDescriptor<F, ?> typeDescriptor) {
		return new TypeAssertionHelper<F, ValueWrapper>() {
			@Override
			public Class<ValueWrapper> getJavaClass() {
				return ValueWrapper.class;
			}

			@Override
			public ValueWrapper create(F fieldValue) {
				return new ValueWrapper<>( fieldValue );
			}
		};
	}

	public static <F, T> TypeAssertionHelper<F, T> nullType() {
		return new TypeAssertionHelper<F, T>() {
			@Override
			public Class<T> getJavaClass() {
				return null;
			}

			@Override
			public T create(F fieldValue) {
				return neverCalled( fieldValue );
			}
		};
	}

	public static <F, T> TypeAssertionHelper<F, T> wrongType(FieldTypeDescriptor<T, ?> wrongTypeDescriptor) {
		return new TypeAssertionHelper<F, T>() {
			@Override
			public Class<T> getJavaClass() {
				return wrongTypeDescriptor.getJavaType();
			}

			@Override
			public T create(F fieldValue) {
				return neverCalled( fieldValue );
			}
		};
	}

	@SuppressWarnings("unchecked")
	public static <F, T> TypeAssertionHelper<F, T> raw(FieldTypeDescriptor<F, ?> typeDescriptor) {
		BiPredicate<F, F> isSame;
		if ( TckConfiguration.get().getBackendFeatures().rawType( typeDescriptor ).equals( String.class ) ) {
			// If the raw type is String then we are looking at the Elasticsearch backend.
			// Yes the strings in Lucene will also have string raw type, but we are not overriding the equals check for strings
			// hence this "assumption" is relatively safe to make.
			if ( BigDecimalFieldTypeDescriptor.INSTANCE.equals( typeDescriptor ) ) {
				isSame = (a, b) -> new BigDecimal( ( (String) a ) ).setScale( 2, RoundingMode.HALF_UP )
						.equals( new BigDecimal( ( (String) b ) ).setScale( 2, RoundingMode.HALF_UP ) );
			}
			else if ( FloatFieldTypeDescriptor.INSTANCE.equals( typeDescriptor ) ) {
				isSame = (a, b) -> Math.abs( ParseUtils.parseFloat( (String) a )
						- ParseUtils.parseFloat( (String) b ) ) < 1.0e-5f;
			}
			else if ( DoubleFieldTypeDescriptor.INSTANCE.equals( typeDescriptor ) ) {
				isSame = (a, b) -> Math.abs( ParseUtils.parseDouble( (String) a )
						- ParseUtils.parseDouble( (String) b ) ) < 1.0e-5;
			}
			else if ( BigIntegerFieldTypeDescriptor.INSTANCE.equals( typeDescriptor ) ) {
				isSame = (a, b) -> ParseUtils.parseBigDecimal( (String) a ).toBigInteger()
						.equals( ParseUtils.parseBigDecimal( (String) b ).toBigInteger() );
			}
			else {
				isSame = Objects::equals;
			}
		}
		else {
			if ( OffsetTimeFieldTypeDescriptor.INSTANCE.equals( typeDescriptor ) ) {
				isSame = (a, b) -> Instant.EPOCH.plus( (long) a, ChronoUnit.NANOS )
						.atOffset( ZoneOffset.UTC ).toOffsetTime()
						.equals( Instant.EPOCH.plus( (long) b, ChronoUnit.NANOS )
								.atOffset( ZoneOffset.UTC ).toOffsetTime() );
			}
			else {
				isSame = Objects::equals;
			}
		}
		return new TypeAssertionHelper<F, T>() {
			@Override
			public Class<T> getJavaClass() {
				return (Class<T>) TckConfiguration.get().getBackendFeatures().rawType( typeDescriptor );
			}

			@Override
			public T create(F fieldValue) {
				return (T) TckConfiguration.get().getBackendFeatures().toRawValue( typeDescriptor, fieldValue );
			}

			@Override
			public boolean isSame(F a, F b) {
				return isSame.test( a, b );
			}

			@Override
			public void assertSameAggregation(T value1, F value2) {
				if ( Number.class.isAssignableFrom( typeDescriptor.getJavaType() ) ) {
					assertThat( Double.parseDouble( TckConfiguration.get().getBackendFeatures()
							.fromRawAggregation( typeDescriptor, value1 ).toString() ) )
							.isEqualTo( Double.parseDouble( value2.toString() ) );
				}
				else {
					assertThat( TckConfiguration.get().getBackendFeatures().fromRawAggregation( typeDescriptor, value1 ) )
							.isEqualTo( create( value2 ) );
				}
			}
		};
	}

	public static <F> TypeAssertionHelper<F, String> string(FieldTypeDescriptor<F, ?> typeDescriptor) {
		return new TypeAssertionHelper<F, String>() {
			@Override
			public Class<String> getJavaClass() {
				return String.class;
			}

			@Override
			public String create(F fieldValue) {
				return TckConfiguration.get().getBackendFeatures().toStringValue( typeDescriptor, fieldValue );
			}
		};
	}

	public static <F> TypeAssertionHelper<F, Double> rawDouble(FieldTypeDescriptor<F, ?> typeDescriptor) {
		return new TypeAssertionHelper<F, Double>() {
			@Override
			public Class<Double> getJavaClass() {
				return Double.class;
			}

			@Override
			public Double create(F fieldValue) {
				return TckConfiguration.get().getBackendFeatures().toDoubleValue( typeDescriptor, fieldValue );
			}
		};
	}

	private static <P1, R> R neverCalled(P1 param) {
		throw new IllegalStateException( "This should not be called; called with parameter " + param );
	}

}
