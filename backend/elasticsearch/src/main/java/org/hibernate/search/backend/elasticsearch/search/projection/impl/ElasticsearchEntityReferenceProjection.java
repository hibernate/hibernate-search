package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.reporting.impl.ElasticsearchSearchHints;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;

import tools.jackson.databind.node.ObjectNode;

public class ElasticsearchEntityReferenceProjection<R> extends AbstractElasticsearchProjection<R>
		implements ElasticsearchSearchProjection.Extractor<DocumentReference, R> {

	private final DocumentReferenceExtractionHelper helper;

	ElasticsearchEntityReferenceProjection(ElasticsearchSearchIndexScope<?> scope, DocumentReferenceExtractionHelper helper) {
		super( scope );
		this.helper = helper;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public Extractor<?, R> request(ObjectNode requestBody, ProjectionRequestContext context) {
		context.checkNotNested(
				ProjectionTypeKeys.ENTITY_REFERENCE,
				ElasticsearchSearchHints.INSTANCE.entityReferenceProjectionNestingNotSupportedHint()
		);
		helper.request( requestBody, context );
		return this;
	}

	@Override
	public DocumentReference extract(ProjectionHitMapper<?> projectionHitMapper, ObjectNode hit,
			ObjectNode source, ProjectionExtractContext context) {
		return helper.extract( hit, context );
	}

	@Override
	@SuppressWarnings("unchecked")
	public R transform(LoadingResult<?> loadingResult, DocumentReference extractedData,
			ProjectionTransformContext context) {
		return (R) loadingResult.convertReference( extractedData );
	}

}
