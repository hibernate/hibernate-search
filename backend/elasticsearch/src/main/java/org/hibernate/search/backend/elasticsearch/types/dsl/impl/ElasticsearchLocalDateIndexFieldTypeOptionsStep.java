/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchLocalDateFieldCodec;

class ElasticsearchLocalDateIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchLocalDateIndexFieldTypeOptionsStep, LocalDate> {

	ElasticsearchLocalDateIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, LocalDate.class );
	}

	@Override
	protected ElasticsearchFieldCodec<LocalDate> createCodec(DateTimeFormatter formatter) {
		return new ElasticsearchLocalDateFieldCodec( formatter );
	}

	@Override
	protected ElasticsearchLocalDateIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
