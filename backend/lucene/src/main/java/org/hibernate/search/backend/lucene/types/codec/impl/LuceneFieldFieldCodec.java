/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.backend.lucene.types.converter.LuceneFieldContributor;
import org.hibernate.search.backend.lucene.types.converter.LuceneFieldValueExtractor;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class LuceneFieldFieldCodec<F> implements LuceneFieldCodec<F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneFieldContributor<F> fieldContributor;

	private final LuceneFieldValueExtractor<F> fieldValueExtractor;

	public LuceneFieldFieldCodec(LuceneFieldContributor<F> fieldContributor, LuceneFieldValueExtractor<F> fieldValueExtractor) {
		this.fieldContributor = fieldContributor;
		this.fieldValueExtractor = fieldValueExtractor;
	}

	@Override
	public void encode(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, F value) {
		if ( value == null ) {
			return;
		}

		fieldContributor.contribute( absoluteFieldPath, value, f -> contributeField( documentBuilder, absoluteFieldPath, f ) );
	}

	@Override
	public F decode(IndexableField field) {
		if ( fieldValueExtractor == null ) {
			throw log.unsupportedProjectionForNativeField(
					EventContexts.fromIndexFieldAbsolutePath( field.name() )
			);
		}
		return fieldValueExtractor.extract( field );
	}

	@Override
	public Query createExistsQuery(String absoluteFieldPath) {
		throw new AssertionFailure(
				"This method should not be called, as native Lucene fields are not supported by the query DSL."
		);
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldCodec<?> obj) {
		if ( this == obj ) {
			return true;
		}
		if ( LuceneFieldFieldCodec.class != obj.getClass() ) {
			return false;
		}

		LuceneFieldFieldCodec<?> other = (LuceneFieldFieldCodec<?>) obj;

		return Objects.equals( fieldContributor, other.fieldContributor )
				&& Objects.equals( fieldValueExtractor, other.fieldValueExtractor );
	}

	private static void contributeField(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, IndexableField field) {
		if ( !absoluteFieldPath.equals( field.name() ) ) {
			throw log.invalidFieldPath( absoluteFieldPath, field.name() );
		}
		documentBuilder.addField( field );
	}
}
