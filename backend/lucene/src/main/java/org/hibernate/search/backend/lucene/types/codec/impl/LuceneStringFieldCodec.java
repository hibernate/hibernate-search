/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.AnalyzerConstants;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;

public final class LuceneStringFieldCodec implements LuceneStandardFieldCodec<String, String> {

	private final boolean sortable;
	private final boolean searchable;
	private final boolean aggregable;

	private final FieldType fieldType;

	private final String indexNullAsValue;

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
	public void addToDocument(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, String value) {
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
			documentBuilder.addField( new SortedSetDocValuesField( absoluteFieldPath, normalized ) );
		}

		if ( !sortable && fieldType.omitNorms() ) {
			// For the "exists" predicate
			documentBuilder.addFieldName( absoluteFieldPath );
		}
	}

	@Override
	public String decode(IndexableField field) {
		return field.stringValue();
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldCodec<?> obj) {
		if ( this == obj ) {
			return true;
		}
		return LuceneStringFieldCodec.class == obj.getClass();
	}

	@Override
	public String encode(String value) {
		return value;
	}

	private BytesRef normalize(String absoluteFieldPath, String value) {
		if ( value == null ) {
			return null;
		}
		if ( analyzerOrNormalizer == AnalyzerConstants.KEYWORD_ANALYZER ) {
			// Optimization when analysis is disabled
			return new BytesRef( value );
		}
		return analyzerOrNormalizer.normalize( absoluteFieldPath, value );
	}
}
