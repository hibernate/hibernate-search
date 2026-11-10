package org.hibernate.search.util.json.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.SerializableString;
import tools.jackson.core.StreamWriteConstraints;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.io.CharacterEscapes;
import tools.jackson.core.io.SerializedString;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.databind.JsonNode;

/** Direct synchronous generator output, sharing the cursor byte representation. */
public final class JacksonJsonBodyWriter {
	private static final SerializedString LINE_SEPARATOR_ESCAPE = new SerializedString( "\\u2028" );
	private static final SerializedString PARAGRAPH_SEPARATOR_ESCAPE = new SerializedString( "\\u2029" );
	private static final CharacterEscapes REQUEST_ESCAPES = new CharacterEscapes() {
		private final int[] ascii = createAscii();

		private int[] createAscii() {
			int[] codes = standardAsciiEscapesForJSON();
			for ( char character : new char[] { '<', '>', '&', '=', '\'' } ) {
				codes[character] = ESCAPE_STANDARD;
			}
			return codes;
		}

		@Override
		public int[] getEscapeCodesForAscii() {
			return ascii;
		}

		@Override
		public SerializableString getEscapeSequence(int character) {
			return character == 0x2028 ? LINE_SEPARATOR_ESCAPE
					: character == 0x2029 ? PARAGRAPH_SEPARATOR_ESCAPE
					: null;
		}
	};
	private static final JsonFactory FACTORY = JsonFactory.builder()
			.streamWriteConstraints( StreamWriteConstraints.builder().maxNestingDepth( Integer.MAX_VALUE ).build() )
			.disable( StreamWriteFeature.AUTO_CLOSE_TARGET, StreamWriteFeature.FLUSH_PASSED_TO_STREAM )
			.disable( JsonWriteFeature.WRITE_HEX_UPPER_CASE )
			.characterEscapes( REQUEST_ESCAPES ).build();

	private JacksonJsonBodyWriter() {
	}

	/** Write approved Jackson nodes without reflection, diagnostic rendering, or a materialized body. */
	public static void write(JsonNode root, OutputStream destination) {
		JsonTreeValidator.validate( root );
		writeTree( root, destination );
	}

	/** Write a stable tree already validated at its external boundary. */
	public static void writeInternal(JsonNode root, OutputStream destination) throws IOException {
		try {
			writeTree( root, destination );
		}
		catch (JacksonException e) {
			if ( e.getCause() instanceof IOException failure ) {
				throw failure;
			}
			throw e;
		}
	}

	private static void writeTree(JsonNode root, OutputStream destination) {
		try ( JsonGenerator generator = FACTORY.createGenerator( ObjectWriteContext.empty(), destination ) ) {
			ArrayDeque<Iterator<?>> frames = new ArrayDeque<>();
			JsonNode value = root;
			while ( true ) {
				if ( value.isObject() ) {
					generator.writeStartObject();
					frames.push( value.properties().iterator() );
				}
				else if ( value.isArray() ) {
					generator.writeStartArray();
					frames.push( value.iterator() );
				}
				else if ( value.isNull() ) {
					generator.writeNull();
				}
				else {
					JsonNode primitive = value;
					if ( primitive.isString() ) {
						generator.writeString( primitive.asString() );
					}
					else if ( primitive.isBoolean() ) {
						generator.writeBoolean( primitive.booleanValue() );
					}
					else {
						generator.writeNumber( JsonNumbers.render( primitive ) );
					}
				}
				while ( !frames.isEmpty() && !frames.peek().hasNext() ) {
					frames.pop();
					if ( generator.streamWriteContext().inArray() ) {
						generator.writeEndArray();
					}
					else {
						generator.writeEndObject();
					}
				}
				if ( frames.isEmpty() ) {
					break;
				}
				Object next = frames.peek().next();
				if ( next instanceof Map.Entry ) {
					Map.Entry<?, ?> member = (Map.Entry<?, ?>) next;
					generator.writeName( (String) member.getKey() );
					value = (JsonNode) member.getValue();
				}
				else {
					value = (JsonNode) next;
				}
			}
		}
	}
}
