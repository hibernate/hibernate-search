/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.metadata.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.backend.spi.SingularTermDeletionQuery;
import org.hibernate.search.backend.spi.SingularTermDeletionQuery.Type;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;
import org.hibernate.search.engine.metadata.impl.PropertyMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.metadata.NumericFieldSettingsDescriptor.NumericEncodingType;

/**
 * @author Martin Braun
 */
public final class MetadataRehasher {

	public List<RehashedTypeMetadata> rehash(List<TypeMetadata> originals) {
		return originals.stream().map( this::rehash ).collect( Collectors.toList() );
	}

	public RehashedTypeMetadata rehash(TypeMetadata original) {
		RehashedTypeMetadata rehashed = new RehashedTypeMetadata();
		rehashed.originalTypeMetadata = original;

		if ( !this.handlePropertyMetadata( original, rehashed, original.getIdPropertyMetadata() ) ) {
			throw new IllegalArgumentException(
					"couldn't find any id field for: " + original.getType()
							+ "! This is required in order to use Hibernate Search with JPA!"
			);
		}

		for ( EmbeddedTypeMetadata embedded : original.getEmbeddedTypeMetadata() ) {
			this.rehashRec( embedded, rehashed );
		}
		return rehashed;
	}

	private void rehashRec(EmbeddedTypeMetadata original, RehashedTypeMetadata rehashed) {
		// handle the current TypeMetadata
		this.handleTypeMetadata( original, rehashed );
		// recursion
		for ( EmbeddedTypeMetadata embedded : original.getEmbeddedTypeMetadata() ) {
			this.rehashRec( embedded, rehashed );
		}
	}

	private void handleTypeMetadata(EmbeddedTypeMetadata original, RehashedTypeMetadata rehashed) {
		for ( PropertyMetadata propertyMetadata : original.getAllPropertyMetadata() ) {
			if ( this.handlePropertyMetadata( original, rehashed, propertyMetadata ) ) {
				return;
			}
		}
		throw new IllegalArgumentException(
				"couldn't find any id field for: " + original.getType()
						+ "! This is required in order to use Hibernate Search with JPA!"
		);
	}

	private boolean handlePropertyMetadata(
			TypeMetadata original,
			RehashedTypeMetadata rehashed,
			PropertyMetadata propertyMetadata) {
		for ( DocumentFieldMetadata documentFieldMetadata : propertyMetadata.getFieldMetadata() ) {
			// this must either be id or id of an embedded object
			if ( documentFieldMetadata.isIdInEmbedded() || documentFieldMetadata.isId() ) {
				Class<?> type = original.getType();
				rehashed.idFieldNamesForType.computeIfAbsent( type, (key) -> new ArrayList<>() ).add(
						documentFieldMetadata.getName()
				);
				rehashed.idPropertyNameForType.put( type, propertyMetadata.getPropertyAccessorName() );
				if ( rehashed.documentFieldMetadataForIdFieldName.containsKey( documentFieldMetadata.getName() ) ) {
					throw new AssertionFailure( "field handled twice!" );
				}
				rehashed.idPropertyAccessorForType.put( type, propertyMetadata.getPropertyAccessor() );
				rehashed.documentFieldMetadataForIdFieldName.put(
						documentFieldMetadata.getName(),
						documentFieldMetadata
				);
				SingularTermDeletionQuery.Type deletionQueryType;
				if ( documentFieldMetadata.isNumeric() ) {
					//as of Hibernate Search 5.3.0.Beta1 ids are always Strings, but just to make sure
					NumericEncodingType numEncType = documentFieldMetadata.getNumericEncodingType();
					switch ( numEncType ) {
						case LONG:
							deletionQueryType = Type.LONG;
							break;
						case INTEGER:
							deletionQueryType = Type.INT;
							break;
						case DOUBLE:
							deletionQueryType = Type.DOUBLE;
							break;
						case FLOAT:
							deletionQueryType = Type.FLOAT;
							break;
						default:
							throw new IllegalArgumentException(
									"unexpected Numeric encoding type for id: " + numEncType
											+ ". only the standard LONG, INTEGER, DOUBLE, FLOAT are allowed!"
							);
					}
				}
				else {
					deletionQueryType = SingularTermDeletionQuery.Type.STRING;
				}
				rehashed.singularTermDeletionQueryTypeForIdFieldName.put(
						documentFieldMetadata.getName(),
						deletionQueryType
				);
				return true;
			}
		}
		return false;
	}
}
