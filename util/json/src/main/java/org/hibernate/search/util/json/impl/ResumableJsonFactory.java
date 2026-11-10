package org.hibernate.search.util.json.impl;

import java.nio.ByteBuffer;

import org.hibernate.search.util.json.logging.impl.JsonLog;

import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.async.NonBlockingByteBufferJsonParser;
import tools.jackson.core.sym.ByteQuadsCanonicalizer;

/**
 * Workaround for Core 3.2.2 pausing within an escaped property-name surrogate pair before
 * consuming all available input, or without updating its current token. Keep this implementation
 * detail isolated; the escaped-name split-point regression determines when it can be removed.
 */
final class ResumableJsonFactory extends JsonFactory {
	ResumableJsonFactory(JsonFactory configured) {
		super( configured );
	}

	NonBlockingByteBufferJsonParser createResumableByteBufferParser(ObjectReadContext readContext) {
		IOContext context = _createNonBlockingContext( null );
		ByteQuadsCanonicalizer symbols = _byteSymbolCanonicalizer.makeChildOrPlaceholder( _factoryFeatures );
		return new ResumableParser( readContext, context, readContext.getStreamReadFeatures( _streamReadFeatures ),
				readContext.getFormatReadFeatures( _formatReadFeatures ), symbols );
	}

	private static final class ResumableParser extends NonBlockingByteBufferJsonParser {
		private byte[] inputArray;
		private int inputArrayOffset;

		private ResumableParser(ObjectReadContext readContext, IOContext context, int features, int formatFeatures,
				ByteQuadsCanonicalizer symbols) {
			super( readContext, context, features, formatFeatures, symbols );
		}

		@Override
		public void feedInput(ByteBuffer input) {
			super.feedInput( input );
			// Heap slices need only their array offset; direct/read-only storage stays on Core's path.
			// Feeding the empty ownership-handoff buffer also clears this caller-array reference.
			inputArray = input.hasArray() ? input.array() : null;
			inputArrayOffset = input.hasArray() ? input.arrayOffset() : 0;
		}

		@Override
		protected byte getByteFromBuffer(int offset) {
			return inputArray == null ? super.getByteFromBuffer( offset ) : inputArray[inputArrayOffset + offset];
		}

		@Override
		protected byte getNextSignedByteFromBuffer() {
			return inputArray == null ? super.getNextSignedByteFromBuffer() : inputArray[inputArrayOffset + _inputPtr++];
		}

		@Override
		protected int getNextUnsignedByteFromBuffer() {
			return inputArray == null
					? super.getNextUnsignedByteFromBuffer()
					: inputArray[inputArrayOffset + _inputPtr++] & 0xff;
		}

		@Override
		public JsonToken nextToken() {
			int lastRetryPointer = -1;
			while ( true ) {
				JsonToken token = super.nextToken();
				if ( token == JsonToken.NOT_AVAILABLE && _pendingSurrogateInName != 0 ) {
					if ( currentToken() != JsonToken.NOT_AVAILABLE ) {
						_updateTokenToNA();
					}
					// Core 3 can pause before consuming available low-surrogate escape bytes.
					if ( !needMoreInput() ) {
						if ( _inputPtr == lastRetryPointer ) {
							throw JsonLog.INSTANCE.parserCannotResumeSurrogatePair();
						}
						lastRetryPointer = _inputPtr;
						continue;
					}
				}
				return token;
			}
		}
	}
}
