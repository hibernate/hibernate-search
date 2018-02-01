/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import org.hibernate.search.backend.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.engine.backend.index.spi.DocumentContributor;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchDocumentObjectBuilder;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkFactory;
import org.hibernate.search.engine.backend.index.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.index.spi.IndexWorker;
import org.hibernate.search.engine.common.spi.SessionContext;


/**
 * @author Yoann Rodiere
 */
public abstract class ElasticsearchIndexWorker implements IndexWorker<ElasticsearchDocumentObjectBuilder> {

	protected final ElasticsearchWorkFactory factory;
	protected final URLEncodedString indexName;
	protected final URLEncodedString typeName;
	protected final String tenantId;

	public ElasticsearchIndexWorker(ElasticsearchWorkFactory factory, URLEncodedString indexName,
			URLEncodedString typeName, SessionContext context) {
		this.factory = factory;
		this.indexName = indexName;
		this.typeName = typeName;
		this.tenantId = context.getTenantIdentifier();
	}

	@Override
	public void add(DocumentReferenceProvider referenceProvider,
			DocumentContributor<ElasticsearchDocumentObjectBuilder> documentContributor) {
		String id = toActualId( referenceProvider.getIdentifier() );
		String routingKey = referenceProvider.getRoutingKey();
		ElasticsearchDocumentObjectBuilder builder = new ElasticsearchDocumentObjectBuilder();
		documentContributor.contribute( builder );
		collect( factory.add( indexName, typeName, id, routingKey, builder.build() ) );
	}

	@Override
	public void update(DocumentReferenceProvider referenceProvider,
			DocumentContributor<ElasticsearchDocumentObjectBuilder> documentContributor) {
		String id = toActualId( referenceProvider.getIdentifier() );
		String routingKey = referenceProvider.getRoutingKey();
		ElasticsearchDocumentObjectBuilder builder = new ElasticsearchDocumentObjectBuilder();
		documentContributor.contribute( builder );
		collect( factory.update( indexName, typeName, id, routingKey, builder.build() ) );
	}

	@Override
	public void delete(DocumentReferenceProvider referenceProvider) {
		String id = toActualId( referenceProvider.getIdentifier() );
		String routingKey = referenceProvider.getRoutingKey();
		collect( factory.delete( indexName, typeName, id, routingKey ) );
	}

	protected final String toActualId(String id) {
		return tenantId == null ? id : tenantId + "_" + id;
	}

	protected abstract void collect(ElasticsearchWork<?> work);

}
