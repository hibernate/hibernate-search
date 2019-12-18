/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.util.Objects;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.LuceneFields;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocValuesFieldExistsQuery;
import org.apache.lucene.search.NormsFieldExistsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;

public final class LuceneStringFieldCodec implements LuceneTextFieldCodec<String> {

	private final boolean sortable;
	private final boolean searchable;
	private final boolean aggregable;

	private final FieldType fieldType;

	private String indexNullAsValue;

	private final Analyzer analyzerOrNormalizer;

	public LuceneStringFieldCodec(boolean searchable, boolean sortable, boolean aggregable,
			FieldType fieldType, String indexNullAsValue, Analyzer analyzerOrNormalizer) {
		this.sortable = sortable;
		this.searchable = searchable;
		this.aggregable = aggregable;
		this.fieldType = fieldType;
		this.indexNullAsValue = indexNullAsValue;
		this.analyzerOrNormalizer = analyzerOrNormalizer;
	}

	@Override
	public void encode(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, String value) {
		if ( value == null && indexNullAsValue != null ) {
			value = indexNullAsValue;
		}

		if ( value == null ) {
			return;
		}

		if ( searchable || fieldType.stored() ) {
			// searchable or projectable or both
			documentBuilder.addField( new Field( absoluteFieldPath, value, fieldType ) );
		}

		if ( sortable || aggregable ) {
			BytesRef normalized = normalize( absoluteFieldPath, value );
			if ( sortable ) {
				documentBuilder.addField( new SortedDocValuesField( absoluteFieldPath, normalized ) );
			}
			if ( aggregable ) {
				documentBuilder.addField( new SortedSetDocValuesFacetField( absoluteFieldPath, normalized.utf8ToString() ) );
			}
		}

		if ( !sortable && fieldType.omitNorms() ) {
			// For createExistsQuery()
			documentBuilder.addFieldName( absoluteFieldPath );
		}
	}

	@Override
	public String decode(Document document, String absoluteFieldPath) {
		return document.get( absoluteFieldPath );
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldCodec<?> obj) {
		if ( this == obj ) {
			return true;
		}
		if ( LuceneStringFieldCodec.class != obj.getClass() ) {
			return false;
		}

		LuceneStringFieldCodec other = (LuceneStringFieldCodec) obj;

		return ( sortable == other.sortable ) && ( searchable == other.searchable )
				&& Objects.equals( fieldType, other.fieldType );
	}

	@Override
	public Query createExistsQuery(String absoluteFieldPath) {
		if ( !fieldType.omitNorms() ) {
			return new NormsFieldExistsQuery( absoluteFieldPath );
		}
		else if ( sortable ) {
			return new DocValuesFieldExistsQuery( absoluteFieldPath );
		}
		else {
			return new TermQuery( new Term( LuceneFields.fieldNamesFieldName(), absoluteFieldPath ) );
		}
	}

	@Override
	public String encode(String value) {
		return value;
	}

	@Override
	public BytesRef normalize(String absoluteFieldPath, String value) {
		if ( value == null ) {
			return null;
		}
		if ( analyzerOrNormalizer == null ) {
			return new BytesRef( value );
		}
		return analyzerOrNormalizer.normalize( absoluteFieldPath, value );
	}
}
