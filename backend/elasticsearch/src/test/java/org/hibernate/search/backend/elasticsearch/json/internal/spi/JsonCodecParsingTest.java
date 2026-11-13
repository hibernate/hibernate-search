package org.hibernate.search.backend.elasticsearch.json.internal.spi;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Arrays;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.util.impl.test.JsonHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@TestForIssue(jiraKey = "HSEARCH-4580")
class JsonCodecParsingTest {

	private JsonCodecService json;

	public static List<? extends Arguments> params() {
		return Arrays.asList(
				Arguments.of( "{\"_routing\": {\"required\": true}}", RootTypeMapping.class ) // HSEARCH-4580
		);
	}

	public JsonCodecParsingTest() {
		json = JsonProviderHelper.create( true ).getCodecNoSerializeNulls();
	}

	@ParameterizedTest(name = "{1}")
	@MethodSource("params")
	void parsingWithoutException(String jsonToParse, Class<?> targetType) {
		assertThatCode( () -> json.decode( jsonToParse, targetType ) ).doesNotThrowAnyException();
	}

	@ParameterizedTest(name = "{1}")
	@MethodSource("params")
	void noInformationLoss(String jsonToParse, Class<?> targetType) {
		Object parsed = json.decode( jsonToParse, targetType );
		String written = json.write( parsed );
		JsonHelper.assertJsonEquals( jsonToParse, written );
	}
}
