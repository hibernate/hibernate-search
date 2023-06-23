/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.reporting.impl;

import java.util.List;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 * Message bundle for Elasticsearch mapping validation.
 *
 */
@MessageBundle(projectCode = "HSEARCH")
public interface ElasticsearchValidationMessages {

	ElasticsearchValidationMessages INSTANCE = Messages.getBundle( ElasticsearchValidationMessages.class );

	@Message(
			value = "Validation of the existing index in the Elasticsearch cluster failed. See below for details."
	)
	String validationFailed();

	@Message(
			value = "Missing alias"
	)
	String aliasMissing();

	@Message(
			value = "Missing type mapping"
	)
	String mappingMissing();

	@Message(
			value = "Missing property mapping"
	)
	String propertyMissing();

	@Message(
			value = "Invalid value. Expected '%1$s', actual is '%2$s'"
	)
	String invalidValue(Object expectedValue, Object actualValue);

	@Message(
			value = "The output format (the first element) is invalid. Expected '%1$s', actual is '%2$s'"
	)
	String invalidOutputFormat(String expectedValue, String actualValue);

	@Message(
			value = "Invalid formats. Every required formats must be in the list,"
					+ " though it's not required to provide them in the same order, and the list must not contain unexpected formats."
					+ " Expected '%1$s', actual is '%2$s', missing elements are '%3$s', unexpected elements are '%4$s'."
	)
	String invalidFormat(List<String> expectedValue,
			List<String> actualValue, List<String> missingFormats, List<String> unexpectedFormats);

	@Message(
			value = "Missing analyzer definition"
	)
	String analyzerMissing();

	@Message(
			value = "Missing normalizer definition"
	)
	String normalizerMissing();

	@Message(
			value = "Invalid char filters. Expected '%1$s', actual is '%2$s'"
	)
	String invalidAnalyzerCharFilters(Object expected, Object actual);

	@Message(
			value = "Invalid tokenizer. Expected '%1$s', actual is '%2$s'"
	)
	String invalidAnalyzerTokenizer(Object expected, Object actual);

	@Message(
			value = "Invalid token filters. Expected '%1$s', actual is '%2$s'"
	)
	String invalidAnalyzerTokenFilters(Object expected, Object actual);

	@Message(
			value = "Missing char filter definition"
	)
	String charFilterMissing();

	@Message(
			value = "Missing tokenizer definition"
	)
	String tokenizerMissing();

	@Message(
			value = "Missing token filter definition"
	)
	String tokenFilterMissing();

	@Message(
			value = "Invalid type. Expected '%1$s', actual is '%2$s'"
	)
	String invalidAnalysisDefinitionType(String expected, String actual);

	@Message(
			value = "Invalid order for dynamic field templates. Expected %1$s, actual is %2$s"
	)
	String dynamicTemplatesInvalidOrder(List<String> expectedValue, List<String> actualValue);

	@Message(
			value = "Missing dynamic field template"
	)
	String dynamicTemplateMissing();

	@Message(
			value = "Unexpected dynamic field template"
	)
	String dynamicTemplateUnexpected();

	@Message(
			value = "Multiple dynamic field templates with this name. The names of dynamic field template must be unique."
	)
	String dynamicTemplateDuplicate();

	@Message(
			value = "Custom index setting attribute missing"
	)
	String customIndexSettingAttributeMissing();

	@Message(
			value = "Custom index mapping attribute missing"
	)
	String customIndexMappingAttributeMissing();

}
