package org.hibernate.search.backend.elasticsearch.client.common.json.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

import org.hibernate.search.backend.elasticsearch.client.common.json.entity.spi.JsonGzipDecoder;
import org.hibernate.search.util.json.spi.JsonParseException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JsonResponseBodyTest {
	@Test
	void cancellationPreservesFailureAndDiagnosticReplayIsSafe() throws Exception {
		var original = new JsonParseException( "Original parse failure" );
		var body = new JsonResponseBody( StandardCharsets.UTF_8, 0 );
		body.feed( ByteBuffer.wrap( "{".getBytes( StandardCharsets.UTF_8 ) ) );
		body.failedParsing( original );
		body.close();
		body.close();
		assertThatThrownBy( body::tree ).isSameAs( original );
		try ( var stream = body.openDiagnosticStream() ) {
			assertThat( stream.readAllBytes() ).isEmpty();
		}
		var cancelled = new JsonResponseBody( StandardCharsets.UTF_8, 0 );
		cancelled.close();
		assertThatThrownBy( cancelled::tree ).hasMessageContaining( "cancelled" );
		try ( var stream = cancelled.openDiagnosticStream() ) {
			assertThat( stream.readAllBytes() ).isEmpty();
		}
	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 5, 8192 })
	void gzipFragmentationLargeScalarAndConcatenatedMembers(int size) throws Exception {
		String input = "{\"value\":\"" + "é😀".repeat( 100_000 ) + "\"}";
		byte[] bytes = input.getBytes( StandardCharsets.UTF_8 );
		ByteArrayOutputStream compressed = new ByteArrayOutputStream();
		try ( GZIPOutputStream stream = new GZIPOutputStream( compressed ) ) {
			stream.write( bytes, 0, bytes.length / 2 );
		}
		try ( GZIPOutputStream stream = new GZIPOutputStream( compressed ) ) {
			stream.write( bytes, bytes.length / 2, bytes.length - bytes.length / 2 );
		}
		try ( var body = new JsonResponseBody( StandardCharsets.UTF_8, 0 ); var decoder = new JsonGzipDecoder( body ) ) {
			byte[] gzip = compressed.toByteArray();
			for ( int offset = 0; offset < gzip.length; offset += size ) {
				ByteBuffer fragment = ByteBuffer.allocateDirect( Math.min( size, gzip.length - offset ) );
				fragment.put( gzip, offset, fragment.capacity() ).flip();
				decoder.feed( fragment );
				assertThat( fragment.hasRemaining() ).isFalse();
			}
			decoder.endOfInput();
			assertThat( body.inputBytes() ).isEqualTo( bytes.length );
			body.complete();
			assertThat( body.tree().asObject().get( "value" ).asString() ).isEqualTo( "é😀".repeat( 100_000 ) );
			try ( var diagnostic = body.openDiagnosticStream() ) {
				assertThat( new String( diagnostic.readAllBytes(), StandardCharsets.UTF_8 ) ).isEqualTo( input );
			}
		}
	}

	@Test
	void encodedRetentionIsBoundedAndReleased() throws Exception {
		var prefix = JsonResponseBody.class.getDeclaredField( "diagnosticPrefix" );
		prefix.setAccessible( true );
		var parser = JsonResponseBody.class.getDeclaredField( "parser" );
		parser.setAccessible( true );
		for ( int size : new int[] { 100, 10_000, 1_000_000 } ) {
			try ( var body = new JsonResponseBody( StandardCharsets.UTF_8, 0 ) ) {
				body.feed( ByteBuffer.wrap( "{\"value\":\"".getBytes( StandardCharsets.UTF_8 ) ) );
				byte[] chunk = new byte[8192];
				Arrays.fill( chunk, (byte) 'x' );
				for ( int count = 0; count < size; count += chunk.length ) {
					body.feed( ByteBuffer.wrap( chunk, 0, Math.min( chunk.length, size - count ) ) );
					assertThat( (byte[]) prefix.get( body ) ).hasSize( 8192 );
				}
				body.feed( ByteBuffer.wrap( "\"}".getBytes( StandardCharsets.UTF_8 ) ) );
				body.complete();
				assertThat( body.tree().asObject().get( "value" ).asString() ).hasSize( size );
				assertThat( prefix.get( body ) ).isNull();
				assertThat( parser.get( body ) ).isNull();
			}
			var cancelled = new JsonResponseBody( StandardCharsets.UTF_8, 0 );
			cancelled.feed( ByteBuffer.wrap( "{\"value\":\"".getBytes( StandardCharsets.UTF_8 ) ) );
			cancelled.close();
			assertThat( prefix.get( cancelled ) ).isNull();
			assertThat( parser.get( cancelled ) ).isNull();
		}
		var body = new JsonResponseBody( StandardCharsets.UTF_8, 0 );
		var gzip = new JsonGzipDecoder( body );
		var output = JsonGzipDecoder.class.getDeclaredField( "output" );
		output.setAccessible( true );
		assertThat( (byte[]) output.get( gzip ) ).hasSize( 8192 );
		gzip.close();
		body.close();
		assertThat( output.get( gzip ) ).isNull();
	}

	@Test
	void gzipChecksumsAndTruncation() throws Exception {
		ByteArrayOutputStream compressed = new ByteArrayOutputStream();
		try ( GZIPOutputStream stream = new GZIPOutputStream( compressed ) ) {
			stream.write( "{}".getBytes( StandardCharsets.UTF_8 ) );
		}
		byte[] bytes = compressed.toByteArray();
		for ( int length = 0; length < bytes.length; ++length ) {
			try ( var body = new JsonResponseBody( StandardCharsets.UTF_8, 0 ); var decoder = new JsonGzipDecoder( body ) ) {
				decoder.feed( ByteBuffer.wrap( bytes, 0, length ) );
				assertThatThrownBy( decoder::endOfInput ).isInstanceOf( JsonParseException.class );
			}
		}
		bytes[bytes.length - 8] ^= 1;
		try ( var body = new JsonResponseBody( StandardCharsets.UTF_8, 0 ); var decoder = new JsonGzipDecoder( body ) ) {
			assertThatThrownBy( () -> decoder.feed( ByteBuffer.wrap( bytes ) ) ).hasMessageContaining( "checksum" );
		}
	}

	@Test
	void deferredFailureDrainingAndBoundedDiagnostics() throws Exception {
		String input = "<proxy>" + "x".repeat( 1_000_000 );
		try ( var body = new JsonResponseBody( StandardCharsets.UTF_8, 0 ) ) {
			byte[] bytes = input.getBytes( StandardCharsets.UTF_8 );
			for ( int offset = 0; offset < bytes.length; offset += 8192 ) {
				ByteBuffer fragment =
						ByteBuffer.wrap( bytes, offset, Math.min( 8192, bytes.length - offset ) ).asReadOnlyBuffer();
				body.feed( fragment );
				assertThat( fragment.hasRemaining() ).isFalse();
			}
			body.complete();
			assertThatThrownBy( body::tree ).isInstanceOf( JsonParseException.class );
			try ( var diagnostic = body.openDiagnosticStream() ) {
				String text = new String( diagnostic.readAllBytes(), StandardCharsets.UTF_8 );
				assertThat( text ).startsWith( "<proxy>" )
						.endsWith( "[response diagnostic truncated after 8192 source bytes]" );
				assertThat( text.length() ).isLessThan( 8300 );
			}
		}
	}

	@Test
	void limitsCountOriginalCharsetBytesEvenAfterParseFailure() {
		Charset charset = StandardCharsets.UTF_16LE;
		byte[] bytes = "{\"value\":\"é\"}".getBytes( charset );
		try ( var body = new JsonResponseBody( charset, bytes.length ) ) {
			body.feed( ByteBuffer.wrap( bytes ) );
			body.complete();
			assertThat( body.tree().asObject().get( "value" ).asString() ).isEqualTo( "é" );
		}
		try ( var body = new JsonResponseBody( charset, bytes.length - 1 ) ) {
			assertThatThrownBy( () -> body.feed( ByteBuffer.wrap( bytes ) ) ).hasMessageContaining( "byte limit" );
		}
		try ( var body = new JsonResponseBody( StandardCharsets.UTF_8, 3 ) ) {
			body.feed( ByteBuffer.wrap( new byte[] { 60 } ) );
			assertThatThrownBy( () -> body.feed( ByteBuffer.wrap( new byte[3] ) ) ).hasMessageContaining( "byte limit" );
		}
	}
}
