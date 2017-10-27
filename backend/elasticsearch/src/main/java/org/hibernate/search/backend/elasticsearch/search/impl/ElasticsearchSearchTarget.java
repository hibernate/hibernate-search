/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.impl.ElasticsearchBackend;
import org.hibernate.search.backend.elasticsearch.index.impl.ElasticsearchIndexManager;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.search.ObjectLoader;
import org.hibernate.search.engine.backend.index.spi.SearchTarget;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.dsl.SearchResultDefinitionContext;
import org.hibernate.search.util.spi.LoggerFactory;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchSearchTarget implements SearchTarget {

	private static final Log log = LoggerFactory.make( Log.class );

	private final ElasticsearchBackend backend;

	// Use LinkedHashSet to ensure stable order when generating requests
	private final Set<ElasticsearchIndexManager> indexManagers = new LinkedHashSet<>();

	public ElasticsearchSearchTarget(ElasticsearchBackend backend, ElasticsearchIndexManager indexManager) {
		this.backend = backend;
		this.indexManagers.add( indexManager );
	}

	@Override
	public void add(SearchTarget other) {
		if ( ! (other instanceof ElasticsearchSearchTarget) ) {
			throw log.cannotMixElasticsearchSearchTargetWithOtherType( this, other );
		}
		ElasticsearchSearchTarget otherEs = (ElasticsearchSearchTarget) other;
		if ( ! backend.equals( otherEs.backend ) ) {
			throw log.cannotMixElasticsearchSearchTargetWithOtherBackend( this, otherEs );
		}
		indexManagers.addAll( otherEs.indexManagers );
	}

	@Override
	public <R, O> SearchResultDefinitionContext<R, O> search(
			SessionContext context, Function<DocumentReference, R> documentReferenceTransformer,
			ObjectLoader<R, O> objectLoader) {
		// Use LinkedHashSet to ensure stable order when generating requests
		Set<ElasticsearchIndexModel> indexModels = indexManagers.stream().map( ElasticsearchIndexManager::getModel )
				.collect( Collectors.toCollection( LinkedHashSet::new ) );
		QueryTargetContextImpl targetContext = new QueryTargetContextImpl( indexModels );
		return new SearchResultDefinitionContextImpl<>( backend, targetContext, context,
				documentReferenceTransformer, objectLoader );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "backend=" ).append( backend )
				.append( ", indexManagers=" ).append( indexManagers )
				.append( "]")
				.toString();
	}

}
