/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.common.spi.SearchIndexIdentifierContext;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.DocumentReferenceProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.EntityProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.EntityReferenceProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.IdProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ScoreProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;

import com.google.gson.JsonObject;

public class ElasticsearchSearchProjectionBuilderFactory implements SearchProjectionBuilderFactory {

	private final ElasticsearchSearchIndexScope<?> scope;
	private final DocumentReferenceExtractionHelper documentReferenceExtractionHelper;
	private final ProjectionExtractionHelper<String> idProjectionExtractionHelper;

	public ElasticsearchSearchProjectionBuilderFactory(SearchProjectionBackendContext searchProjectionBackendContext,
			ElasticsearchSearchIndexScope<?> scope) {
		this.scope = scope;
		this.documentReferenceExtractionHelper = searchProjectionBackendContext.createDocumentReferenceExtractionHelper( scope );
		this.idProjectionExtractionHelper = searchProjectionBackendContext.idProjectionExtractionHelper();
	}

	@Override
	public DocumentReferenceProjectionBuilder documentReference() {
		return new ElasticsearchDocumentReferenceProjection.Builder( scope, documentReferenceExtractionHelper );
	}

	@Override
	public <E> EntityProjectionBuilder<E> entity() {
		return new ElasticsearchEntityProjection.Builder<>( scope, documentReferenceExtractionHelper );
	}

	@Override
	public <R> EntityReferenceProjectionBuilder<R> entityReference() {
		return new ElasticsearchEntityReferenceProjection.Builder<>( scope, documentReferenceExtractionHelper );
	}

	@Override
	public <I> IdProjectionBuilder<I> id(Class<I> identifierType) {
		SearchIndexIdentifierContext identifier = scope.identifier();
		return new ElasticsearchIdProjection.Builder<>( scope, idProjectionExtractionHelper,
				identifier.projectionConverter().withConvertedType( identifierType, identifier ) );
	}

	@Override
	public ScoreProjectionBuilder score() {
		return new ElasticsearchScoreProjection.Builder( scope );
	}

	@Override
	public CompositeProjectionBuilder composite() {
		return new ElasticsearchCompositeProjection.Builder( scope );
	}

	public SearchProjectionBuilder<JsonObject> source() {
		return new ElasticsearchSourceProjection.Builder( scope );
	}

	public SearchProjectionBuilder<JsonObject> explanation() {
		return new ElasticsearchExplanationProjection.Builder( scope );
	}

	public SearchProjectionBuilder<JsonObject> jsonHit() {
		return new ElasticsearchJsonHitProjection.Builder( scope );
	}

}
