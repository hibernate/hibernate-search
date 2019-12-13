/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.testsupport.util;

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
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.assertj.core.api.ListAssert;

public class DocumentAssert {
	private static final String INTERNAL_FIELDS_PREFIX = "__HSEARCH_";

	/**
	 * Creates a consumer that checks that a list of documents contains a document with the given ID,
	 * and that this document passes the given assertion.
	 * <p>
	 * The consumer should generally be passed to {@link ListAssert#satisfies(java.util.function.Consumer)}.
	 *
	 * @param assertions An assertion that should pass on the document with the given id.
	 * @return A consumer to be passed to {@link ListAssert#satisfies(java.util.function.Consumer)}.
	 */
	public static Consumer<List<? extends Document>> containsDocument(Consumer<DocumentAssert> assertions) {
		return allDocuments -> Assertions.assertThat( allDocuments ).anySatisfy(
			document -> assertions.accept( new DocumentAssert( document ) )
		);
	}

	private final Document actual;

	private Set<String> allCheckedPaths = new HashSet<>();

	public DocumentAssert(Document actual) {
		this.actual = actual;
	}

	private ListAssert<IndexableField> asFields() {
		return Assertions.assertThat( actual.getFields() );
	}

	public DocumentAssert hasField(String absoluteFieldPath, String ... values) {
		return hasField( "string", absoluteFieldPath, values );
	}

	public DocumentAssert hasField(String absoluteFieldPath, Number ... values) {
		return hasField( "numeric", absoluteFieldPath, values );
	}

	public DocumentAssert hasInternalField(String absoluteFieldPath, String ... values) {
		return hasField( INTERNAL_FIELDS_PREFIX + absoluteFieldPath, values );
	}

	public DocumentAssert hasInternalField(String absoluteFieldPath, Number ... values) {
		return hasField( INTERNAL_FIELDS_PREFIX + absoluteFieldPath, values );
	}

	@SafeVarargs
	private final <T> DocumentAssert hasField(String type, String absoluteFieldPath, T ... values) {
		String fieldDescription = "field at path '" + absoluteFieldPath + "'"
				+ " with expected type '" + type + "' and expected values '" + Arrays.toString( values ) + "'";
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
						return bytesRef.bytes;
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
