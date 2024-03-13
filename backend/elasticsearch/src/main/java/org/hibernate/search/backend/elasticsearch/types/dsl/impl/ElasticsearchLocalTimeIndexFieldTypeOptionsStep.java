/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchLocalTimeFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultParseConverters;

class ElasticsearchLocalTimeIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchLocalTimeIndexFieldTypeOptionsStep, LocalTime> {

	ElasticsearchLocalTimeIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, LocalTime.class, DefaultParseConverters.LOCAL_TIME );
	}

	@Override
	protected ElasticsearchFieldCodec<LocalTime> createCodec(DateTimeFormatter formatter) {
		return new ElasticsearchLocalTimeFieldCodec( formatter );
	}

	@Override
	protected ElasticsearchLocalTimeIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
