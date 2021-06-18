/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.codec.Charsets;
import org.apache.http.Header;
import org.apache.http.nio.ContentEncoder;

@RunWith(Parameterized.class)
public class GsonHttpEntityTest {

	@Parameterized.Parameters(name = "{0}")
	public static List<Object[]> params() {
		JsonObject bodyPart1 = new JsonParser().parse( "{ \"foo\": \"bar\" }" ).getAsJsonObject();
		JsonObject bodyPart2 = new JsonParser().parse( "{ \"foobar\": 235 }" ).getAsJsonObject();
		JsonObject bodyPart3 = new JsonParser().parse( "{ \"obj1\": " + bodyPart1.toString()
				+ ", \"obj2\": " + bodyPart2.toString() + "}" ).getAsJsonObject();
		List<Object[]> params = new ArrayList<>();

		for ( @SuppressWarnings("unchecked") List<JsonObject> jsonObjects: new List[] {
				Collections.emptyList(),
				Collections.singletonList( bodyPart1 ),
				Collections.singletonList( bodyPart2 ),
				Collections.singletonList( bodyPart3 ),
				Arrays.asList( bodyPart1, bodyPart2, bodyPart3 ),
				Arrays.asList( bodyPart3, bodyPart2, bodyPart1 )
		} ) {
			params.add( new Object[] { jsonObjects.toString(), jsonObjects } );
		}
		params.add( new Object[] {
				"50 small objects",
				Stream.generate( () -> Arrays.asList( bodyPart1, bodyPart2, bodyPart3 ) )
						.flatMap( List::stream ).limit( 50 ).collect( Collectors.toList() ) } );
		params.add( new Object[] {
				"200 small objects",
				Stream.generate( () -> Arrays.asList( bodyPart1, bodyPart2, bodyPart3 ) )
						.flatMap( List::stream ).limit( 200 ).collect( Collectors.toList() ) } );
		params.add( new Object[] {
				"10,000 small objects",
				Stream.generate( () -> Arrays.asList( bodyPart1, bodyPart2, bodyPart3 ) )
						.flatMap( List::stream ).limit( 10_000 ).collect( Collectors.toList() ) } );
		params.add( new Object[] {
				"200 large objects",
				Stream.generate( () -> {
					// Generate one large object
					JsonObject object = new JsonObject();
					JsonArray array = new JsonArray();
					object.add( "array", array );
					Stream.generate( () -> Arrays.asList( bodyPart1, bodyPart2, bodyPart3 ) )
							.flatMap( List::stream ).limit( 1_000 ).forEach( array::add );
					return object;
				} )
						// Reproduce the large object multiple times
						.limit( 200 ).collect( Collectors.toList() ) } );
		params.add( new Object[] {
				"1 very large object",
				Stream.generate( () -> {
					JsonObject object = new JsonObject();
					JsonArray array = new JsonArray();
					object.add( "array", array );
					Stream.generate( () -> Arrays.asList( bodyPart1, bodyPart2, bodyPart3 ) )
							.flatMap( List::stream ).limit( 100_000 ).forEach( array::add );
					return object;
				} )
						// Reproduce the large object multiple times
						.limit( 1 ).collect( Collectors.toList() ) } );

		return params;
	}

	private final List<JsonObject> payload;
	private final GsonHttpEntity gsonEntity;
	private final String expectedPayloadString;
	private final int expectedContentLength;

	@SuppressWarnings("unused")
	public GsonHttpEntityTest(String ignoredLabel, List<JsonObject> payload) throws IOException {
		this.payload = payload;
		Gson gson = GsonProvider.create( GsonBuilder::new, true ).getGson();
		this.gsonEntity = new GsonHttpEntity( gson, payload );
		StringBuilder builder = new StringBuilder();
		for ( JsonObject object : payload ) {
			gson.toJson( object, builder );
			builder.append( "\n" );
		}
		this.expectedPayloadString = builder.toString();
		this.expectedContentLength = Charsets.UTF_8.encode( expectedPayloadString ).limit();
	}

	@Test
	public void initialContentLength() {
		// The content length cannot be known from the start for large, multi-object payloads
		assumeTrue( payload.size() <= 1 || expectedContentLength < 1024 );

		assertThat( gsonEntity.getContentLength() ).isEqualTo( expectedContentLength );
	}

	@Test
	public void contentType() {
		Header contentType = gsonEntity.getContentType();
		assertThat( contentType.getName() ).isEqualTo( "Content-Type" );
		assertThat( contentType.getValue() ).isEqualTo( "application/json; charset=UTF-8" );
	}

	@Test
	public void produceContent_noPushBack() throws IOException {
		int pushBackPeriod = Integer.MAX_VALUE;
		for ( int i = 0; i < 2; i++ ) { // Try several times: the result shouldn't change.
			assertThat( doProduceContent( gsonEntity, pushBackPeriod ) )
					.isEqualTo( expectedPayloadString );
			assertThat( gsonEntity.getContentLength() )
					.isEqualTo( expectedContentLength );
		}
	}

	@Test
	public void produceContent_pushBack_every5Bytes() throws IOException {
		int pushBackPeriod = 5;
		for ( int i = 0; i < 2; i++ ) { // Try several times: the result shouldn't change.
			assertThat( doProduceContent( gsonEntity, pushBackPeriod ) )
					.isEqualTo( expectedPayloadString );
			assertThat( gsonEntity.getContentLength() )
					.isEqualTo( expectedContentLength );
		}
	}

	@Test
	public void produceContent_pushBack_every100Bytes() throws IOException {
		int pushBackPeriod = 100;
		for ( int i = 0; i < 2; i++ ) { // Try several times: the result shouldn't change.
			assertThat( doProduceContent( gsonEntity, pushBackPeriod ) )
					.isEqualTo( expectedPayloadString );
			assertThat( gsonEntity.getContentLength() )
					.isEqualTo( expectedContentLength );
		}
	}

	@Test
	public void produceContent_pushBack_every500Bytes() throws IOException {
		int pushBackPeriod = 500;
		for ( int i = 0; i < 2; i++ ) { // Try several times: the result shouldn't change.
			assertThat( doProduceContent( gsonEntity, pushBackPeriod ) )
					.isEqualTo( expectedPayloadString );
			assertThat( gsonEntity.getContentLength() )
					.isEqualTo( expectedContentLength );
		}
	}

	@Test
	public void writeTo() throws IOException {
		for ( int i = 0; i < 2; i++ ) { // Try several times: the result shouldn't change.
			assertThat( doWriteTo( gsonEntity ) )
					.isEqualTo( expectedPayloadString );
			assertThat( gsonEntity.getContentLength() )
					.isEqualTo( expectedContentLength );
		}
	}

	@Test
	public void getContent() throws IOException {
		for ( int i = 0; i < 2; i++ ) { // Try several times: the result shouldn't change.
			assertThat( doGetContent( gsonEntity ) )
					.isEqualTo( expectedPayloadString );
			assertThat( gsonEntity.getContentLength() )
					.isEqualTo( expectedContentLength );
		}
	}

	private String doProduceContent(GsonHttpEntity entity, int pushBackPeriod) throws IOException {
		try ( ByteArrayOutputStream outputStream = new ByteArrayOutputStream() ) {
			ContentEncoder contentEncoder = new OutputStreamContentEncoder( outputStream, pushBackPeriod );
			while ( !contentEncoder.isCompleted() ) {
				entity.produceContent( contentEncoder, StubIOControl.INSTANCE );
			}
			return outputStream.toString( Charsets.UTF_8.name() );
		}
		finally {
			entity.close();
		}
	}

	private String doWriteTo(GsonHttpEntity entity) throws IOException {
		try ( ByteArrayOutputStream outputStream = new ByteArrayOutputStream() ) {
			entity.writeTo( outputStream );
			return outputStream.toString( Charsets.UTF_8.name() );
		}
	}

	private String doGetContent(GsonHttpEntity entity) throws IOException {
		try ( InputStream inputStream = entity.getContent();
				Reader reader = new InputStreamReader( inputStream, Charsets.UTF_8 );
				BufferedReader bufferedReader = new BufferedReader( reader ) ) {
			StringBuilder builder = new StringBuilder();
			int read;
			while ( ( read = bufferedReader.read() ) >= 0 ) {
				builder.appendCodePoint( read );
			}
			return builder.toString();
		}
	}

	private static class OutputStreamContentEncoder implements ContentEncoder {
		private boolean complete = false;
		private final OutputStream outputStream;
		private final int pushBackPeriod;

		private int written = 0;
		private boolean pushedBack = false;

		private OutputStreamContentEncoder(OutputStream outputStream, int pushBackPeriod) {
			this.outputStream = outputStream;
			this.pushBackPeriod = pushBackPeriod;
		}

		@Override
		public int write(ByteBuffer src) throws IOException {
			int toWrite = src.remaining();
			if ( !pushedBack && ( written % pushBackPeriod ) != ( ( written + toWrite ) % pushBackPeriod ) ) {
				// push back
				pushedBack = true;
				return 0;
			}
			pushedBack = false;
			outputStream.write( src.array(), src.arrayOffset(), toWrite );
			written += toWrite;
			return toWrite;
		}

		@Override
		public void complete() {
			this.complete = true;
		}

		@Override
		public boolean isCompleted() {
			return this.complete;
		}
	}
}