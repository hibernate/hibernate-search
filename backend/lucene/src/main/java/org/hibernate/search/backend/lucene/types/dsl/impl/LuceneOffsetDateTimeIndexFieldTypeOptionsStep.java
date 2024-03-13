/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.OffsetDateTime;

import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.DocValues;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneOffsetDateTimeFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultParseConverters;

class LuceneOffsetDateTimeIndexFieldTypeOptionsStep
		extends AbstractLuceneTemporalIndexFieldTypeOptionsStep<LuceneOffsetDateTimeIndexFieldTypeOptionsStep, OffsetDateTime> {

	LuceneOffsetDateTimeIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, OffsetDateTime.class, DefaultParseConverters.OFFSET_DATE_TIME );
	}

	@Override
	protected LuceneOffsetDateTimeIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<OffsetDateTime, ?> createCodec(Indexing indexing, DocValues docValues,
			Storage storage, OffsetDateTime indexNullAsValue) {
		return new LuceneOffsetDateTimeFieldCodec( indexing, docValues, storage, indexNullAsValue );
	}
}
