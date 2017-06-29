/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.nulls.impl;

import org.hibernate.search.bridge.spi.NullMarker;
import org.hibernate.search.engine.metadata.impl.PartialDocumentFieldMetadata;
import org.hibernate.search.engine.nulls.codec.impl.LuceneDoubleNullMarkerCodec;
import org.hibernate.search.engine.nulls.codec.impl.LuceneFloatNullMarkerCodec;
import org.hibernate.search.engine.nulls.codec.impl.LuceneIntegerNullMarkerCodec;
import org.hibernate.search.engine.nulls.codec.impl.LuceneLongNullMarkerCodec;
import org.hibernate.search.engine.nulls.codec.impl.LuceneStringNullMarkerCodec;
import org.hibernate.search.engine.nulls.codec.impl.NullMarkerCodec;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Yoann Rodiere
 */
public class LuceneMissingValueStrategy implements MissingValueStrategy {

	private static final Log LOG = LoggerFactory.make( Log.class );

	public static final LuceneMissingValueStrategy INSTANCE = new LuceneMissingValueStrategy();

	private LuceneMissingValueStrategy() {
		// use INSTANCE
	}

	@Override
	public NullMarkerCodec createNullMarkerCodec(IndexedTypeIdentifier entityType,
			PartialDocumentFieldMetadata fieldMetadata, NullMarker nullMarker) {
		Object nullEncoded = nullMarker.nullEncoded();
		if ( nullEncoded instanceof String ) {
			return new LuceneStringNullMarkerCodec( nullMarker );
		}
		else if ( nullEncoded instanceof Integer ) {
			return new LuceneIntegerNullMarkerCodec( nullMarker );
		}
		else if ( nullEncoded instanceof Long ) {
			return new LuceneLongNullMarkerCodec( nullMarker );
		}
		else if ( nullEncoded instanceof Float ) {
			return new LuceneFloatNullMarkerCodec( nullMarker );
		}
		else if ( nullEncoded instanceof Double ) {
			return new LuceneDoubleNullMarkerCodec( nullMarker );
		}
		else {
			throw LOG.unsupportedNullTokenType( entityType, fieldMetadata.getPath().getAbsoluteName(),
					nullEncoded.getClass() );
		}
	}

}
