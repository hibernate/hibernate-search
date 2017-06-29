/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.nulls.impl;

import org.hibernate.search.bridge.spi.NullMarker;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.nulls.codec.impl.ElasticsearchAsNullStringNullMarkerCodec;
import org.hibernate.search.elasticsearch.nulls.codec.impl.ElasticsearchAsTokenStringNullMarkerCodec;
import org.hibernate.search.elasticsearch.nulls.codec.impl.ElasticsearchBooleanNullMarkerCodec;
import org.hibernate.search.elasticsearch.nulls.codec.impl.ElasticsearchDoubleNullMarkerCodec;
import org.hibernate.search.elasticsearch.nulls.codec.impl.ElasticsearchFloatNullMarkerCodec;
import org.hibernate.search.elasticsearch.nulls.codec.impl.ElasticsearchIntegerNullMarkerCodec;
import org.hibernate.search.elasticsearch.nulls.codec.impl.ElasticsearchLongNullMarkerCodec;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaTranslator;
import org.hibernate.search.engine.metadata.impl.PartialDocumentFieldMetadata;
import org.hibernate.search.engine.nulls.codec.impl.NullMarkerCodec;
import org.hibernate.search.engine.nulls.impl.MissingValueStrategy;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.util.logging.impl.LoggerFactory;

public final class ElasticsearchMissingValueStrategy implements MissingValueStrategy {
	private static final Log LOG = LoggerFactory.make( Log.class );

	private final ElasticsearchSchemaTranslator schemaTranslator;

	public ElasticsearchMissingValueStrategy(ElasticsearchSchemaTranslator schemaTranslator) {
		super();
		this.schemaTranslator = schemaTranslator;
	}

	@Override
	public NullMarkerCodec createNullMarkerCodec(IndexedTypeIdentifier entityType,
			PartialDocumentFieldMetadata fieldMetadata, NullMarker nullMarker) {
		Object nullEncoded = nullMarker.nullEncoded();
		if ( nullEncoded instanceof String ) {
			if ( schemaTranslator.isTextDataType( fieldMetadata ) ) {
				/*
				 * The "text" datatype does not support null_value,
				 * which implies a slightly different null value handling
				 * on our side.
				 */
				return new ElasticsearchAsTokenStringNullMarkerCodec( nullMarker );
			}
			else {
				return new ElasticsearchAsNullStringNullMarkerCodec( nullMarker );
			}
		}
		else if ( nullEncoded instanceof Integer ) {
			return new ElasticsearchIntegerNullMarkerCodec( nullMarker );
		}
		else if ( nullEncoded instanceof Long ) {
			return new ElasticsearchLongNullMarkerCodec( nullMarker );
		}
		else if ( nullEncoded instanceof Float ) {
			return new ElasticsearchFloatNullMarkerCodec( nullMarker );
		}
		else if ( nullEncoded instanceof Double ) {
			return new ElasticsearchDoubleNullMarkerCodec( nullMarker );
		}
		else if ( nullEncoded instanceof Boolean ) {
			return new ElasticsearchBooleanNullMarkerCodec( nullMarker );
		}
		else {
			throw LOG.unsupportedNullTokenType( entityType, fieldMetadata.getPath().getRelativeName(),
					nullEncoded.getClass() );
		}
	}

}

