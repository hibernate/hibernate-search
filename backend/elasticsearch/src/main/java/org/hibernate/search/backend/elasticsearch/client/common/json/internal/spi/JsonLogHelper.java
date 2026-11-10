package org.hibernate.search.backend.elasticsearch.client.common.json.internal.spi;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonProviderHelper;
import org.hibernate.search.util.common.annotation.Incubating;

import tools.jackson.databind.node.ObjectNode;

/**
 * Helpers allowing to log JSON.
 *
 */
@Incubating
public final class JsonLogHelper {


	public static JsonLogHelper get() {
		return JsonProviderHelper.create( true ).getLogHelper();
	}

	public static JsonLogHelper create(JsonCodecService codec, boolean prettyPrinting) {
		return new JsonLogHelper( codec, prettyPrinting );
	}

	private final JsonCodecService json;

	private final boolean prettyPrinting;

	private JsonLogHelper(JsonCodecService json, boolean prettyPrinting) {
		this.json = json;
		this.prettyPrinting = prettyPrinting;
	}

	public String toString(ObjectNode object) {
		StringBuilder stringBuilder = new StringBuilder();
		append( stringBuilder, object );
		return stringBuilder.toString();
	}

	public void append(StringBuilder sb, ObjectNode object) {
		beforeValue( sb );
		doAppend( sb, object );
		afterValue( sb );
	}

	public String toString(Iterable<ObjectNode> objects) {
		StringBuilder sb = new StringBuilder( 180 );
		append( sb, objects );
		return sb.toString();
	}

	public void append(StringBuilder sb, Iterable<ObjectNode> objects) {
		boolean first = true;
		beforeValue( sb );
		for ( ObjectNode object : objects ) {
			if ( first ) {
				first = false;
			}
			else if ( prettyPrinting ) {
				sb.append( "\n" );
			}
			else {
				sb.append( "\\n" );
			}
			doAppend( sb, object );
		}
		afterValue( sb );
	}

	private void beforeValue(StringBuilder sb) {
		if ( prettyPrinting ) {
			sb.append( "\n" );
		}
	}

	private void doAppend(StringBuilder sb, ObjectNode object) {
		try {
			sb.append( json.write( object ) );
		}
		catch (RuntimeException e) {
			StringWriter writer = new StringWriter();
			e.printStackTrace( new PrintWriter( writer ) );
			sb.append( writer.toString() );
		}
	}

	private void afterValue(StringBuilder sb) {
		if ( prettyPrinting ) {
			sb.append( "\n" );
		}
	}
}
