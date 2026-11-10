package org.hibernate.search.backend.elasticsearch.client.common.json.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.hibernate.search.backend.elasticsearch.client.common.json.entity.spi.JsonGzipDecoder;
import org.hibernate.search.util.json.spi.JsonParseException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JsonGzipDecoderTest {
	private static final int FTEXT = 0x01;
	private static final int FHCRC = 0x02;
	private static final int FEXTRA = 0x04;
	private static final int FNAME = 0x08;
	private static final int FCOMMENT = 0x10;
	private static final int ALL_OPTIONAL_FIELDS = FHCRC | FEXTRA | FNAME | FCOMMENT;
	private static final int FIXED_HEADER_LENGTH = 10;
	private static final int FLAGS_OFFSET = 3;

	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 5, 8192 })
	void optionalHeadersAndHeaderChecksumAcrossFragments(int fragmentSize) throws Exception {
		// Include every flag combination, including the advisory FTEXT bit, and a two-byte XLEN.
		for ( int flags = 0; flags <= ( ALL_OPTIONAL_FIELDS | FTEXT ); ++flags ) {
			for ( int extraLength : new int[] { 0, 260 } ) {
				byte[] member = member( "{}", flags, extraLength ).bytes();
				try ( var standardDecoder = new GZIPInputStream( new ByteArrayInputStream( member ) ) ) {
					assertThat( new String( standardDecoder.readAllBytes(), StandardCharsets.UTF_8 ) ).isEqualTo( "{}" );
				}
				try ( var body = new JsonResponseBody( StandardCharsets.UTF_8, 0 );
						var decoder = new JsonGzipDecoder( body ) ) {
					feed( decoder, member, fragmentSize );
					decoder.endOfInput();
					body.complete();
					assertThat( body.tree().asObject().size() ).isZero();
				}
			}
		}
	}

	@Test
	void corruptedHeaderChecksumAndSizeAreRejected() throws Exception {
		Member member = member( "{}", ALL_OPTIONAL_FIELDS, 260 );
		byte[] corruptHeader = member.bytes().clone();
		corruptHeader[member.headerLength() - Short.BYTES] ^= 1;
		try ( var body = new JsonResponseBody( StandardCharsets.UTF_8, 0 ); var decoder = new JsonGzipDecoder( body ) ) {
			assertThatThrownBy( () -> feed( decoder, corruptHeader, 1 ) )
					.isInstanceOf( JsonParseException.class ).hasMessageContaining( "header checksum" );
		}
		byte[] corruptSize = member.bytes().clone();
		corruptSize[corruptSize.length - Integer.BYTES] ^= 1;
		try ( var body = new JsonResponseBody( StandardCharsets.UTF_8, 0 ); var decoder = new JsonGzipDecoder( body ) ) {
			assertThatThrownBy( () -> feed( decoder, corruptSize, 1 ) )
					.isInstanceOf( JsonParseException.class ).hasMessageContaining( "size" );
		}
	}

	@Test
	void truncatedOptionalHeaderIsRejected() throws Exception {
		Member member = member( "{}", ALL_OPTIONAL_FIELDS, 260 );
		for ( int length = 0; length < member.headerLength(); ++length ) {
			try ( var body = new JsonResponseBody( StandardCharsets.UTF_8, 0 ); var decoder = new JsonGzipDecoder( body ) ) {
				feed( decoder, Arrays.copyOf( member.bytes(), length ), 1 );
				assertThatThrownBy( decoder::endOfInput )
						.isInstanceOf( JsonParseException.class ).hasMessageContaining( "Truncated gzip" );
			}
		}
	}

	@Test
	void emptyMemberIsValidAndCanPrecedeAnotherMember() throws Exception {
		byte[] emptyMember = member( "", ALL_OPTIONAL_FIELDS, 0 ).bytes();
		try ( var body = new JsonResponseBody( StandardCharsets.UTF_8, 0 ); var decoder = new JsonGzipDecoder( body ) ) {
			feed( decoder, emptyMember, 1 );
			decoder.endOfInput();
			body.complete();
			assertThat( body.tree() ).isNull();
		}
		try ( var body = new JsonResponseBody( StandardCharsets.UTF_8, 0 ); var decoder = new JsonGzipDecoder( body ) ) {
			feed( decoder, emptyMember, 1 );
			feed( decoder, member( "{}", FHCRC | FNAME, 0 ).bytes(), 1 );
			decoder.endOfInput();
			body.complete();
			assertThat( body.tree().asObject().size() ).isZero();
		}
		try ( var body = new JsonResponseBody( StandardCharsets.UTF_8, 0 ); var decoder = new JsonGzipDecoder( body ) ) {
			assertThatThrownBy( decoder::endOfInput )
					.isInstanceOf( JsonParseException.class ).hasMessageContaining( "Truncated gzip" );
		}
	}

	private static void feed(JsonGzipDecoder decoder, byte[] bytes, int fragmentSize) {
		for ( int offset = 0; offset < bytes.length; offset += fragmentSize ) {
			ByteBuffer fragment = ByteBuffer.wrap( bytes, offset, Math.min( fragmentSize, bytes.length - offset ) )
					.asReadOnlyBuffer();
			decoder.feed( fragment );
			assertThat( fragment.hasRemaining() ).isFalse();
		}
	}

	private static Member member(String text, int flags, int extraLength) throws Exception {
		var compressed = new ByteArrayOutputStream();
		try ( var gzip = new GZIPOutputStream( compressed ) ) {
			gzip.write( text.getBytes( StandardCharsets.UTF_8 ) );
		}
		byte[] ordinaryMember = compressed.toByteArray();
		var header = new ByteArrayOutputStream();
		byte[] fixedHeader = Arrays.copyOf( ordinaryMember, FIXED_HEADER_LENGTH );
		fixedHeader[FLAGS_OFFSET] = (byte) flags;
		header.writeBytes( fixedHeader );
		if ( ( flags & FEXTRA ) != 0 ) {
			header.write( extraLength );
			header.write( extraLength >>> Byte.SIZE );
			if ( extraLength > 0 ) {
				// One extra subfield: two-byte ID, two-byte payload length, then the payload.
				header.write( 'H' );
				header.write( 'S' );
				int payloadLength = extraLength - 2 * Short.BYTES;
				header.write( payloadLength );
				header.write( payloadLength >>> Byte.SIZE );
				header.writeBytes( new byte[payloadLength] );
			}
		}
		if ( ( flags & FNAME ) != 0 ) {
			header.writeBytes( "response.json\0".getBytes( StandardCharsets.ISO_8859_1 ) );
		}
		if ( ( flags & FCOMMENT ) != 0 ) {
			header.writeBytes( "gzip response\0".getBytes( StandardCharsets.ISO_8859_1 ) );
		}
		if ( ( flags & FHCRC ) != 0 ) {
			var crc = new CRC32();
			crc.update( header.toByteArray() );
			header.write( (int) crc.getValue() );
			header.write( (int) ( crc.getValue() >>> Byte.SIZE ) );
		}
		int headerLength = header.size();
		header.write( ordinaryMember, FIXED_HEADER_LENGTH, ordinaryMember.length - FIXED_HEADER_LENGTH );
		return new Member( header.toByteArray(), headerLength );
	}

	private record Member(byte[] bytes, int headerLength) {
	}
}
