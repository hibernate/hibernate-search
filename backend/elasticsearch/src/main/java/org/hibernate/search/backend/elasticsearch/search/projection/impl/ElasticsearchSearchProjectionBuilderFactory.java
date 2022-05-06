/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.common.spi.SearchIndexIdentifierContext;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
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
	public SearchProjection<DocumentReference> documentReference() {
		return new ElasticsearchDocumentReferenceProjection( scope, documentReferenceExtractionHelper );
	}

	@Override
	public <E> SearchProjection<E> entity() {
		return new ElasticsearchEntityProjection<>( scope, documentReferenceExtractionHelper );
	}

	@Override
	public <R> SearchProjection<R> entityReference() {
		return new ElasticsearchEntityReferenceProjection<>( scope, documentReferenceExtractionHelper );
	}

	@Override
	public <I> SearchProjection<I> id(Class<I> identifierType) {
		SearchIndexIdentifierContext identifier = scope.identifier();
		return new ElasticsearchIdProjection<>( scope, idProjectionExtractionHelper,
				identifier.projectionConverter().withConvertedType( identifierType, identifier ) );
	}

	@Override
	public SearchProjection<Float> score() {
		return new ElasticsearchScoreProjection( scope );
	}

	@Override
	public CompositeProjectionBuilder composite() {
		return new ElasticsearchCompositeProjection.Builder( scope );
	}

	public SearchProjection<JsonObject> source() {
		return new ElasticsearchSourceProjection( scope );
	}

	public SearchProjection<JsonObject> explanation() {
		return new ElasticsearchExplanationProjection( scope );
	}

	public SearchProjection<JsonObject> jsonHit() {
		return new ElasticsearchJsonHitProjection( scope );
	}

}
