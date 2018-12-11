/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.converter.impl;

import java.util.Objects;

import org.apache.lucene.analysis.Analyzer;

import org.hibernate.search.engine.backend.document.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.spi.UserDocumentFieldConverter;
import org.hibernate.search.backend.lucene.util.impl.AnalyzerUtils;

public final class LuceneStringFieldConverter extends AbstractLuceneFieldConverter<String, String> {

	private final Analyzer analyzerOrNormalizer;

	public LuceneStringFieldConverter(UserDocumentFieldConverter<String> userConverter, Analyzer analyzerOrNormalizer) {
		super( userConverter );
		this.analyzerOrNormalizer = analyzerOrNormalizer;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + userConverter + "," + analyzerOrNormalizer + "]";
	}

	@Override
	public String convertDslToIndex(Object value,
			ToDocumentFieldValueConvertContext context) {
		return userConverter.convertDslToIndex( value, context );
	}

	@Override
	public boolean isConvertDslToIndexCompatibleWith(LuceneFieldConverter<?, ?> other) {
		if ( !super.isConvertDslToIndexCompatibleWith( other ) ) {
			return false;
		}

		LuceneStringFieldConverter castedOther = (LuceneStringFieldConverter) other;
		return Objects.equals( analyzerOrNormalizer, castedOther.analyzerOrNormalizer );
	}

	public String normalize(String absoluteFieldPath, String value) {
		if ( value == null ) {
			return null;
		}

		if ( analyzerOrNormalizer == null ) {
			return value;
		}

		return AnalyzerUtils.normalize( analyzerOrNormalizer, absoluteFieldPath, value );
	}
}
