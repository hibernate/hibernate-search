/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchOffsetTimeFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultParseConverters;

class ElasticsearchOffsetTimeIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchOffsetTimeIndexFieldTypeOptionsStep, OffsetTime> {

	ElasticsearchOffsetTimeIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, OffsetTime.class, DefaultParseConverters.OFFSET_TIME );
	}

	@Override
	protected ElasticsearchFieldCodec<OffsetTime> createCodec(DateTimeFormatter formatter) {
		return new ElasticsearchOffsetTimeFieldCodec( formatter );
	}

	@Override
	protected ElasticsearchOffsetTimeIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
