/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.hibernate.search.backend.lucene.lowlevel.common.impl.AnalyzerConstants;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;

public final class LuceneStringFieldCodec implements LuceneStandardFieldCodec<String, String> {

	private final FieldType mainFieldType;
	private final DocValues docValues;
	private final String indexNullAsValue;
	private final Analyzer analyzerOrNormalizer;

	public LuceneStringFieldCodec(FieldType mainFieldType, DocValues docValues,
			String indexNullAsValue, Analyzer analyzerOrNormalizer) {
		this.mainFieldType = mainFieldType;
		this.docValues = docValues;
		this.indexNullAsValue = indexNullAsValue;
		this.analyzerOrNormalizer = analyzerOrNormalizer;
	}

	@Override
	public void addToDocument(LuceneDocumentContent documentBuilder, String absoluteFieldPath, String value) {
		if ( value == null && indexNullAsValue != null ) {
			value = indexNullAsValue;
		}

		if ( value == null ) {
			return;
		}

		if ( mainFieldType != null ) {
			documentBuilder.addField( new Field( absoluteFieldPath, value, mainFieldType ) );
		}

		if ( DocValues.ENABLED.equals( docValues ) ) {
			BytesRef normalized = normalize( absoluteFieldPath, value );
			documentBuilder.addField( new SortedSetDocValuesField( absoluteFieldPath, normalized ) );
		}

		if ( ( mainFieldType == null || mainFieldType.omitNorms() ) && DocValues.DISABLED.equals( docValues ) ) {
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
