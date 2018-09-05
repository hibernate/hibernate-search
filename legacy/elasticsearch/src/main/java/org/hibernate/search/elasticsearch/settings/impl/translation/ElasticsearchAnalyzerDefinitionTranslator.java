/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl.translation;

import org.hibernate.search.annotations.CharFilterDef;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.elasticsearch.settings.impl.model.CharFilterDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenFilterDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenizerDefinition;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.exception.SearchException;

/**
 * An object responsible for translating a Hibernate Search analyzer-related definition to Elasticsearch definitions.
 *
 * @author Yoann Rodiere
 */
public interface ElasticsearchAnalyzerDefinitionTranslator extends Service {

	/**
	 * Translate a Lucene analyzer class, throwing an exception if translation fails.
	 *
	 * @param luceneClass The Lucene analyzer class to be translated.
	 * @return The name of this analyzer type on Elasticsearch.
	 * @throws SearchException If an error occurs.
	 */
	String translate(Class<?> luceneClass);

	/**
	 * Translate a tokenizer definition, throwing an exception if translation fails.
	 *
	 * @param hibernateSearchDef The Hibernate Search definition to be translated.
	 * @return The Elasticsearch translation.
	 * @throws SearchException If an error occurs.
	 */
	TokenizerDefinition translate(TokenizerDef hibernateSearchDef);

	/**
	 * Translate a char filter definition, throwing an exception if translation fails.
	 *
	 * @param hibernateSearchDef The Hibernate Search definition to be translated.
	 * @return The Elasticsearch translation.
	 * @throws SearchException If an error occurs.
	 */
	CharFilterDefinition translate(CharFilterDef hibernateSearchDef);

	/**
	 * Translate a token filter definition, throwing an exception if translation fails.
	 *
	 * @param hibernateSearchDef The Hibernate Search definition to be translated.
	 * @return The Elasticsearch translation.
	 * @throws SearchException If an error occurs.
	 */
	TokenFilterDefinition translate(TokenFilterDef hibernateSearchDef);

}
