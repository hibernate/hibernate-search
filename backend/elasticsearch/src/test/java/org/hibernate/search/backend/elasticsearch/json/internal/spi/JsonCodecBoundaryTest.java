package org.hibernate.search.backend.elasticsearch.json.internal.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class JsonCodecBoundaryTest {
	@Test
	void generatedModelCodecsUseDirectNativeNodes() throws Exception {
		Path root = Path.of( "target/generated-sources/annotations" );
		try ( var files = Files.walk( root ) ) {
			var codecs = files.filter( p -> p.getFileName().toString().endsWith( "JsonCodec.java" ) ).toList();
			assertThat( codecs ).hasSize( 15 );
			for ( Path file : codecs ) {
				assertThat( Files.readString( file ) ).as( file.toString() )
						.contains( "tools.jackson.databind.JsonNode" )
						.doesNotContain( "com.google.gson", "ObjectMapper", "JsonMapper", "java.lang.reflect", "toString()",
								"parse(" );
			}
		}
	}
}
