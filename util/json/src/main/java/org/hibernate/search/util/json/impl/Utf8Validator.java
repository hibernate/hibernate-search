package org.hibernate.search.util.json.impl;

import java.nio.ByteBuffer;

import org.hibernate.search.util.json.logging.impl.JsonLog;

/** Validates canonical UTF-8 with constant state, including sequences split between feeds. */
final class Utf8Validator {
	private int remaining;
	private int lower = 0x80;
	private int upper = 0xbf;

	void accept(ByteBuffer input) {
		for ( int i = input.position(); i < input.limit(); i++ ) {
			int value = input.get( i ) & 0xff;
			if ( remaining != 0 ) {
				if ( value < lower || value > upper ) {
					throw JsonLog.INSTANCE.invalidUtf8Continuation();
				}
				remaining--;
				lower = 0x80;
				upper = 0xbf;
			}
			else if ( value <= 0x7f ) {
				/* ASCII, validated as JSON by the parser. */ }
			else if ( value >= 0xc2 && value <= 0xdf ) {
				remaining = 1;
			}
			else if ( value >= 0xe0 && value <= 0xef ) {
				remaining = 2;
				if ( value == 0xe0 ) {
					lower = 0xa0;
				}
				if ( value == 0xed ) {
					upper = 0x9f;
				}
			}
			else if ( value >= 0xf0 && value <= 0xf4 ) {
				remaining = 3;
				if ( value == 0xf0 ) {
					lower = 0x90;
				}
				if ( value == 0xf4 ) {
					upper = 0x8f;
				}
			}
			else {
				throw JsonLog.INSTANCE.invalidUtf8LeadingByte();
			}
		}
	}

	void endOfInput() {
		if ( remaining != 0 ) {
			throw JsonLog.INSTANCE.truncatedUtf8Input();
		}
	}
}
