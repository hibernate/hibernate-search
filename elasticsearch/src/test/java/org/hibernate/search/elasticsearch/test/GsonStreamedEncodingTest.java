/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.elasticsearch.test;

import static java.util.Collections.singletonList;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.apache.commons.codec.digest.DigestUtils.getSha256Digest;
import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.hibernate.search.elasticsearch.impl.JsonBuilder;
import org.hibernate.search.elasticsearch.util.impl.GsonHttpEntity;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Tests for GsonHttpEntity to be able to write the whole JSON string
 * out correctly, and produce a matching sha256 digest.
 *
 * @author Sanne Grinovero (C) 2017 Red Hat Inc.
 */
@TestForIssue( jiraKey = "HSEARCH-2818" )
public class GsonStreamedEncodingTest {

	public static final int MAX_TESTING_BUFFER_BYTES = 4000;
	private static final String JSON_TEST_PAYLOAD_VERSION = "{\"version\":{\"number\":\"5.5.0\"}}\n";
	private static final String JSON_TEST_PAYLOAD_EMPTY = "{}\n";
	private static final int BULK_BATCH_SIZE = 100;
	private static final String JSON_TEST_PAYLOAD_LARGE_BULK = produceLargeBukJSONContent();

	private static final Gson gson = new Gson();

	@Test
	public void testEmptyJSON() {
		final List<JsonObject> list = singletonList( buildEmptyJSON() );
		verifyProducedContent( list );
		verifySha256Signature( list );
		verifyOutput( JSON_TEST_PAYLOAD_EMPTY, list );
	}

	@Test
	public void testSinglePropertyJSON() {
		final List<JsonObject> list = singletonList( buildVersionJSON() );
		verifyProducedContent( list );
		verifySha256Signature( list );
		verifyOutput( JSON_TEST_PAYLOAD_VERSION, list );
	}

	@Test
	public void testTripleBulkJSON() {
		final List<JsonObject> list = new ArrayList<>( 3 );
		list.add( buildEmptyJSON() );
		list.add( buildVersionJSON() );
		list.add( buildEmptyJSON() );
		verifyProducedContent( list );
		verifySha256Signature( list );
		verifyOutput(
				JSON_TEST_PAYLOAD_EMPTY +
				JSON_TEST_PAYLOAD_VERSION +
				JSON_TEST_PAYLOAD_EMPTY,
				list );
	}

	@Test
	public void testHugeBulkJSON() {
		final List<JsonObject> list = produceLargeBulkJSON();
		verifyProducedContent( list );
		verifySha256Signature( list );
		verifyOutput( JSON_TEST_PAYLOAD_LARGE_BULK, list );
	}

	@Test
	public void testContentIsRepeatable() {
		final List<JsonObject> list = new ArrayList<>( 3 );
		list.add( buildEmptyJSON() );
		list.add( buildVersionJSON() );
		list.add( buildEmptyJSON() );
		try ( GsonHttpEntity entity = new GsonHttpEntity( gson, list ) ) {
			final byte[] productionOne = produceContentWithCustomEncoder( entity );
			final byte[] productionTwo = produceContentWithCustomEncoder( entity );
			assertArrayEquals( productionOne, productionTwo );
		}
		catch (IOException e) {
			throw new RuntimeException( "We're mocking IO operations, this should not happen?", e );
		}
	}

	@Test
	public void testDigestToTriggerLengthComputation() {
		final List<JsonObject> list = produceLargeBulkJSON();
		try ( GsonHttpEntity entity = new GsonHttpEntity( gson, list ) ) {
			assertEquals( -1l, entity.getContentLength() );
		}
		catch (IOException e) {
			throw new RuntimeException( "We're mocking IO operations, this should not happen?", e );
		}
		//Need to discard the entity and get a new one, as the getContentLenght()
		//invocation will have frozen the value: we can't report inconsistent values
		//to the Apache HTTP client or it gets confused.
		try ( GsonHttpEntity entity = new GsonHttpEntity( gson, list ) ) {
			final MessageDigest digest = getSha256Digest();
			entity.fillDigest( digest );
			assertNotEquals( -1l, entity.getContentLength() );
			final byte[] content = produceContentWithCustomEncoder( entity );
			assertEquals( content.length, entity.getContentLength() );
		}
		catch (IOException e) {
			throw new RuntimeException( "We're mocking IO operations, this should not happen?", e );
		}
	}

	@Test
	public void testShortMessageIsNotChunked() {
		final List<JsonObject> list = new ArrayList<>( 3 );
		list.add( buildEmptyJSON() );
		list.add( buildVersionJSON() );
		list.add( buildEmptyJSON() );
		final byte[] traditionalEncoding = traditionalEncoding( list );
		try ( GsonHttpEntity entity = new GsonHttpEntity( gson, list ) ) {
			assertEquals( traditionalEncoding.length, entity.getContentLength() );
		}
		catch (IOException e) {
			throw new RuntimeException( "We're mocking IO operations, this should not happen?", e );
		}
	}

	private void verifyOutput(final String expected, final List<JsonObject> list) {
		assertEquals( expected, encodeToString( list ) );
	}

	private void verifySha256Signature(final List<JsonObject> jsonObjects) {
		final String optimisedEncoding = optimisedSha256( jsonObjects );
		final String traditionalEncoding = traditionalSha256( jsonObjects );
		assertEquals( "SHA-256 signatures not matching", traditionalEncoding, optimisedEncoding );
	}

	private String optimisedSha256(final List<JsonObject> bodyParts) {
		notEmpty( bodyParts );
		try ( GsonHttpEntity entity = new GsonHttpEntity( gson, bodyParts ) ) {
			final MessageDigest digest = getSha256Digest();
			entity.fillDigest( digest );
			return encodeHexString( digest.digest() );
		}
		catch (IOException e) {
			throw new RuntimeException( "We're mocking IO operations, this should not happen?", e );
		}
	}

	private String traditionalSha256(final List<JsonObject> jsonObjects) {
		return sha256Hex( traditionalEncoding( jsonObjects ) );
	}

	private void verifyProducedContent(final List<JsonObject> jsonObjects) {
		byte[] expected = traditionalEncoding( jsonObjects );
		byte[] optimised = optimisedEncoding( jsonObjects );
		if ( Arrays.equals( expected, optimised ) == false ) {
			CharBuffer decodedExpected = StandardCharsets.UTF_8.decode( ByteBuffer.wrap( expected ) );
			CharBuffer decodedOptimised = StandardCharsets.UTF_8.decode( ByteBuffer.wrap( optimised ) );
			System.out.println( "Rendered :\n" + decodedOptimised );
			System.out.println( "Should be:\n" + decodedExpected );
		}
		assertArrayEquals( expected, optimised );
	}

	private byte[] optimisedEncoding(List<JsonObject> bodyParts) {
		notEmpty( bodyParts );
		try ( GsonHttpEntity entity = new GsonHttpEntity( gson, bodyParts ) ) {
			byte[] firstRun = produceContentWithCustomEncoder( entity );
			entity.close();
			byte[] secondRun = produceContentWithCustomEncoder( entity );
			Assert.assertArrayEquals( "Being repeatable, we expect it to be able to reproduce all content even after being closed", firstRun, secondRun );
			return secondRun;
		}
		catch (IOException e) {
			throw new RuntimeException( "We're mocking IO operations, this should not happen?", e );
		}
	}

	private byte[] produceContentWithCustomEncoder(GsonHttpEntity entity) throws IOException {
		IOControl fakeIO = new FakeIOControl();
		HeapContentEncoder sink = new HeapContentEncoder();
		int loopCounter = 0;
		while ( sink.isCompleted() == false ) {
			entity.produceContent( sink, fakeIO );
			/*
			 * For testing, be really aggressive on the need to
			 * manage small write windows the right way.
			 *
			 * Also, using small write windows helped to reproduce HSEARCH-2854
			 * almost all the time (we only need the flow control to push
			 * back at least once).
			 */
			sink.setNextAcceptedBytesSize( loopCounter++ % 3 );
		}
		return sink.flipAndRead();
	}

	private void notEmpty(final List<JsonObject> bodyParts) {
		assertFalse( "Pointless to test this, we don't use this strategy for empty blocks", bodyParts.isEmpty() );
	}

	/**
	 * This is the simplest encoding strategy; we don't use this as
	 * it would require to allocate significantly larger intermediate
	 * buffers. See also HSEARCH-2818.
	 */
	byte[] traditionalEncoding(final List<JsonObject> bodyParts) {
		return encodeToString( bodyParts ).getBytes( StandardCharsets.UTF_8 );
	}

	private String encodeToString(final List<JsonObject> bodyParts) {
		notEmpty( bodyParts );
		final StringBuilder builder = new StringBuilder();
		for ( JsonObject bodyPart : bodyParts ) {
			gson.toJson( bodyPart, builder );
			builder.append( '\n' );
		}
		return builder.toString();
	}

	private static List<JsonObject> produceLargeBulkJSON() {
		ArrayList<JsonObject> list = new ArrayList<>( BULK_BATCH_SIZE );
		for ( int i = 0; i < 100; i++ ) {
			list.add( buildVersionJSON() );
		}
		return list;
	}

	private static JsonObject buildEmptyJSON() {
		return JsonBuilder.object()
				.build();
	}

	private static JsonObject buildVersionJSON() {
		return JsonBuilder.object()
				.add( "version", JsonBuilder.object()
						.addProperty( "number", "5.5.0" )
				).build();
	}

	private static String produceLargeBukJSONContent() {
		final StringBuilder content = new StringBuilder( BULK_BATCH_SIZE * JSON_TEST_PAYLOAD_VERSION.length() );
		for ( int i = 0; i < BULK_BATCH_SIZE; i++ ) {
			content.append( JSON_TEST_PAYLOAD_VERSION );
		}
		return content.toString();
	}

	private static final class HeapContentEncoder implements ContentEncoder {

		private boolean contentComplete = false;
		private int nextWriteAcceptLimit = 0;
		private ByteBuffer buf = ByteBuffer.allocate( MAX_TESTING_BUFFER_BYTES );
		private boolean lastWriteWasZeroLength = false;
		private boolean closed = false;

		@Override
		public int write(final ByteBuffer byteBuffer) throws IOException {
			assertFalse( closed );

			lastWriteWasZeroLength = !byteBuffer.hasRemaining();

			int toRead = Math.min( byteBuffer.remaining(), nextWriteAcceptLimit );
			byte[] currentRead = new byte[toRead];
			byteBuffer.get( currentRead );
			buf.put( currentRead );
//			debugReadSoFar( currentRead );
			return toRead;
		}

		private void debugReadSoFar(byte[] currentRead) {
			if ( currentRead.length > 0 ) {
				CharBuffer decodedExpected = StandardCharsets.UTF_8.decode( ByteBuffer.wrap( currentRead ) );
				String easy = decodedExpected.toString();
				System.out.println( easy );
				return;
			}
		}

		@Override
		public void complete() throws IOException {
			assertFalse( "A final zero-length write was detected - this should never happen. See HSEARCH-2854.",
					lastWriteWasZeroLength );
			assertFalse( closed );
			assertFalse( "Can't mark it 'complete' multiple times", contentComplete );
			contentComplete = true;
		}

		@Override
		public boolean isCompleted() {
			return contentComplete;
		}

		public byte[] flipAndRead() {
			assertFalse( "can read the buffer only once", closed );
			closed = true;
			buf.flip();
			byte[] currentRead = new byte[buf.remaining()];
			buf.get( currentRead );
			return currentRead;
		}

		public void setNextAcceptedBytesSize(int size) {
			this.nextWriteAcceptLimit = size;
		}

	}

	private static final class FakeIOControl implements IOControl {
		@Override
		public void requestInput() {
			fail( "Should not invoke this" );
		}

		@Override
		public void suspendInput() {
			fail( "Should not invoke this" );
		}

		@Override
		public void requestOutput() {
			fail( "Should not invoke this" );
		}

		@Override
		public void suspendOutput() {
			fail( "Should not invoke this" );
		}

		@Override
		public void shutdown() throws IOException {
			fail( "Should not invoke this" );
		}
	}

}
