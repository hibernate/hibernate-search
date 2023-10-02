/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.testsupport.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;
import org.assertj.core.api.Condition;
import org.assertj.core.api.ListAssert;

public class DocumentAssert {
	private static final String INTERNAL_FIELDS_PREFIX = "__HSEARCH_";

	/**
	 * Creates a consumer that checks that a list of documents contains a document with the given ID,
	 * and that this document passes the given assertion.
	 * <p>
	 * The consumer should generally be passed to {@link ListAssert#satisfies(java.util.function.Consumer[])}
	 *
	 * @param assertions An assertion that should pass on the document with the given id.
	 * @return A consumer to be passed to {@link ListAssert#satisfies(java.util.function.Consumer[])}.
	 */
	public static Consumer<List<? extends Document>> containsDocument(Consumer<DocumentAssert> assertions) {
		return allDocuments -> assertThat( allDocuments ).anySatisfy(
				document -> assertions.accept( new DocumentAssert( document ) )
		);
	}

	public static DocumentAssert assertThatDocument(Document document) {
		return new DocumentAssert( document );
	}

	private final Document actual;

	private Set<String> allCheckedPaths = new HashSet<>();

	public DocumentAssert(Document actual) {
		this.actual = actual;
	}

	private ListAssert<IndexableField> asFields() {
		return assertThat( actual.getFields() );
	}

	public DocumentAssert hasField(String absoluteFieldPath, String... values) {
		return hasField( "string", absoluteFieldPath, values );
	}

	public DocumentAssert hasField(String absoluteFieldPath, Number... values) {
		return hasField( "numeric", absoluteFieldPath, values );
	}

	public DocumentAssert hasField(String absoluteFieldPath, byte[]... values) {
		return hasField( "byte[]", absoluteFieldPath, values );
	}

	public DocumentAssert hasVectorField(String absoluteFieldPath, byte[]... values) {
		return hasField( "byte[]", absoluteFieldPath, Arrays.stream( values ).toArray( byte[][]::new ) );
	}

	public DocumentAssert hasVectorField(String absoluteFieldPath, float[]... values) {
		return hasField( "float[]", absoluteFieldPath,
				Arrays.stream( values ).map( floats -> {
					ByteBuffer buffer = ByteBuffer.allocate( floats.length * Float.BYTES );
					for ( float v : floats ) {
						buffer.putFloat( v );
					}
					return buffer.array();
				} ).toArray( byte[][]::new ) );
	}

	@SafeVarargs
	private final <T> DocumentAssert hasField(String type, String absoluteFieldPath, T... values) {
		String fieldDescription = "field at path '" + absoluteFieldPath + "'"
				+ " with expected type '" + type + "' and expected values '" + Arrays.deepToString( values ) + "'";
		Predicate<IndexableField> predicate = field -> absoluteFieldPath.equals( field.name() );
		asFields()
				.areAtLeastOne( new Condition<>( predicate, fieldDescription ) )
				.filteredOn( predicate )
				.extracting( (Function<IndexableField, Object>) f -> {
					// We can't just return everything and then exclude nulls,
					// since .stringValue() converts a number to a string automatically...
					Number number = f.numericValue();
					if ( number != null ) {
						return number;
					}
					BytesRef bytesRef = f.binaryValue();
					if ( bytesRef != null ) {
						return BytesRef.deepCopyOf( bytesRef ).bytes;
					}
					String string = f.stringValue();
					if ( string != null ) {
						return string;
					}
					return null;
				} )
				.filteredOn( Objects::nonNull )
				.as( fieldDescription )
				.containsExactlyInAnyOrder( (Object[]) values );
		allCheckedPaths.add( absoluteFieldPath );
		return this;
	}

	public void andOnlyInternalFields() {
		Set<String> allowedPaths = new HashSet<>( allCheckedPaths );
		// There is no stored internal field at the moment, we use docvalues instead.
		asFields().are( new Condition<>(
				field -> allowedPaths.contains( field.name() ),
				"exclusively fields with path " + allCheckedPaths
						+ " or prefixed with " + INTERNAL_FIELDS_PREFIX + " , and no other path"
		) );
	}
}
