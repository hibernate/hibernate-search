package org.hibernate.search.backend.elasticsearch.json.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.search.backend.elasticsearch.gson.GsonJsonHandlers;
import org.hibernate.search.backend.elasticsearch.json.jackson.JacksonJsonHandlers;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.json.JsonHandler;
import org.hibernate.search.util.json.internal.spi.JsonValidation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.gson.JsonObject;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.BinaryNode;
import tools.jackson.databind.node.DoubleNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.POJONode;

class JsonHandlerBridgeTest {
	@Test
	void handlerExportsUseTheirScopedValidationPolicy() {
		ObjectNode internal = JsonNodeFactory.instance.objectNode().put( "invalid", Double.NaN );
		var disabled = JsonValidation.of( false );
		var enabled = JsonValidation.of( true );
		assertThat( JsonHandlerBridge.exportObject( JacksonJsonHandlers.object(), internal, "test", disabled ) )
				.isSameAs( internal );
		assertThatThrownBy( () -> JsonHandlerBridge.exportObject( JacksonJsonHandlers.object(), internal, "test", enabled ) )
				.isInstanceOf( SearchException.class );
		assertThat( JsonHandlerBridge.exportObject( JacksonJsonHandlers.object(), internal, "test", disabled ) )
				.isSameAs( internal );
		assertThatThrownBy( () -> JsonHandlerBridge.importObject( JacksonJsonHandlers.object(), internal, "test" ) )
				.isInstanceOf( SearchException.class );
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{}",
			"[1,null,{\"nested\":null}]",
			"true",
			"\"text\"",
			"null",
			"9007199254740993",
			"123456789012345678901234567890",
			"0.123456789012345678901234567890",
			"-0.0" })
	void builtInPathsPreserveValues(String text) {
		JsonNode node = JacksonJsonHandlers.value().fromJson( text );
		assertThat( JsonHandlerBridge.importValue( JacksonJsonHandlers.value(), node, "test import" ) ).isSameAs( node );
		assertThat( JsonHandlerBridge.exportValue( JacksonJsonHandlers.value(), node, "test export" ) ).isSameAs( node );
		var gson = GsonJsonHandlers.value().fromJson( text );
		JsonNode imported = JsonHandlerBridge.importValue( GsonJsonHandlers.value(), gson, "test import" );
		assertThat( imported ).isEqualTo( node );
		var exported = JsonHandlerBridge.exportValue( GsonJsonHandlers.value(), imported, "test export" );
		assertThat( JsonHandlerBridge.importValue( GsonJsonHandlers.value(), exported, "test reimport" ) ).isEqualTo( node );
		if ( node.isArray() ) {
			assertThat( imported.get( 1 ).isNull() ).isTrue();
		}
		if ( node.isDouble() ) {
			assertThat( Double.doubleToRawLongBits( imported.doubleValue() ) ).isEqualTo( Long.MIN_VALUE );
		}
	}

	@Test
	void customJacksonObjectAndValueHandlersAreNeverBypassed() {
		AtomicInteger imports = new AtomicInteger();
		AtomicInteger exports = new AtomicInteger();
		JsonHandler<ObjectNode> custom = new JsonHandler<>() {
			@Override
			public Class<ObjectNode> javaType() {
				return ObjectNode.class;
			}

			@Override
			public String toJson(ObjectNode value) {
				imports.incrementAndGet();
				return JacksonJsonHandlers.object().toJson( value.deepCopy().put( "customImport", true ) );
			}

			@Override
			public ObjectNode fromJson(String json) {
				exports.incrementAndGet();
				return JacksonJsonHandlers.object().fromJson( json ).put( "customExport", true );
			}
		};
		ObjectNode original = JacksonJsonHandlers.object().fromJson( "{\"unknown\":[null,true]}" );
		assertThat( JsonHandlerBridge.isTrusted( custom ) ).isFalse();
		ObjectNode imported = JsonHandlerBridge.importObject( custom, original, "custom import" );
		assertThat( imported.path( "customImport" ).booleanValue() ).isTrue();
		assertThat( JsonHandlerBridge.exportObject( custom, imported, "custom export" ).path( "customExport" ).booleanValue() )
				.isTrue();
		assertThat( imports.get() ).isOne();
		assertThat( exports.get() ).isOne();
		assertThat( original.has( "customImport" ) ).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = { "[]", "null", "1", "true", "\"text\"" })
	void objectEndpointsRejectPresentNonObjects(String json) {
		JsonNode value = JacksonJsonHandlers.value().fromJson( json );
		assertThatThrownBy( () -> JsonHandlerBridge.importObject( JacksonJsonHandlers.value(), value, "predicate import" ) )
				.isInstanceOf( SearchException.class ).hasMessageContaining( "predicate import" );
		assertThatThrownBy( () -> JsonHandlerBridge.exportObject( JacksonJsonHandlers.value(), value, "source export" ) )
				.isInstanceOf( SearchException.class ).hasMessageContaining( "source export" );
		assertThat( JsonHandlerBridge.exportObject( JacksonJsonHandlers.value(), null, "missing source" ) ).isNull();
	}

	@Test
	void directPathRejectsNonJsonNodesAndCyclesButAllowsSharedAcyclicChildren() {
		ObjectNode shared = JsonNodeFactory.instance.objectNode().put( "member", true );
		ObjectNode root = JsonNodeFactory.instance.objectNode();
		root.set( "a", shared );
		root.set( "b", shared );
		assertThat( JsonHandlerBridge.importObject( JacksonJsonHandlers.object(), root, "shared" ) ).isSameAs( root );
		Map<String, JsonNode> malformedMembers = new LinkedHashMap<>();
		malformedMembers.put( null, JsonNodeFactory.instance.nullNode() );
		for ( JsonNode bad : new JsonNode[] {
				new ObjectNode( JsonNodeFactory.instance, malformedMembers ),
				new POJONode( new Object() ),
				BinaryNode.valueOf( new byte[] { 1 } ),
				DoubleNode.valueOf( Double.NaN ),
				new ObjectNode( JsonNodeFactory.instance ) {} } ) {
			root.set( "bad", bad );
			assertThatThrownBy( () -> JsonHandlerBridge.importObject( JacksonJsonHandlers.object(), root, "external tree" ) )
					.isInstanceOf( SearchException.class );
		}
		root.remove( "bad" );
		root.set( "cycle", root );
		assertThatThrownBy( () -> JsonHandlerBridge.importObject( JacksonJsonHandlers.object(), root, "external tree" ) )
				.isInstanceOf( SearchException.class );
	}

	@Test
	void preciseValuesAreNotConvertedThroughDouble() {
		JsonNode values = JsonHandlerBridge.importValue( GsonJsonHandlers.value(),
				GsonJsonHandlers.value().fromJson(
						"[9007199254740993,123456789012345678901234567890,0.123456789012345678901234567890]" ),
				"numbers" );
		assertThat( values.get( 0 ).bigIntegerValue() ).isEqualTo( new BigInteger( "9007199254740993" ) );
		assertThat( values.get( 1 ).bigIntegerValue() ).isEqualTo( new BigInteger( "123456789012345678901234567890" ) );
		assertThat( values.get( 2 ).decimalValue() ).isEqualTo( new BigDecimal( "0.123456789012345678901234567890" ) );
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void runtimeTypeAndFailureCauseAreCheckedAtTheBoundary() {
		JsonHandler raw = JacksonJsonHandlers.value();
		assertThatThrownBy( () -> JsonHandlerBridge.importValue( raw, new JsonObject(), "wrong type" ) )
				.isInstanceOf( SearchException.class ).hasCauseInstanceOf( ClassCastException.class );
		IllegalStateException cause = new IllegalStateException( "handler failed" );
		JsonHandler<JsonNode> custom = new JsonHandler<>() {
			@Override
			public Class<JsonNode> javaType() {
				return JsonNode.class;
			}

			@Override
			public String toJson(JsonNode value) {
				throw cause;
			}

			@Override
			public JsonNode fromJson(String json) {
				throw cause;
			}
		};
		assertThatThrownBy( () -> JsonHandlerBridge.importValue( custom, JsonNodeFactory.instance.nullNode(), "indexing" ) )
				.isInstanceOf( SearchException.class ).hasCause( cause ).hasMessageContaining( "indexing" );
	}

	@Test
	void hugeExactNodesCanTransferDirectlyWhileTextFallbackImportsEnforceTokenLimit() {
		BigInteger integer = new BigInteger( "1".repeat( 1001 ) );
		JsonNode node = JsonNodeFactory.instance.numberNode( integer );
		assertThat( JsonHandlerBridge.importValue( JacksonJsonHandlers.value(), node, "huge direct import" ) ).isSameAs( node );
		assertThat( JsonHandlerBridge.exportValue( JacksonJsonHandlers.value(), node, "huge direct export" ) ).isSameAs( node );
		assertThat( node.bigIntegerValue() ).isEqualTo( integer );
		var gson = JsonHandlerBridge.exportValue( GsonJsonHandlers.value(), node, "huge Gson export" );
		assertThat( gson.getAsBigInteger() ).isEqualTo( integer );
		assertThatThrownBy( () -> JsonHandlerBridge.importValue( GsonJsonHandlers.value(), gson, "huge Gson import" ) )
				.isInstanceOf( SearchException.class ).hasMessageContaining( "huge Gson import" );
		AtomicInteger calls = new AtomicInteger();
		AtomicInteger exports = new AtomicInteger();
		JsonHandler<JsonNode> custom = new JsonHandler<>() {
			@Override
			public Class<JsonNode> javaType() {
				return JsonNode.class;
			}

			@Override
			public String toJson(JsonNode value) {
				calls.incrementAndGet();
				return JacksonJsonHandlers.value().toJson( value );
			}

			@Override
			public JsonNode fromJson(String json) {
				exports.incrementAndGet();
				throw new AssertionFailure( "Import must not invoke fromJson" );
			}
		};
		assertThatThrownBy( () -> JsonHandlerBridge.importValue( custom, node, "huge custom import" ) )
				.isInstanceOf( SearchException.class ).hasMessageContaining( "huge custom import" );
		assertThat( calls.get() ).isOne();
		assertThat( exports.get() ).isZero();
	}

}
