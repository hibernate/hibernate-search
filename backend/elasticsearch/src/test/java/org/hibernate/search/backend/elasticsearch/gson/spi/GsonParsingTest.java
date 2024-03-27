/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.gson.spi;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Arrays;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.util.impl.test.JsonHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@TestForIssue(jiraKey = "HSEARCH-4580")
class GsonParsingTest {

	private Gson gson;

	public static List<? extends Arguments> params() {
		return Arrays.asList(
				Arguments.of( "{\"_routing\": {\"required\": true}}", RootTypeMapping.class ) // HSEARCH-4580
		);
	}

	public GsonParsingTest() {
		gson = GsonProvider.create( GsonBuilder::new, true ).getGsonNoSerializeNulls();
	}

	@ParameterizedTest(name = "{1}")
	@MethodSource("params")
	void parsingWithoutException(String jsonToParse, Class<?> targetType) {
		assertThatCode( () -> gson.fromJson( jsonToParse, targetType ) ).doesNotThrowAnyException();
	}

	@ParameterizedTest(name = "{1}")
	@MethodSource("params")
	void noInformationLoss(String jsonToParse, Class<?> targetType) {
		Object parsed = gson.fromJson( jsonToParse, targetType );
		String written = gson.toJson( parsed );
		JsonHelper.assertJsonEquals( jsonToParse, written );
	}
}
