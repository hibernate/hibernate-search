/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchDocumentObjectBuilder;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.impl.ElasticsearchBackend;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkFactory;
import org.hibernate.search.engine.backend.index.spi.ChangesetIndexWorker;
import org.hibernate.search.engine.backend.index.spi.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTargetBuilder;
import org.hibernate.search.engine.backend.index.spi.StreamIndexWorker;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.util.impl.common.LoggerFactory;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchIndexManager implements IndexManager<ElasticsearchDocumentObjectBuilder> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchBackend backend;
	private final URLEncodedString name;
	private final URLEncodedString typeName;
	private final ElasticsearchIndexModel model;
	private final ElasticsearchWorkFactory workFactory;
	private final ElasticsearchWorkOrchestrator changesetOrchestrator;
	private final ElasticsearchWorkOrchestrator streamOrchestrator;

	public ElasticsearchIndexManager(ElasticsearchBackend backend, URLEncodedString name, URLEncodedString typeName,
			ElasticsearchIndexModel model) {
		this.backend = backend;
		this.name = name;
		this.typeName = typeName;
		this.model = model;
		this.workFactory = backend.getWorkFactory();
		this.changesetOrchestrator = backend.createChangesetOrchestrator();
		this.streamOrchestrator = backend.getStreamOrchestrator();
	}

	@Override
	public void close() {
		changesetOrchestrator.close();
	}

	public String getName() {
		return name.original;
	}

	public ElasticsearchIndexModel getModel() {
		return model;
	}

	@Override
	public ChangesetIndexWorker<ElasticsearchDocumentObjectBuilder> createWorker(SessionContext context) {
		return new ElasticsearchChangesetIndexWorker( workFactory, changesetOrchestrator, name, typeName, context );
	}

	@Override
	public StreamIndexWorker<ElasticsearchDocumentObjectBuilder> createStreamWorker(SessionContext context) {
		return new ElasticsearchStreamIndexWorker( workFactory, streamOrchestrator, name, typeName, context );
	}

	@Override
	public IndexSearchTargetBuilder createSearchTarget() {
		return new ElasticsearchIndexSearchTargetBuilder( backend, this );
	}

	@Override
	public void addToSearchTarget(IndexSearchTargetBuilder searchTargetBuilder) {
		if ( ! (searchTargetBuilder instanceof ElasticsearchIndexSearchTargetBuilder ) ) {
			throw log.cannotMixElasticsearchSearchTargetWithOtherType( searchTargetBuilder, this );
		}

		ElasticsearchIndexSearchTargetBuilder esSearchTargetBuilder = (ElasticsearchIndexSearchTargetBuilder) searchTargetBuilder;
		esSearchTargetBuilder.add( backend, this );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "name=" ).append( name.original )
				.append( "]")
				.toString();
	}


}
