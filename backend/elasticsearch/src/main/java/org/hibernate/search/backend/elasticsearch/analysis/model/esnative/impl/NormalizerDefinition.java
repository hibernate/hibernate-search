/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.esnative.impl;

import com.google.gson.annotations.JsonAdapter;

/**
 * A definition of an Elasticsearch normalizer, to be included in index settings.
 *
 */
@JsonAdapter(NormalizerDefinitionJsonAdapterFactory.class)
public class NormalizerDefinition extends AbstractCompositeAnalysisDefinition {

}
