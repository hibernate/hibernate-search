/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFloatFieldCodec;

class ElasticsearchFloatIndexFieldTypeOptionsStep
		extends AbstractElasticsearchNumericFieldTypeOptionsStep<ElasticsearchFloatIndexFieldTypeOptionsStep, Float> {

	ElasticsearchFloatIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Float.class, DataTypes.FLOAT );
	}

	@Override
	protected ElasticsearchFieldCodec<Float> completeCodec() {
		return ElasticsearchFloatFieldCodec.INSTANCE;
	}

	@Override
	protected ElasticsearchFloatIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
