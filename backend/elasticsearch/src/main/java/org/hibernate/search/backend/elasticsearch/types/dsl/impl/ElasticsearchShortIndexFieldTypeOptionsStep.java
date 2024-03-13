/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchShortFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultParseConverters;

class ElasticsearchShortIndexFieldTypeOptionsStep
		extends AbstractElasticsearchNumericFieldTypeOptionsStep<ElasticsearchShortIndexFieldTypeOptionsStep, Short> {

	ElasticsearchShortIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Short.class, DataTypes.SHORT, DefaultParseConverters.SHORT );
	}

	@Override
	protected ElasticsearchFieldCodec<Short> completeCodec() {
		return ElasticsearchShortFieldCodec.INSTANCE;
	}

	@Override
	protected ElasticsearchShortIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
