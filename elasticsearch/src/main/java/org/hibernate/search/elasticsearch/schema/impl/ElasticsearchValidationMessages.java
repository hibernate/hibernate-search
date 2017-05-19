/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl;

import java.util.List;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 * Message bundle for Elasticsearch mapping validation.
 *
 * @author Yoann Rodiere
 */
@MessageBundle(projectCode = "HSEARCH")
public interface ElasticsearchValidationMessages {

	@Message(
			value = "index '%1$s'"
	)
	String indexContext(String name);

	@Message(
			value = "mapping '%1$s'"
	)
	String mappingContext(String name);

	@Message(
			value = "property '%1$s'"
	)
	String mappingPropertyContext(String path);

	@Message(
			value = "field '%1$s'"
	)
	String mappingPropertyFieldContext(String name);

	@Message(
			value = "analyzer '%1$s'"
	)
	String analyzerContext(String name);

	@Message(
			value = "normalizer '%1$s'"
	)
	String normalizerContext(String name);

	@Message(
			value = "char filter '%1$s'"
	)
	String charFilterContext(String name);

	@Message(
			value = "tokenizer '%1$s'"
	)
	String tokenizerContext(String name);

	@Message(
			value = "token filter '%1$s'"
	)
	String tokenFilterContext(String name);

	@Message(
			value = "Missing type mapping"
	)
	String mappingMissing();

	@Message(
			value = "Missing property mapping"
	)
	String propertyMissing();

	@Message(
			value = "Missing field mapping"
	)
	String propertyFieldMissing();

	@Message(
			value = "Invalid value for attribute '%1$s'. Expected '%2$s', actual is '%3$s'"
	)
	String invalidAttributeValue(String string, Object expectedValue, Object actualValue);

	@Message(
			value = "The output format (the first format in the '%1$s' attribute) is invalid. Expected '%2$s', actual is '%3$s'"
	)
	String invalidOutputFormat(String string, String expectedValue, String actualValue);

	@Message(
			value = "Invalid formats for attribute '%1$s'. Every required formats must be in the list,"
			+ " though it's not required to provide them in the same order, and the list must not contain unexpected formats."
			+ " Expected '%2$s', actual is '%3$s', missing elements are '%4$s', unexpected elements are '%5$s'."
	)
	String invalidInputFormat(String string, List<String> expectedValue,
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
			value = "Invalid value for parameter '%1$s'. Expected '%2$s', actual is '%3$s'"
	)
	String invalidAnalysisDefinitionParameter(String name, Object expected, Object actual);
}
