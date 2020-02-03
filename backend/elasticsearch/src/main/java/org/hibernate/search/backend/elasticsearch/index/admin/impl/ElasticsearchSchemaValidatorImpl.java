/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.admin.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.impl.IndexMetadata;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.validation.impl.IndexSettingsValidator;
import org.hibernate.search.backend.elasticsearch.validation.impl.RootTypeMappingValidator;
import org.hibernate.search.backend.elasticsearch.validation.impl.ValidationErrorCollector;
import org.hibernate.search.backend.elasticsearch.validation.impl.Validator;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings;
import org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchEventContexts;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;

/**
 * The default {@link ElasticsearchSchemaValidator} implementation.
 * <p>
 * <strong>Important implementation note:</strong> unexpected attributes (i.e. those not mapped to a field in TypeMapping)
 * are totally ignored. This allows users to leverage Elasticsearch features that are not supported in
 * Hibernate Search, by setting those attributes manually.
 */
public class ElasticsearchSchemaValidatorImpl implements ElasticsearchSchemaValidator {

	private final Validator<IndexSettings> indexSettingsValidator = new IndexSettingsValidator();
	private final Validator<RootTypeMapping> rootTypeMappingValidator = new RootTypeMappingValidator();

	@Override
	public void validate(IndexMetadata expectedIndexMetadata, IndexMetadata actualIndexMetadata,
			ContextualFailureCollector contextualFailureCollector) {
		ValidationErrorCollector errorCollector = new ValidationErrorCollector(
				contextualFailureCollector.withContext( ElasticsearchEventContexts.getSchemaValidation() )
		);

		validateSettings( errorCollector, expectedIndexMetadata, actualIndexMetadata );

		rootTypeMappingValidator.validate(
				errorCollector, expectedIndexMetadata.getMapping(), actualIndexMetadata.getMapping()
		);
	}

	@Override
	public boolean isSettingsValid(IndexMetadata expectedIndexMetadata, IndexMetadata actualIndexMetadata) {
		ValidationErrorCollector errorCollector = new ValidationErrorCollector();

		validateSettings( errorCollector, expectedIndexMetadata, actualIndexMetadata );

		return !errorCollector.hasError();
	}

	private void validateSettings(ValidationErrorCollector errorCollector,
			IndexMetadata expectedIndexMetadata, IndexMetadata actualIndexMetadata) {
		indexSettingsValidator.validate(
				errorCollector, expectedIndexMetadata.getSettings(), actualIndexMetadata.getSettings()
		);
	}

}
