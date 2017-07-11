/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.gson.impl;

import java.util.Optional;
import java.util.function.Supplier;

import org.hibernate.search.exception.AssertionFailure;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
class RootJsonAccessor implements JsonObjectAccessor, JsonCompositeAccessor<JsonObject> {

	static final JsonObjectAccessor INSTANCE = new RootJsonAccessor();

	private RootJsonAccessor() {
		// Private, use INSTANCE instead.
	}

	@Override
	public Optional<JsonObject> get(JsonObject root) {
		return Optional.ofNullable( root );
	}

	@Override
	public void set(JsonObject root, JsonObject value) {
		throw new UnsupportedOperationException( "Cannot set the root element" );
	}

	@Override
	public void add(JsonObject root, JsonObject value) {
		throw new UnsupportedOperationException( "Cannot add a value to the root element" );
	}

	@Override
	public JsonObject getOrCreate(JsonObject root, Supplier<? extends JsonObject> newValueSupplier) throws UnexpectedJsonElementTypeException {
		return requireRoot( root );
	}

	@Override
	public JsonObject getOrCreate(JsonObject root) throws UnexpectedJsonElementTypeException {
		return requireRoot( root );
	}

	@Override
	public String toString() {
		return "root";
	}

	@Override
	public String getStaticAbsolutePath() {
		return null;
	}

	@Override
	public UnknownTypeJsonAccessor property(String propertyName) {
		return new ObjectPropertyJsonAccessor( this, propertyName );
	}

	private JsonObject requireRoot(JsonObject root) {
		if ( root == null ) {
			throw new AssertionFailure( "A null root was encountered" );
		}
		else {
			return root;
		}
	}
}
