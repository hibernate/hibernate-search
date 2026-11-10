package org.hibernate.search.util.json.impl;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.hibernate.search.util.json.internal.spi.JsonParserSession;
import org.hibernate.search.util.json.internal.spi.JsonParsingOptions;
import org.hibernate.search.util.json.logging.impl.JsonLog;
import org.hibernate.search.util.json.spi.JsonParseException;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.StringNode;

public final class JacksonJsonParserFactory {
	private final ResumableJsonFactory factory;
	private final JsonParsingOptions options;

	public JacksonJsonParserFactory() {
		this( JsonParsingOptions.DEFAULT );
	}

	public JacksonJsonParserFactory(JsonParsingOptions options) {
		this.options = Objects.requireNonNull( options );
		factory = new ResumableJsonFactory( JsonFactory.builder().streamReadConstraints( StreamReadConstraints.builder()
				.maxNestingDepth( options.maxDepth() ).maxStringLength( options.maxStringLength() )
				.maxNameLength( options.maxNameLength() ).maxNumberLength( options.maxNumberLength() )
				.maxDocumentLength( -1 ).maxTokenCount( -1 ).build() ).build() );
	}

	/** Character input is already owned/materialized; Core retains it only for this synchronous call. */
	public JsonNode parse(String json) {
		if ( options.maxInputBytes() == 0 ) {
			validateUtf16( json );
		}
		else if ( exceedsUtf8ByteLimit( json, options.maxInputBytes() ) ) {
			throw JsonLog.INSTANCE.inputByteLimitExceeded( options.maxInputBytes() );
		}
		JsonTreeBuilder builder = new JsonTreeBuilder();
		try ( JsonParser parser = factory.createParser( ObjectReadContext.empty(), json ) ) {
			JsonToken token;
			while ( ( token = parser.nextToken() ) != null ) {
				switch ( token ) {
					case START_OBJECT:
						builder.startObject();
						break;
					case START_ARRAY:
						builder.startArray();
						break;
					case END_OBJECT:
					case END_ARRAY:
						builder.endContainer();
						break;
					case PROPERTY_NAME:
						String name = parser.currentName();
						// The async parser constrains names by decoded UTF-8 byte length, not UTF-16 units.
						if ( exceedsUtf8ByteLimit( name, options.maxNameLength() ) ) {
							throw JsonLog.INSTANCE.nameLengthLimitExceeded( options.maxNameLength() );
						}
						builder.name( name );
						break;
					case VALUE_STRING:
						String value = parser.getString();
						factory.streamReadConstraints().validateStringLength( value.length() );
						builder.add( StringNode.valueOf( value ) );
						break;
					case VALUE_NUMBER_INT:
						builder.add( JsonNumbers.fromIntegerToken( parser.getString() ) );
						break;
					case VALUE_NUMBER_FLOAT:
						builder.add( JsonNumbers.fromFloatingToken( parser.getString() ) );
						break;
					case VALUE_TRUE:
						builder.add( BooleanNode.TRUE );
						break;
					case VALUE_FALSE:
						builder.add( BooleanNode.FALSE );
						break;
					case VALUE_NULL:
						builder.add( NullNode.instance );
						break;
					default:
						throw JsonLog.INSTANCE.unexpectedToken( String.valueOf( token ) );
				}
			}
			return builder.finish();
		}
		catch (JsonParseException e) {
			throw e;
		}
		catch (RuntimeException e) {
			throw JsonLog.INSTANCE.invalidInput( e.getMessage(), e );
		}
		finally {
			builder.clear();
		}
	}

	private static void validateUtf16(String text) {
		int index = 0;
		while ( index < text.length() ) {
			char character = text.charAt( index++ );
			if ( Character.isSurrogate( character ) ) {
				if ( !Character.isHighSurrogate( character )
						|| index == text.length()
						|| !Character.isLowSurrogate( text.charAt( index++ ) ) ) {
					throw JsonLog.INSTANCE.invalidUnicodeInput();
				}
			}
		}
	}

	/**
	 * Core's character parser accepts raw unpaired surrogates and constrains names in UTF-16 units.
	 * This scan retains strict Unicode and byte-limit parity with async input without allocating UTF-8.
	 * Stop as soon as a limit is exceeded: the caller rejects the whole input in that case.
	 */
	private static boolean exceedsUtf8ByteLimit(String text, long maxBytes) {
		// Even ASCII needs one UTF-8 byte per UTF-16 unit; longer input can be rejected immediately.
		if ( text.length() > maxBytes ) {
			return true;
		}
		long result = 0;
		int index = 0;
		while ( index < text.length() ) {
			char character = text.charAt( index++ );
			if ( character < 0x80 ) {
				result++;
			}
			else if ( character < 0x800 ) {
				result += 2;
			}
			else if ( Character.isHighSurrogate( character )
					&& index < text.length()
					&& Character.isLowSurrogate( text.charAt( index ) ) ) {
				index++;
				result += 4;
			}
			else if ( Character.isSurrogate( character ) ) {
				throw JsonLog.INSTANCE.invalidUnicodeInput();
			}
			else {
				result += 3;
			}
			if ( result > maxBytes ) {
				return true;
			}
		}
		return false;
	}

	public JsonParserSession createParser() {
		try {
			return new JacksonJsonParserSession( factory.createResumableByteBufferParser( ObjectReadContext.empty() ),
					options );
		}
		catch (RuntimeException e) {
			throw JsonLog.INSTANCE.cannotCreateParser( e );
		}
	}

	public JsonParserSession createParser(Charset charset) {
		Objects.requireNonNull( charset );
		if ( StandardCharsets.UTF_8.equals( charset ) ) {
			return createParser();
		}
		return new TranscodingJsonParserSession( new JacksonJsonParserFactory( options.withMaxInputBytes( 0 ) ).createParser(),
				charset, options );
	}
}
