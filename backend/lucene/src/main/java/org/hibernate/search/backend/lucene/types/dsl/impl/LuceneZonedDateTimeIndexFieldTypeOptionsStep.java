/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.ZonedDateTime;

import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.DocValues;
import org.hibernate.search.backend.lucene.types.codec.impl.Indexing;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneZonedDateTimeFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.Storage;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultParseConverters;

class LuceneZonedDateTimeIndexFieldTypeOptionsStep
		extends AbstractLuceneTemporalIndexFieldTypeOptionsStep<LuceneZonedDateTimeIndexFieldTypeOptionsStep, ZonedDateTime> {

	LuceneZonedDateTimeIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, ZonedDateTime.class, DefaultParseConverters.ZONED_DATE_TIME );
	}

	@Override
	protected LuceneZonedDateTimeIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	@Override
	protected AbstractLuceneNumericFieldCodec<ZonedDateTime, ?> createCodec(Indexing indexing, DocValues docValues,
			Storage storage, ZonedDateTime indexNullAsValue) {
		return new LuceneZonedDateTimeFieldCodec( indexing, docValues, storage, indexNullAsValue );
	}
}
