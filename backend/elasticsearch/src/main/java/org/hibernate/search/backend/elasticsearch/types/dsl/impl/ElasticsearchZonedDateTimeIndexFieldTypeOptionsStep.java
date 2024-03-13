/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchZonedDateTimeFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultParseConverters;

class ElasticsearchZonedDateTimeIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchZonedDateTimeIndexFieldTypeOptionsStep,
				ZonedDateTime> {

	ElasticsearchZonedDateTimeIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, ZonedDateTime.class, DefaultParseConverters.ZONED_DATE_TIME );
	}

	@Override
	protected ElasticsearchFieldCodec<ZonedDateTime> createCodec(DateTimeFormatter formatter) {
		return new ElasticsearchZonedDateTimeFieldCodec( formatter );
	}

	@Override
	protected ElasticsearchZonedDateTimeIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
