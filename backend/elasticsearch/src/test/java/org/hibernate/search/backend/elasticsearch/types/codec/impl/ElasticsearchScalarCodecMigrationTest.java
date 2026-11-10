package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;

import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonProviderHelper;
import org.hibernate.search.backend.elasticsearch.json.jackson.JacksonJsonHandlers;
import org.hibernate.search.engine.backend.types.VectorSimilarity;

import org.junit.jupiter.api.Test;

class ElasticsearchScalarCodecMigrationTest {
	@Test
	void ordinaryNumericFieldsKeepTruncationAndWrappingAcrossExactNodeClasses() {
		var json = JsonProviderHelper.create( false ).getCodec();
		var integer = new ElasticsearchIntegerFieldCodec( json );
		var bytes = new ElasticsearchByteFieldCodec( json );
		assertThat( integer.decode( JacksonJsonHandlers.value().fromJson( "4294967297" ) ) ).isEqualTo( 1 );
		assertThat( integer.decode( JacksonJsonHandlers.value().fromJson( "-3.9" ) ) ).isEqualTo( -3 );
		assertThat( bytes.decode( JacksonJsonHandlers.value().fromJson( "258" ) ) ).isEqualTo( (byte) 2 );
		assertThat( integer.decode( JacksonJsonHandlers.value().fromJson( "null" ) ) ).isNull();
		assertThatThrownBy( () -> integer.decode( JacksonJsonHandlers.value().fromJson( "\"123\"" ) ) )
				.isInstanceOf( RuntimeException.class );
	}

	@Test
	void scaledFloatAggregationKeysRetainTruncationWithBoundedIntegerExpansion() {
		var codec = new ElasticsearchBigIntegerFieldCodec( JsonProviderHelper.create( false ).getCodec(), 0 );
		assertThat( codec.decodeAggregationKey( JacksonJsonHandlers.value().fromJson( "-3.9" ), null ) )
				.isEqualTo( BigInteger.valueOf( -3 ) );
		assertThat( codec.decode( JacksonJsonHandlers.value().fromJson( "1.25e2" ) ) )
				.isEqualTo( BigInteger.valueOf( 125 ) );
		assertThatThrownBy( () -> codec.decodeAggregationKey( JacksonJsonHandlers.value().fromJson( "1e100001" ), null ) )
				.isInstanceOf( ArithmeticException.class );
	}

	@Test
	void vectorsRetainArrayDecodingAndSignedFloatingZero() {
		var codec = new ElasticsearchFloatVectorFieldCodec( JsonProviderHelper.create( false ).getCodec(),
				VectorSimilarity.DEFAULT, 3, null, null, null );
		float[] values = codec.decode( JacksonJsonHandlers.value().fromJson( "[-0.0,1.25,2]" ) );
		assertThat( values ).containsExactly( -0.0f, 1.25f, 2.0f );
		assertThat( Float.floatToRawIntBits( values[0] ) ).isEqualTo( Integer.MIN_VALUE );
		assertThat( codec.canDecodeArrays() ).isTrue();
	}
}
