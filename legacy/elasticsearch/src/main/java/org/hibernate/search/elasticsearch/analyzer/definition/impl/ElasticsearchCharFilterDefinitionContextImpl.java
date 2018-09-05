/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.definition.impl;

import org.hibernate.search.elasticsearch.settings.impl.model.CharFilterDefinition;
import org.hibernate.search.util.StringHelper;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchCharFilterDefinitionContextImpl
		extends ElasticsearchAnalysisComponentDefinitionContextImpl<CharFilterDefinition> {

	protected ElasticsearchCharFilterDefinitionContextImpl(String name) {
		super( name, new CharFilterDefinition() );
	}

	@Override
	public void populate(ElasticsearchAnalysisDefinitionRegistry registry) {
		if ( StringHelper.isEmpty( definition.getType() ) ) {
			throw LOG.invalidElasticsearchCharFilterDefinition( name );
		}
		registry.register( name, definition );
	}

}
