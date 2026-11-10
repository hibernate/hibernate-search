package org.hibernate.search.backend.elasticsearch.json.internal.spi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchMiscLog;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.json.internal.spi.JsonBodies;
import org.hibernate.search.util.json.internal.spi.JsonCodec;
import org.hibernate.search.util.json.internal.spi.JsonCodecRegistry;
import org.hibernate.search.util.json.internal.spi.JsonCodecs;
import org.hibernate.search.util.json.internal.spi.JsonNumbers;
import org.hibernate.search.util.json.internal.spi.JsonParserSession;
import org.hibernate.search.util.json.internal.spi.JsonParsing;
import org.hibernate.search.util.json.internal.spi.JsonParsingOptions;
import org.hibernate.search.util.json.internal.spi.JsonValidation;
import org.hibernate.search.util.json.spi.JsonEmptyInputException;
import org.hibernate.search.util.json.spi.JsonParseException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

/**
 * Internal backend JSON services using explicit known-type codecs and bounded byte input.
 * JsonNode arguments must be stable ordinary acyclic trees produced by parsing/codecs or validated at
 * their external boundary. Custom clients accepting arbitrary trees must validate them first with
 * JsonTreeValidation.validateExternal; this service does not establish trust based on node type.
 */
@Incubating
public final class JsonCodecService {
	private static final int TEXT_BUFFER_SIZE = 8192;

	private final JsonCodecRegistry registry;
	private final JsonValidation validation;
	private final boolean serializeNulls;
	private final boolean pretty;
	private final JsonParsing parsing;

	public JsonCodecService(JsonCodecRegistry registry, boolean serializeNulls, boolean pretty) {
		this( registry, serializeNulls, pretty, JsonValidation.systemDefault() );
	}

	public JsonCodecService(JsonCodecRegistry registry, boolean serializeNulls, boolean pretty, JsonValidation validation) {
		this( registry, serializeNulls, pretty, validation, new JsonParsing() );
	}

	public JsonCodecService(JsonCodecRegistry registry, boolean serializeNulls, boolean pretty, JsonValidation validation,
			JsonParsing parsing) {
		this.parsing = Objects.requireNonNull( parsing );
		this.validation = Objects.requireNonNull( validation );
		this.registry = registry;
		this.serializeNulls = serializeNulls;
		this.pretty = pretty;
	}

	public JsonValidation validation() {
		return validation;
	}

	public JsonParsing parsing() {
		return parsing;
	}

	public <T> JsonCodec<T> getCodec(Class<T> type) {
		return registry.get( type );
	}

	public <T> T decode(String value, Class<T> type) {
		return value == null ? null : decode( parsing.parse( value ), type );
	}

	public <T> T decode(InputStream input, Class<T> type) throws IOException {
		return decode( input, StandardCharsets.UTF_8, type );
	}

	public <T> T decode(InputStream input, Charset charset, Class<T> type) throws IOException {
		try ( JsonParserSession parser = parsing.createParser( charset ) ) {
			byte[] buffer = new byte[JsonParsingOptions.DEFAULT_MAX_INPUT_CHUNK_SIZE];
			int count;
			boolean present = false;
			while ( ( count = input.read( buffer ) ) != -1 ) {
				if ( count == 0 ) {
					continue;
				}
				present = true;
				ByteBuffer bytes = ByteBuffer.wrap( buffer, 0, count );
				while ( bytes.hasRemaining() ) {
					parser.feed( bytes );
				}
			}
			if ( !present ) {
				return null;
			}
			try {
				return decode( parser.endOfInput(), type );
			}
			catch (JsonEmptyInputException e) {
				return null;
			}
		}
	}

	public <T> T decode(JsonNode value, Class<T> type) {
		if ( value == null ) {
			return null;
		}
		if ( type == JsonNode.class ) {
			return type.cast( value );
		}
		if ( value.isNull() ) {
			return null;
		}
		JsonCodec<T> codec = type == ObjectNode.class || type == ArrayNode.class ? null : registry.get( type );
		try {
			if ( type == ObjectNode.class ) {
				return type.cast( (ObjectNode) value );
			}
			if ( type == ArrayNode.class ) {
				return type.cast( (ArrayNode) value );
			}
			return codec.decode( value );
		}
		catch (JsonParseException e) {
			throw e;
		}
		catch (IllegalArgumentException | IllegalStateException | ClassCastException e) {
			throw ElasticsearchMiscLog.INSTANCE.cannotDecodeJson( type.getName(), e.getMessage(), e );
		}
	}

	@SuppressWarnings("unchecked")
	public JsonNode encode(Object value) {
		if ( value == null ) {
			return NullNode.instance;
		}
		if ( value instanceof JsonNode element ) {
			validation.validateInternal( element );
			return serializeNulls ? element : JsonCodecs.copy( element, false );
		}
		if ( value instanceof String string ) {
			return StringNode.valueOf( string );
		}
		if ( value instanceof Character character ) {
			return StringNode.valueOf( character.toString() );
		}
		if ( value instanceof Boolean bool ) {
			return BooleanNode.valueOf( bool );
		}
		if ( value instanceof Number number ) {
			return JsonNumbers.fromNumber( number );
		}
		return ( (JsonCodec<Object>) registry.get( value.getClass() ) ).encode( value, serializeNulls );
	}

	/** Explicit diagnostic/raw-projection String output. */
	public String write(Object value) {
		StringWriter writer = new StringWriter();
		try {
			write( value, writer );
		}
		catch (IOException e) {
			throw ElasticsearchMiscLog.INSTANCE.cannotWriteJsonToString( e );
		}
		return writer.toString();
	}

	/** Streaming formatted output used by schema exports. */
	public void write(Object value, Writer writer) throws IOException {
		JsonNode tree = encode( value );
		if ( pretty ) {
			validation.validateInternal( tree );
			render( tree, writer, 0 );
		}
		else {
			compact( tree, writer );
		}
	}

	/** Compact streaming UTF-8 output, preserving schema export's file format. */
	public void writeCompact(Object value, OutputStream output) throws IOException {
		JsonBodies.ofInternal( encode( value ), validation ).writeTo( output );
	}

	private void render(JsonNode value, Writer writer, int depth) throws IOException {
		if ( value instanceof ObjectNode object ) {
			writer.write( '{' );
			int i = 0;
			for ( var e : object.properties() ) {
				if ( i++ > 0 ) {
					writer.write( ',' );
				}
				indent( writer, depth + 1 );
				compact( StringNode.valueOf( e.getKey() ), writer );
				writer.write( pretty ? ": " : ":" );
				render( e.getValue(), writer, depth + 1 );
			}
			if ( i > 0 ) {
				indent( writer, depth );
			}
			writer.write( '}' );
		}
		else if ( value instanceof ArrayNode array ) {
			writer.write( '[' );
			int i = 0;
			for ( var e : array ) {
				if ( i++ > 0 ) {
					writer.write( ',' );
				}
				indent( writer, depth + 1 );
				render( e, writer, depth + 1 );
			}
			if ( i > 0 ) {
				indent( writer, depth );
			}
			writer.write( ']' );
		}
		else {
			compact( value, writer );
		}
	}

	private void compact(JsonNode value, Writer writer) throws IOException {
		try ( var input = JsonBodies.ofInternal( value, validation ).openStream();
				var reader = new InputStreamReader( input, StandardCharsets.UTF_8 ) ) {
			char[] buffer = new char[TEXT_BUFFER_SIZE];
			int count;
			while ( ( count = reader.read( buffer ) ) != -1 ) {
				writer.write( buffer, 0, count );
			}
		}
	}

	private void indent(Writer writer, int depth) throws IOException {
		if ( pretty ) {
			writer.write( '\n' );
			for ( int i = 0; i < depth; i++ ) {
				writer.write( "  " );
			}
		}
	}
}
