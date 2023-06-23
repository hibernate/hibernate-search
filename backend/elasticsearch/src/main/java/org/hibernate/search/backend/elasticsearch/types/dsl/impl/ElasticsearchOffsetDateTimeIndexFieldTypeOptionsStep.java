/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchOffsetDateTimeFieldCodec;

class ElasticsearchOffsetDateTimeIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchOffsetDateTimeIndexFieldTypeOptionsStep,
				OffsetDateTime> {

	ElasticsearchOffsetDateTimeIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, OffsetDateTime.class );
	}

	@Override
	protected ElasticsearchFieldCodec<OffsetDateTime> createCodec(DateTimeFormatter formatter) {
		return new ElasticsearchOffsetDateTimeFieldCodec( formatter );
	}

	@Override
	protected ElasticsearchOffsetDateTimeIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
