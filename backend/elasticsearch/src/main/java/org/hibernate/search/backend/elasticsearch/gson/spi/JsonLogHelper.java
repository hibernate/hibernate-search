/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.gson.spi;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Helpers allowing to log JSON.
 *
 */
public final class JsonLogHelper {

	private static final JsonLogHelper INSTANCE = create( new GsonBuilder(), true );

	public static JsonLogHelper get() {
		return INSTANCE;
	}

	public static JsonLogHelper create(GsonBuilder gsonBuilder, boolean prettyPrinting) {
		if ( prettyPrinting ) {
			gsonBuilder.setPrettyPrinting();
		}
		return new JsonLogHelper( gsonBuilder.create(), prettyPrinting );
	}

	private final Gson gson;

	private final boolean prettyPrinting;

	private JsonLogHelper(Gson gson, boolean prettyPrinting) {
		this.gson = gson;
		this.prettyPrinting = prettyPrinting;
	}

	public String toString(JsonObject object) {
		StringBuilder stringBuilder = new StringBuilder();
		append( stringBuilder, object );
		return stringBuilder.toString();
	}

	public void append(StringBuilder sb, JsonObject object) {
		beforeValue( sb );
		doAppend( sb, object );
		afterValue( sb );
	}

	public String toString(Iterable<JsonObject> objects) {
		StringBuilder sb = new StringBuilder( 180 );
		append( sb, objects );
		return sb.toString();
	}

	public void append(StringBuilder sb, Iterable<JsonObject> objects) {
		boolean first = true;
		beforeValue( sb );
		for ( JsonObject object : objects ) {
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

	private void doAppend(StringBuilder sb, JsonObject object) {
		try {
			gson.toJson( object, sb );
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
