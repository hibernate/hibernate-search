/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchStandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.util.common.AssertionFailure;

abstract class AbstractElasticsearchStandardIndexFieldTypeOptionsStep<
		S extends AbstractElasticsearchStandardIndexFieldTypeOptionsStep<?, F>,
		F>
		extends AbstractElasticsearchIndexFieldTypeOptionsStep<S, F>
		implements ElasticsearchStandardIndexFieldTypeOptionsStep<S, F> {

	AbstractElasticsearchStandardIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext,
			Class<F> fieldType) {
		super( buildContext, fieldType, new PropertyMapping() );
	}

	protected static boolean resolveDefault(Projectable projectable) {
		switch ( projectable ) {
			case DEFAULT:
			case YES:
				return true;
			case NO:
				return false;
			default:
				throw new AssertionFailure( "Unexpected value for Projectable: " + projectable );
		}
	}

	protected static boolean resolveDefault(Searchable searchable) {
		switch ( searchable ) {
			case DEFAULT:
			case YES:
				return true;
			case NO:
				return false;
			default:
				throw new AssertionFailure( "Unexpected value for Searchable: " + searchable );
		}
	}

	protected static boolean resolveDefault(Sortable sortable) {
		switch ( sortable ) {
			case DEFAULT:
			case NO:
				return false;
			case YES:
				return true;
			default:
				throw new AssertionFailure( "Unexpected value for Sortable: " + sortable );
		}
	}

	protected static boolean resolveDefault(Aggregable aggregable) {
		switch ( aggregable ) {
			case DEFAULT:
			case NO:
				return false;
			case YES:
				return true;
			default:
				throw new AssertionFailure( "Unexpected value for Aggregable: " + aggregable );
		}
	}
}
