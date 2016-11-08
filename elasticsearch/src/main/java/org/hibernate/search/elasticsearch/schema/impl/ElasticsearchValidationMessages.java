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
			value = "Index '%1$s':"
	)
	String errorIntro(String indexName);

	@Message(
			value = "Index '%1$s', mapping '%2$s':"
	)
	String errorIntro(String indexName, String mappingName);

	@Message(
			value = "Index '%1$s', mapping '%2$s', property '%3$s':"
	)
	String errorIntro(String indexName, String mappingName, String path);

	@Message(
			value = "Index '%1$s', mapping '%2$s', property '%3$s', field '%4$s':"
	)
	String errorIntro(String indexName, String mappingName, String path, String field);

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

}
