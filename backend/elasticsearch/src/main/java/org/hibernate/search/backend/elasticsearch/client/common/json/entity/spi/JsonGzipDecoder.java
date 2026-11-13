package org.hibernate.search.backend.elasticsearch.client.common.json.entity.spi;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;

import org.hibernate.search.backend.elasticsearch.logging.spi.ElasticsearchClientLog;

/**
 * Incremental gzip framing for clients whose response consumer precedes decompression.
 * GZIPInputStream requires blocking reads; HTTP callbacks instead feed the available bytes here.
 * The framing follows RFC 1952, section 2.3.1.
 */
public final class JsonGzipDecoder implements AutoCloseable {
	private static final int OUTPUT_BUFFER_SIZE = 8192;
	private static final int FIXED_HEADER_LENGTH = 10;
	private static final int COMPRESSION_METHOD_OFFSET = 2;
	private static final int FLAGS_OFFSET = 3;
	private static final int FHCRC = 0x02;
	private static final int FEXTRA = 0x04;
	private static final int FNAME = 0x08;
	private static final int FCOMMENT = 0x10;
	private static final int RESERVED_FLAGS = 0xE0;
	private static final int HEADER_CRC_MASK = 0xFFFF;
	private static final long UNSIGNED_INT_MASK = 0xFFFFFFFFL;
	private static final int TRAILER_LENGTH = 2 * Integer.BYTES;

	private enum State {
		HEADER, EXTRA_LENGTH, EXTRA, NAME, COMMENT, HEADER_CRC, DEFLATE, TRAILER
	}

	private final org.hibernate.search.util.json.spi.JsonResponseConsumer<?> body;
	// Gzip supplies its own header and trailer around a raw DEFLATE stream.
	private final Inflater inflater = new Inflater( true );
	private final CRC32 crc = new CRC32();
	private final CRC32 headerCrc = new CRC32();
	private byte[] output = new byte[OUTPUT_BUFFER_SIZE];
	private State state = State.HEADER;
	private int offset;
	private int flags;
	private int extraLength;
	private long fieldValue;
	private boolean memberCompleted;

	public JsonGzipDecoder(org.hibernate.search.util.json.spi.JsonResponseConsumer<?> body) {
		this.body = body;
	}

	public void feed(ByteBuffer input) {
		try {
			while ( input.hasRemaining() ) {
				if ( state == State.DEFLATE ) {
					inflater.setInput( input );
					while ( !inflater.needsInput() ) {
						int count = inflater.inflate( output );
						if ( count > 0 ) {
							crc.update( output, 0, count );
							body.feed( ByteBuffer.wrap( output, 0, count ) );
						}
						if ( inflater.finished() ) {
							state = State.TRAILER;
							offset = 0;
							fieldValue = 0;
							break;
						}
						if ( count == 0 && !inflater.needsInput() ) {
							throw ElasticsearchClientLog.INSTANCE.invalidGzipDeflateStream();
						}
					}
					continue;
				}
				int value = Byte.toUnsignedInt( input.get() );
				switch ( state ) {
					case HEADER:
						headerCrc.update( value );
						if ( ( offset == 0 && value != ( GZIPInputStream.GZIP_MAGIC & 0xFF ) )
								|| ( offset == 1 && value != ( GZIPInputStream.GZIP_MAGIC >>> Byte.SIZE ) )
								|| ( offset == COMPRESSION_METHOD_OFFSET && value != Deflater.DEFLATED )
								|| ( offset == FLAGS_OFFSET && ( value & RESERVED_FLAGS ) != 0 ) ) {
							throw ElasticsearchClientLog.INSTANCE.invalidGzipHeader();
						}
						if ( offset == FLAGS_OFFSET ) {
							flags = value;
						}
						if ( ++offset == FIXED_HEADER_LENGTH ) {
							nextHeaderState();
						}
						break;
					case EXTRA_LENGTH:
						headerCrc.update( value );
						extraLength |= value << ( Byte.SIZE * offset++ );
						if ( offset == Short.BYTES ) {
							state = State.EXTRA;
							if ( extraLength == 0 ) {
								nextHeaderState();
							}
						}
						break;
					case EXTRA:
						headerCrc.update( value );
						if ( --extraLength == 0 ) {
							nextHeaderState();
						}
						break;
					case NAME:
					case COMMENT:
						headerCrc.update( value );
						if ( value == 0 ) {
							nextHeaderState();
						}
						break;
					case HEADER_CRC:
						// FHCRC stores the low 16 bits of the CRC32 of all preceding header bytes.
						fieldValue |= (long) value << ( Byte.SIZE * offset++ );
						if ( offset == Short.BYTES ) {
							if ( fieldValue != ( headerCrc.getValue() & HEADER_CRC_MASK ) ) {
								throw ElasticsearchClientLog.INSTANCE.invalidGzipHeaderChecksum();
							}
							state = State.DEFLATE;
						}
						break;
					case TRAILER:
						// Two little-endian words: decoded CRC32, then decoded size modulo 2^32 (ISIZE).
						fieldValue |= (long) value << ( Byte.SIZE * ( offset++ % Integer.BYTES ) );
						if ( offset == Integer.BYTES ) {
							if ( fieldValue != crc.getValue() ) {
								throw ElasticsearchClientLog.INSTANCE.invalidGzipChecksum();
							}
							fieldValue = 0;
						}
						if ( offset == TRAILER_LENGTH ) {
							if ( fieldValue != ( inflater.getBytesWritten() & UNSIGNED_INT_MASK ) ) {
								throw ElasticsearchClientLog.INSTANCE.invalidGzipSize();
							}
							memberCompleted = true;
							// Concatenated members continue with a fresh header and independent checksums.
							state = State.HEADER;
							offset = 0;
							inflater.reset();
							crc.reset();
							headerCrc.reset();
						}
						break;
					default:
						throw ElasticsearchClientLog.INSTANCE.unexpectedGzipState();
				}
			}
		}
		catch (DataFormatException e) {
			throw ElasticsearchClientLog.INSTANCE.invalidGzipContent( e );
		}
	}

	private void nextHeaderState() {
		offset = 0;
		fieldValue = 0;
		// Optional fields follow the fixed header in this order, regardless of the flag bit order.
		if ( ( flags & FEXTRA ) != 0 ) {
			flags &= ~FEXTRA;
			extraLength = 0;
			state = State.EXTRA_LENGTH;
		}
		else if ( ( flags & FNAME ) != 0 ) {
			flags &= ~FNAME;
			state = State.NAME;
		}
		else if ( ( flags & FCOMMENT ) != 0 ) {
			flags &= ~FCOMMENT;
			state = State.COMMENT;
		}
		else {
			state = ( flags & FHCRC ) != 0 ? State.HEADER_CRC : State.DEFLATE;
		}
	}

	public void endOfInput() {
		if ( !memberCompleted || state != State.HEADER || offset != 0 ) {
			throw ElasticsearchClientLog.INSTANCE.truncatedGzipResponse();
		}
	}

	@Override
	public void close() {
		inflater.end();
		output = null;
	}
}
