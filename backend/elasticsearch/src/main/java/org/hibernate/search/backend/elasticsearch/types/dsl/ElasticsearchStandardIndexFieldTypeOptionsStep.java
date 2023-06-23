/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl;

import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;

public interface ElasticsearchStandardIndexFieldTypeOptionsStep<
		S extends ElasticsearchStandardIndexFieldTypeOptionsStep<?, F>,
		F>
		extends StandardIndexFieldTypeOptionsStep<S, F> {

}
