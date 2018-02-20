/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.util.SearchException;

/**
 * A schema node builder.
 */
abstract class AbstractIndexSchemaNodeBuilder {

	private final Map<String, LuceneIndexSchemaNodeContributor> content = new HashMap<>();

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( ",absolutePath=" ).append( getAbsolutePath() )
				.append( "]" )
				.toString();
	}

	public abstract String getAbsolutePath();

	public void putProperty(String name, LuceneIndexSchemaNodeContributor contributor) {
		Object previous = content.putIfAbsent( name, contributor );
		if ( previous != null ) {
			// TODO more explicit error message
			throw new SearchException( "The index model node '" + name + "' was added twice at path '" + getAbsolutePath() + "'."
					+ " Multiple bridges may be trying to access the same index field, "
					+ " or two indexedEmbeddeds may have prefixes that end up mixing fields together,"
					+ " or you may have declared multiple conflicting mappings."
					+ " In any case, there is something wrong with your mapping and you should fix it." );
		}
	}

	final void contributeChildren(LuceneIndexSchemaObjectNode node, LuceneIndexSchemaNodeCollector collector) {
		for ( LuceneIndexSchemaNodeContributor contributor : content.values() ) {
			contributor.contribute( collector, node );
		}
	}
}
