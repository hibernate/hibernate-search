package org.hibernate.search.backend.elasticsearch.json.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.json.internal.spi.JsonParsing;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;

class JsonAccessorTest {

	private static final JsonAccessor<Long> STRICT = JsonAccessor.root().property( "parent" ).property( "value" ).asLong();

	private static final JsonAccessor<Long> ADAPTED = JsonAccessor.root().property( "parent" ).unwrapSingletonArrays()
			.property( "value" ).unwrapSingletonArrays().asLong();

	private static ObjectNode tree(String json) {
		return (ObjectNode) new JsonParsing().parse( json );
	}

	@Test
	void strictAtIntermediateAndTerminalLocations() {
		assertThat( STRICT.get( tree( "{\"parent\":{\"value\":42}}" ) ) ).contains( 42L );
		assertThatThrownBy( () -> STRICT.get( tree( "{\"parent\":[{\"value\":42}]}" ) ) )
				.isInstanceOf( UnexpectedJsonElementTypeException.class ).hasMessageContaining( ".parent(ObjectNode)" );
		assertThatThrownBy( () -> STRICT.get( tree( "{\"parent\":{\"value\":[42]}}" ) ) )
				.isInstanceOf( UnexpectedJsonElementTypeException.class )
				.hasMessageContaining( ".parent(ObjectNode).value(JsonNode(Long))" );
		assertThatThrownBy( () -> STRICT.get( tree( "{\"parent\":{\"value\":\"42\"}}" ) ) )
				.isInstanceOf( UnexpectedJsonElementTypeException.class );
	}

	@Test
	void explicitAdaptationAtBothLocations() {
		assertThat( ADAPTED.get( tree( "{\"parent\":[[{\"value\":[[42]]}]]}" ) ) ).contains( 42L );
		for ( String json : new String[] { "{\"parent\":[]}", "{\"parent\":[null]}", "{\"parent\":{\"value\":[[]]}}" } ) {
			assertThat( ADAPTED.get( tree( json ) ) ).isEmpty();
		}
		assertThatThrownBy( () -> ADAPTED.get( tree( "{\"parent\":[{},{}]}" ) ) ).isInstanceOf( IllegalStateException.class )
				.hasMessageContaining( ".parent" );
		assertThatThrownBy( () -> ADAPTED.get( tree( "{\"parent\":{\"value\":[1,2]}}" ) ) )
				.isInstanceOf( IllegalStateException.class ).hasMessageContaining( ".value" );
	}

	@Test
	void writesTraverseWrappersAndReplaceTheSelectedLocation() {
		ObjectNode root = tree( "{\"parent\":[[{\"value\":[[42]]}]]}" );
		ADAPTED.set( root, 7L );
		assertThat( JsonScalarConversions.equivalent( root, tree( "{\"parent\":[[{\"value\":7}]]}" ) ) ).isTrue();
		ADAPTED.set( root, null );
		assertThat( JsonScalarConversions.equivalent( root, tree( "{\"parent\":[[{\"value\":null}]]}" ) ) ).isTrue();
	}

	@Test
	void getOrCreatePreservesRawPresenceAndAdaptedAbsence() {
		AtomicInteger calls = new AtomicInteger();
		for ( String json : new String[] { "{}", "{\"parent\":null}", "{\"parent\":{\"value\":null}}" } ) {
			ObjectNode root = tree( json );
			assertThat( ADAPTED.getOrCreate( root, () -> {
				calls.incrementAndGet();
				return 9L;
			} ) ).isEqualTo( 9L );
			assertThat( JsonScalarConversions.equivalent( root, tree( "{\"parent\":{\"value\":9}}" ) ) ).isTrue();
		}
		assertThat( calls ).hasValue( 3 );
		for ( String json : new String[] { "{\"parent\":{\"value\":[]}}", "{\"parent\":{\"value\":[[null]]}}" } ) {
			ObjectNode root = tree( json );
			assertThat( ADAPTED.getOrCreate( root, () -> {
				throw new AssertionFailure( "Raw present value cannot call supplier" );
			} ) ).isNull();
			assertThat( JsonScalarConversions.equivalent( root, tree( json ) ) ).isTrue();
		}
		ObjectNode existing = tree( "{\"parent\":[[{\"value\":[[42]]}]]}" );
		assertThat( ADAPTED.getOrCreate( existing, () -> {
			throw new AssertionFailure( "Existing value" );
		} ) ).isEqualTo( 42 );
		assertThat( JsonScalarConversions.equivalent( existing, tree( "{\"parent\":[[{\"value\":[[42]]}]]}" ) ) ).isTrue();
	}

	@Test
	void absentAdaptedIntermediateWritesFailDescriptivelyWithoutReplacingWrappers() {
		for ( String json : new String[] { "{\"parent\":[]}", "{\"parent\":[null]}" } ) {
			ObjectNode root = tree( json );
			assertThatThrownBy( () -> ADAPTED.set( root, 1L ) ).isInstanceOf( IllegalStateException.class )
					.hasMessageContaining( "Cannot traverse an absent adapted parent" ).hasMessageContaining( ".parent" );
			assertThatThrownBy( () -> ADAPTED.getOrCreate( root, () -> 1L ) ).isInstanceOf( IllegalStateException.class );
			assertThat( JsonScalarConversions.equivalent( root, tree( json ) ) ).isTrue();
		}
	}

	@Test
	void nullMissingArrayIdentityIndexCreationAndReuse() {
		ObjectNode root = tree( "{\"value\":null,\"array\":[[1]]}" );
		assertThat( JsonAccessor.root().property( "missing" ).get( root ) ).isEmpty();
		assertThat( JsonAccessor.root().property( "value" ).get( root ) ).contains( NullNode.instance );
		assertThat( JsonAccessor.root().property( "array" ).asArray().get( root ) )
				.containsSame( (ArrayNode) root.get( "array" ) );
		JsonAccessor<String> indexed = JsonAccessor.root().property( "created" ).element( 2 ).property( "name" ).asString();
		indexed.set( root, "first" );
		assertThat( root.get( "created" ) ).isEqualTo( new JsonParsing().parse( "[null,null,{\"name\":\"first\"}]" ) );
		ObjectNode second = new ObjectNode( JsonNodeFactory.instance );
		indexed.set( second, "second" );
		assertThat( indexed.get( root ) ).contains( "first" );
		assertThat( indexed.get( second ) ).contains( "second" );
		assertThat( JsonAccessor.root().property( "array" ).element( 5 ).get( root ) ).isEmpty();
		assertThatThrownBy( () -> JsonAccessor.root().property( "array" ).element( -1 ).get( root ) )
				.isInstanceOf( IndexOutOfBoundsException.class );
	}

	@Test
	void scalarTypesAndNumericNarrowingAreIndependentOfArrayAdaptation() {
		ObjectNode root = tree( "{\"parent\":{\"value\":1.9},\"integer\":4294967297,\"text\":\"123\",\"boolean\":true}" );
		assertThat( STRICT.get( root ) ).contains( 1L );
		assertThat( JsonAccessor.root().property( "integer" ).asInteger().get( root ) ).contains( 1 );
		assertThat( JsonElementTypes.BIG_INTEGER.fromElement( root.get( "text" ) ) )
				.isEqualTo( new BigInteger( "123" ) );
		assertThat( JsonAccessor.root().property( "boolean" ).asBoolean().get( root ) ).contains( true );
		assertThatThrownBy( () -> JsonAccessor.root().property( "boolean" ).asString().get( root ) )
				.isInstanceOf( UnexpectedJsonElementTypeException.class );
		assertThat( JsonAccessor.root().property( "integer" ).asFloat().get( root ) ).contains( 4294967297F );
		assertThat( JsonAccessor.root().property( "integer" ).asDouble().get( root ) ).contains( 4294967297D );
	}

	@Test
	void rootAndArrayMutationContracts() {
		assertThatThrownBy( () -> JsonAccessor.root().get( null ) ).isInstanceOf( AssertionFailure.class );
		assertThatThrownBy( () -> JsonAccessor.root().set( new ObjectNode( JsonNodeFactory.instance ),
				new ObjectNode( JsonNodeFactory.instance ) ) ).isInstanceOf( UnsupportedOperationException.class );
		ObjectNode root = new ObjectNode( JsonNodeFactory.instance );
		var array = JsonAccessor.root().property( "array" ).asArray();
		array.addElementIfAbsent( root, JsonScalarConversions.toNode( 1 ) );
		array.addElementIfAbsent( root, JsonScalarConversions.toNode( 1.0 ) );
		assertThat( (ArrayNode) root.get( "array" ) ).hasSize( 1 );
		ObjectNode absent = tree( "{\"array\":[]}" );
		assertThatThrownBy( () -> JsonAccessor.root().property( "array" ).unwrapSingletonArrays().asArray()
				.addElementIfAbsent( absent, JsonScalarConversions.toNode( 1 ) ) ).isInstanceOf( IllegalStateException.class )
				.hasMessageContaining( "Cannot modify an absent adapted array" );
		assertThat( JsonScalarConversions.equivalent( absent, tree( "{\"array\":[]}" ) ) ).isTrue();
		JsonAccessor.root().property( "" ).asString().set( root, "empty name" );
		assertThat( root.get( "" ).asString() ).isEqualTo( "empty name" );
	}
}
