package org.hibernate.search.backend.elasticsearch.json.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.json.internal.spi.JsonParsing;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;

class JsonAccessorCharacterizationTest {

	private static final JsonAccessor<Long> VALUE = JsonAccessor.root().property( "parent" ).unwrapSingletonArrays()
			.property( "value" ).unwrapSingletonArrays().asLong();

	@Test
	void intermediateAndTerminalSingletonArrays() {
		assertThat( VALUE.get( tree( "{\"parent\":[[{\"value\":[[42]]}]]}" ) ) ).contains( 42L );
		assertThat( VALUE.get( tree( "{\"parent\":[]}" ) ) ).isEmpty();
		assertThat( VALUE.get( tree( "{\"parent\":[null]}" ) ) ).isEmpty();
		assertThat( VALUE.get( tree( "{\"parent\":{\"value\":[[]]}}" ) ) ).isEmpty();
		assertThatThrownBy( () -> VALUE.get( tree( "{\"parent\":[{},{}]}" ) ) ).isInstanceOf( IllegalStateException.class )
				.hasMessageContaining( ".parent(unwrapSingletonArrays)" );
		assertThatThrownBy( () -> VALUE.get( tree( "{\"parent\":{\"value\":[1,2]}}" ) ) )
				.isInstanceOf( IllegalStateException.class )
				.hasMessageContaining( ".parent(unwrapSingletonArrays)(ObjectNode).value(unwrapSingletonArrays)" );
	}

	@Test
	void writesTraverseSingletonObjectsButReplaceSelectedScalarLocation() {
		ObjectNode root = tree( "{\"parent\":[[{\"value\":[[42]]}]]}" );
		VALUE.set( root, 7L );
		assertThat( JsonScalarConversions.equivalent( root, tree( "{\"parent\":[[{\"value\":7}]]}" ) ) ).isTrue();
		VALUE.set( root, null );
		assertThat( JsonScalarConversions.equivalent( root, tree( "{\"parent\":[[{\"value\":null}]]}" ) ) ).isTrue();
	}

	@Test
	void creationDistinguishesRawNullFromAdaptedAbsence() {
		AtomicInteger calls = new AtomicInteger();
		for ( String json : new String[] { "{}", "{\"parent\":null}", "{\"parent\":{\"value\":null}}" } ) {
			ObjectNode root = tree( json );
			assertThat( VALUE.get( root ) ).isEmpty();
			assertThat( VALUE.getOrCreate( root, () -> {
				calls.incrementAndGet();
				return 9L;
			} ) ).isEqualTo( 9L );
			assertThat( JsonScalarConversions.equivalent( root, tree( "{\"parent\":{\"value\":9}}" ) ) ).isTrue();
		}
		assertThat( calls.get() ).isEqualTo( 3 );
		ObjectNode emptyTerminal = tree( "{\"parent\":{\"value\":[]}}" );
		assertThat( VALUE.getOrCreate( emptyTerminal, () -> {
			calls.incrementAndGet();
			return 10L;
		} ) ).isNull();
		assertThat( calls.get() ).isEqualTo( 3 );
		assertThat( JsonScalarConversions.equivalent( emptyTerminal, tree( "{\"parent\":{\"value\":[]}}" ) ) ).isTrue();
		// An existing empty/null singleton array is not replaced to create an intermediate object.
		for ( String json : new String[] { "{\"parent\":[]}", "{\"parent\":[null]}" } ) {
			assertThatThrownBy( () -> VALUE.set( tree( json ), 1L ) ).isInstanceOf( IllegalStateException.class );
			assertThatThrownBy( () -> VALUE.getOrCreate( tree( json ), () -> 1L ) ).isInstanceOf( IllegalStateException.class );
		}
	}

	@Test
	void getOrCreatePreservesExistingAdaptedContainer() {
		ObjectNode root = tree( "{\"parent\":[[{\"value\":[[42]]}]]}" );
		assertThat( VALUE.getOrCreate( root, () -> {
			throw new AssertionFailure( "Existing value must not call supplier" );
		} ) ).isEqualTo( 42L );
		assertThat( JsonScalarConversions.equivalent( root, tree( "{\"parent\":[[{\"value\":[[42]]}]]}" ) ) ).isTrue();
		ObjectNode nullSingleton = tree( "{\"parent\":{\"value\":[[null]]}}" );
		assertThat( VALUE.getOrCreate( nullSingleton, () -> {
			throw new AssertionFailure( "Existing null singleton must not call supplier" );
		} ) ).isNull();
		assertThat( JsonScalarConversions.equivalent( nullSingleton, tree( "{\"parent\":{\"value\":[[null]]}}" ) ) ).isTrue();
	}

	@Test
	void rawNullArrayIdentityAndIndexedCreation() {
		ObjectNode root = tree( "{\"value\":null,\"array\":[[1]]}" );
		assertThat( JsonAccessor.root().property( "missing" ).get( root ) ).isEmpty();
		assertThat( JsonAccessor.root().property( "value" ).get( root ) ).containsSame( NullNode.instance );
		ArrayNode array = (ArrayNode) root.get( "array" );
		assertThat( JsonAccessor.root().property( "array" ).asArray().get( root ) ).containsSame( array );
		JsonAccessor.root().property( "created" ).element( 2 ).unwrapSingletonArrays().property( "name" )
				.unwrapSingletonArrays().asString().set( root, "x" );
		assertThat( root.get( "created" ) ).isEqualTo( new JsonParsing().parse( "[null,null,{\"name\":\"x\"}]" ) );
		assertThat( JsonAccessor.root().property( "array" ).element( 10 ).get( root ) ).isEmpty();
		assertThatThrownBy( () -> JsonAccessor.root().property( "array" ).element( -1 ).get( root ) )
				.isInstanceOf( IndexOutOfBoundsException.class );
	}

	@Test
	void numericNarrowingAndTypeChecks() {
		ObjectNode root = tree( "{\"parent\":{\"value\":1.9},\"big\":4294967297,\"string\":\"42\",\"boolean\":true}" );
		assertThat( VALUE.get( root ) ).contains( 1L );
		assertThat( JsonAccessor.root().property( "big" ).unwrapSingletonArrays().asInteger().get( root ) ).contains( 1 );
		assertThatThrownBy( () -> JsonAccessor.root().property( "string" ).unwrapSingletonArrays().asLong().get( root ) )
				.isInstanceOf( UnexpectedJsonElementTypeException.class );
		assertThatThrownBy( () -> JsonAccessor.root().property( "boolean" ).unwrapSingletonArrays().asString().get( root ) )
				.isInstanceOf( UnexpectedJsonElementTypeException.class );
		assertThat( JsonElementTypes.BIG_INTEGER.fromElement( JsonScalarConversions.toNode( "42" ) ) ).isEqualTo( 42 );
	}

	@Test
	void definitionsAreReusableAcrossTrees() {
		ObjectNode first = new ObjectNode( JsonNodeFactory.instance );
		ObjectNode second = new ObjectNode( JsonNodeFactory.instance );
		VALUE.set( first, 1L );
		VALUE.set( second, 2L );
		assertThat( VALUE.get( first ) ).contains( 1L );
		assertThat( VALUE.get( second ) ).contains( 2L );
	}

	@Test
	void rootAndWriteErrors() {
		assertThatThrownBy( () -> JsonAccessor.root().get( null ) ).isInstanceOf( AssertionFailure.class )
				.hasMessageContaining( "null root" );
		assertThatThrownBy( () -> JsonAccessor.root().set( new ObjectNode( JsonNodeFactory.instance ),
				new ObjectNode( JsonNodeFactory.instance ) ) ).isInstanceOf( UnsupportedOperationException.class );
		assertThatThrownBy( () -> VALUE.set( tree( "{\"parent\":42}" ), 1L ) )
				.isInstanceOf( UnexpectedJsonElementTypeException.class )
				.hasMessageContaining( ".parent(unwrapSingletonArrays)" );
	}

	private static ObjectNode tree(String json) {
		return (ObjectNode) new JsonParsing().parse( json );
	}
}
