/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchShortFieldCodec;

class ElasticsearchShortIndexFieldTypeOptionsStep
		extends AbstractElasticsearchScalarFieldTypeOptionsStep<ElasticsearchShortIndexFieldTypeOptionsStep, Short> {

	ElasticsearchShortIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Short.class, DataTypes.SHORT );
	}

	@Override
	protected ElasticsearchFieldCodec<Short> complete(PropertyMapping mapping) {
		return ElasticsearchShortFieldCodec.INSTANCE;
	}

	@Override
	protected ElasticsearchShortIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
