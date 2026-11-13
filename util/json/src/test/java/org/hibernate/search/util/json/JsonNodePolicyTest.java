package org.hibernate.search.util.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.search.util.json.internal.spi.JsonNumbers;
import org.hibernate.search.util.json.internal.spi.JsonParsing;
import org.hibernate.search.util.json.internal.spi.JsonTreeValidation;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.BinaryNode;
import tools.jackson.databind.node.DecimalNode;
import tools.jackson.databind.node.DoubleNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.MissingNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.POJONode;

class JsonNodePolicyTest {
	@Test
	void duplicateReplacementAndInsertionOrderAndIndependentCopy() {
		var root = (ObjectNode) new JsonParsing().parse( "{\"z\":1,\"a\":null,\"z\":2,\"nested\":{\"array\":[1]}}" );
		assertThat( root.properties() ).extracting( entry -> entry.getKey() ).containsExactly( "z", "a", "nested" );
		assertThat( root.get( "z" ).intValue() ).isEqualTo( 2 );
		var copy = root.deepCopy();
		( (ArrayNode) copy.get( "nested" ).get( "array" ) ).add( 2 );
		assertThat( root.get( "nested" ).get( "array" ) ).hasSize( 1 );
		assertThat( root.get( "a" ).isNull() ).isTrue();
		assertThat( root.get( "missing" ) ).isNull();
	}

	@Test
	void scalarConstructionSnapshotsMutableNumbersAndKeepsExactValuesAndFloatingZero() {
		var mutable = new AtomicInteger( 1 );
		var node = JsonNumbers.fromNumber( mutable );
		mutable.set( 2 );
		assertThat( node.intValue() ).isEqualTo( 1 );
		assertThat( JsonNumbers.fromNumber( 9007199254740993L ).longValue() ).isEqualTo( 9007199254740993L );
		var integer = new BigInteger( "18446744073709551617" );
		assertThat( JsonNumbers.fromNumber( integer ).bigIntegerValue() ).isEqualTo( integer );
		var decimal = new BigDecimal( "1.00000000000000000000000000001" );
		assertThat( JsonNumbers.fromNumber( decimal ).decimalValue() ).isEqualByComparingTo( decimal );
		assertThat( Double.doubleToRawLongBits( JsonNumbers.fromNumber( -0.0d ).doubleValue() ) ).isEqualTo( Long.MIN_VALUE );
		assertThat( Float.floatToRawIntBits( JsonNumbers.fromNumber( -0.0f ).floatValue() ) ).isEqualTo( Integer.MIN_VALUE );
		assertThatThrownBy( () -> JsonNumbers.fromNumber( Double.NaN ) ).isInstanceOf( IllegalArgumentException.class );
		assertThatThrownBy( () -> JsonNumbers.fromNumber( Float.POSITIVE_INFINITY ) )
				.isInstanceOf( IllegalArgumentException.class );
	}

	@Test
	void customNumbersRequireStrictJsonGrammarBeforeExactConstruction() {
		for ( String text : new String[] { "+1", "01", "-01", ".5", "1.", "1e", "NaN", " 1", "1 " } ) {
			assertThatThrownBy( () -> JsonNumbers.fromNumber( new TextNumber( text ) ) )
					.as( text ).isInstanceOf( IllegalArgumentException.class );
		}
		for ( String text : new String[] { "0", "-1", "18446744073709551617", "1.2500", "1e+10", "-0.0" } ) {
			assertThat( JsonNumbers.fromNumber( new TextNumber( text ) ) )
					.as( text ).isEqualTo( new JsonParsing().parse( text ) );
		}
	}

	private static final class TextNumber extends Number {
		private final String text;

		private TextNumber(String text) {
			this.text = text;
		}

		@Override
		public int intValue() {
			throw new UnsupportedOperationException();
		}

		@Override
		public long longValue() {
			throw new UnsupportedOperationException();
		}

		@Override
		public float floatValue() {
			throw new UnsupportedOperationException();
		}

		@Override
		public double doubleValue() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return text;
		}
	}

	@Test
	void externalValidationAllowsSharedSubtreesAndRejectsCyclesCustomMissingBinaryAndPojo() {
		var child = new ArrayNode( JsonNodeFactory.instance ).add( 1 );
		var root = new ObjectNode( JsonNodeFactory.instance ).set( "a", child ).set( "b", child );
		JsonTreeValidation.validateExternal( root );
		assertThat( root.get( "a" ) ).isSameAs( root.get( "b" ) );
		child.add( root );
		assertThatThrownBy( () -> JsonTreeValidation.validateExternal( root ) ).isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "Cyclic" );
		for ( var illegal : new JsonNode[] {
				MissingNode.getInstance(),
				BinaryNode.valueOf( new byte[] { 1 } ),
				new POJONode( new Object() ),
				new ObjectNode( JsonNodeFactory.instance ) {},
				DoubleNode.valueOf( Double.NaN ) } ) {
			assertThatThrownBy( () -> JsonTreeValidation.validateExternal( illegal ) )
					.isInstanceOf( IllegalArgumentException.class );
		}
	}

	@Test
	void directExactNumbersAreNotLimitedByTheInputTokenCapButDecimalBoundsAreShared() {
		var huge = JsonNumbers.fromNumber( new BigInteger( "1" + "0".repeat( 4095 ) ) );
		JsonTreeValidation.validateExternal( huge );
		assertThatThrownBy( () -> new JsonParsing().parse( huge.numberValue().toString() ) )
				.isInstanceOf( RuntimeException.class );
		var invalid = DecimalNode.valueOf( new BigDecimal( BigInteger.ONE, Integer.MIN_VALUE ) );
		assertThatThrownBy( () -> JsonTreeValidation.validateExternal( invalid ) )
				.isInstanceOf( IllegalArgumentException.class );
		JsonTreeValidation.validateExternal( new JsonParsing().parse( "1e2147483647" ) );
		assertThatThrownBy( () -> new JsonParsing().parse( "10e2147483647" ) ).isInstanceOf( RuntimeException.class );
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void directContainerBackingCollectionsRequireStringNamesAndNonNullJsonValues() {
		Map raw = new HashMap();
		raw.put( null, IntNode.valueOf( 1 ) );
		assertThatThrownBy( () -> JsonTreeValidation.validateExternal( new ObjectNode( JsonNodeFactory.instance, raw ) ) )
				.isInstanceOf( IllegalArgumentException.class ).hasMessageContaining( "member name" );
		raw.clear();
		raw.put( 7, IntNode.valueOf( 1 ) );
		assertThatThrownBy( () -> JsonTreeValidation.validateExternal( new ObjectNode( JsonNodeFactory.instance, raw ) ) )
				.isInstanceOf( IllegalArgumentException.class ).hasMessageContaining( "member name" );
		raw.clear();
		raw.put( "value", null );
		assertThatThrownBy( () -> JsonTreeValidation.validateExternal( new ObjectNode( JsonNodeFactory.instance, raw ) ) )
				.isInstanceOf( IllegalArgumentException.class ).hasMessageContaining( "container value" );
		raw.put( "value", new Object() );
		assertThatThrownBy( () -> JsonTreeValidation.validateExternal( new ObjectNode( JsonNodeFactory.instance, raw ) ) )
				.isInstanceOf( IllegalArgumentException.class ).hasMessageContaining( "container value" );
		List broken = Arrays.asList( (Object) null );
		assertThatThrownBy( () -> JsonTreeValidation.validateExternal( new ArrayNode( JsonNodeFactory.instance, broken ) ) )
				.isInstanceOf( IllegalArgumentException.class ).hasMessageContaining( "container value" );
	}

	@Test
	void bigIntegerConversionIsValueBasedExactAndExpansionIsBounded() {
		assertThat( JsonNumbers.toBigInteger( new BigDecimal( "1e3" ) ) ).isEqualTo( new BigInteger( "1000" ) );
		assertThat( JsonNumbers.toBigInteger( new BigDecimal( "1.000" ) ) ).isEqualTo( BigInteger.ONE );
		assertThat( JsonNumbers.toBigInteger( new BigDecimal( "0e2147483647" ) ) ).isZero();
		assertThatThrownBy( () -> JsonNumbers.toBigInteger( new BigDecimal( "1.9" ) ) )
				.isInstanceOf( ArithmeticException.class );
		assertThatThrownBy( () -> JsonNumbers.toBigInteger( new BigDecimal( "1e100001" ) ) )
				.isInstanceOf( ArithmeticException.class ).hasMessageContaining( "expansion" );
	}

	@Test
	void truncatingAggregationKeysBoundExpansionWithoutRejectingFractions() {
		assertThat( JsonNumbers.truncateToBigInteger( new BigDecimal( "1.9" ) ) ).isEqualTo( BigInteger.ONE );
		assertThat( JsonNumbers.truncateToBigInteger( new BigDecimal( "-1.9" ) ) ).isEqualTo( BigInteger.ONE.negate() );
		assertThat( JsonNumbers.truncateToBigInteger( new BigDecimal( "1e-2147483647" ) ) ).isEqualTo( BigInteger.ZERO );
		assertThatThrownBy( () -> JsonNumbers.truncateToBigInteger( new BigDecimal( "1e100001" ) ) )
				.isInstanceOf( ArithmeticException.class );
	}
}
