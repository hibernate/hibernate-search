/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaNamedPredicateOptionsStep;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.search.predicate.definition.PredicateDefinition;

public class ElasticsearchIndexNamedPredicateOptions implements IndexSchemaNamedPredicateOptionsStep {

	public final IndexFieldInclusion inclusion;
	public final PredicateDefinition definition;

	ElasticsearchIndexNamedPredicateOptions(IndexFieldInclusion inclusion, PredicateDefinition definition) {
		this.inclusion = inclusion;
		this.definition = definition;
	}

}
