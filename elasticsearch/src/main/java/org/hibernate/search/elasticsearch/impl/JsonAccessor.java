/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import org.hibernate.search.exception.AssertionFailure;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * A class that abstracts the ways of accessing values in a JSON tree.
 *
 * @author Yoann Rodiere
 */
abstract class JsonAccessor {

	/**
	 * Get the current value of the element this accessor points to for the given {@code root}.
	 *
	 * @param root The root to be accessed.
	 * @return The current value pointed to by this accessor on the {@code root},
	 * or null if it doesn't exist.
	 */
	public abstract JsonElement get(JsonObject root);

	/**
	 * Set the given value on the element this accessor points to for the given {@code root}.
	 *
	 * @param root The root to be accessed.
	 * @param newValue The value to set.
	 * @throws AssertionFailure If an element in the path has unexpected type, preventing
	 * access to the element this accessor points to.
	 */
	public abstract void set(JsonObject root, JsonElement newValue);

	/**
	 * Add the given primitive value to the element this accessor points to for the
	 * given {@code root}.
	 *
	 * <p>This method differs from {@link #set(JsonObject, JsonElement)}:
	 * <ul>
	 * <li>If there is currently no value, the given value is simply
	 * {@link #set(JsonObject, JsonElement) set}
	 * <li>If there is an array value, the given value is added to this array
	 * <li>Otherwise, the current value is replaced by an array containing the current
	 * value followed by the given value.
	 * </ul>
	 *
	 * @param root The root to be accessed.
	 * @param newValue The value to add.
	 * @throws AssertionFailure If an element in the path has unexpected type, preventing
	 * access to the element this accessor points to.
	 */
	public abstract void add(JsonObject root, JsonPrimitive newValue);

	/**
	 * Get the current value of the lement this accessor points to for the given {@code root},
	 * creating it and setting it if it hasn't been set yet.
	 *
	 * @param root The root to be accessed.
	 * @param type The expected {@link JsonElementType}.
	 * @return The current value pointed to by this accessor on the {@code root}, always non-null
	 * and typed according to {@code type}.
	 * @throws AssertionFailure if the element already exists and is not of type {@code type}, or
	 * if an element in the path has unexpected type, preventing access to the element this accessor
	 * points to.
	 */
	public abstract <T extends JsonElement> T getOrCreate(JsonObject root, JsonElementType<T> type);

	public static JsonAccessor root() {
		return ROOT;
	}

	private static final JsonAccessor ROOT = new JsonAccessor() {
		@Override
		public JsonElement get(JsonObject root) {
			if ( root == null ) {
				throw new AssertionFailure( "A null root was encountered" );
			}
			return root;
		}

		@Override
		public void set(JsonObject root, JsonElement value) {
			throw new UnsupportedOperationException( "Cannot set the root element" );
		}

		@Override
		public <T extends JsonElement> T getOrCreate(JsonObject root, JsonElementType<T> type) {
			return type.cast( get( root ) );
		}

		@Override
		public void add(JsonObject root, JsonPrimitive value) {
			throw new UnsupportedOperationException( "Cannot add a value to the root element" );
		}

		@Override
		public String toString() {
			return "root";
		}
	};

	private abstract static class NonRootAccessor<P extends JsonElement> extends JsonAccessor {
		private final JsonAccessor parentAccessor;

		public NonRootAccessor(JsonAccessor parentAccessor) {
			super();
			this.parentAccessor = parentAccessor;
		}

		protected JsonAccessor getParentAccessor() {
			return parentAccessor;
		}

		protected abstract JsonElementType<P> getExpectedParentType();

		@Override
		public JsonElement get(JsonObject root) {
			P parent = getExpectedParentType().cast( parentAccessor.get( root ) );
			if ( parent == null ) {
				return null;
			}
			return doGet( parent );
		}

		protected abstract JsonElement doGet(P parent);

		@Override
		public void set(JsonObject root, JsonElement newValue) {
			P parent = parentAccessor.getOrCreate( root, getExpectedParentType() );
			doSet( parent, newValue );
		}

		protected abstract void doSet(P parent, JsonElement newValue);

		@Override
		public <T extends JsonElement> T getOrCreate(JsonObject root, JsonElementType<T> type) {
			P parent = parentAccessor.getOrCreate( root, getExpectedParentType() );
			JsonElement currentValue = doGet( parent );
			if ( currentValue == null || currentValue.isJsonNull() ) {
				T result = type.newInstance();
				doSet( parent, result );
				return result;
			}
			else if ( !type.isInstance( currentValue ) ) {
				throw new AssertionFailure(
						"Unexpected type at '" + this + "'. Expected '"
						+ type + "', got '" + currentValue + "'"
						);
			}
			else {
				return type.cast( currentValue );
			}
		}

		@Override
		public void add(JsonObject root, JsonPrimitive newValue) {
			P parent = parentAccessor.getOrCreate( root, getExpectedParentType() );
			JsonElement currentValue = doGet( parent );
			if ( currentValue == null ) { // Do not overwrite JsonNull, because it might be there on purpose
				doSet( parent, newValue );
			}
			else if ( currentValue.isJsonArray() ) {
				currentValue.getAsJsonArray().add( newValue );
			}
			else {
				doSet( parent, JsonBuilder.array().add( currentValue ).add( newValue ).build() );
			}
		}
	}

	public static JsonAccessor objectProperty(JsonAccessor parentAccessor, String propertyName) {
		return new ObjectPropertyJsonAccessor( parentAccessor, propertyName );
	}

	private static class ObjectPropertyJsonAccessor extends NonRootAccessor<JsonObject> {
		private final String propertyName;

		public ObjectPropertyJsonAccessor(JsonAccessor parentAccessor, String propertyName) {
			super( parentAccessor );
			this.propertyName = propertyName;
		}

		@Override
		protected JsonElementType<JsonObject> getExpectedParentType() {
			return JsonElementType.OBJECT;
		}

		@Override
		protected JsonElement doGet(JsonObject parent) {
			return parent.get( propertyName );
		}

		@Override
		protected void doSet(JsonObject parent, JsonElement newValue) {
			parent.add( propertyName, newValue );
		}

		@Override
		public String toString() {
			return new StringBuilder()
					.append( getParentAccessor() )
					.append( "." ).append( propertyName )
					.toString();
		}
	}

	public static JsonAccessor arrayElement(JsonAccessor parentAccessor, int index) {
		return new ArrayElementJsonAccessor( parentAccessor, index );
	}

	private static class ArrayElementJsonAccessor extends NonRootAccessor<JsonArray> {
		private final int index;

		public ArrayElementJsonAccessor(JsonAccessor parentAccessor, int index) {
			super( parentAccessor );
			this.index = index;
		}

		@Override
		protected JsonElementType<JsonArray> getExpectedParentType() {
			return JsonElementType.ARRAY;
		}

		@Override
		protected JsonElement doGet(JsonArray parent) {
			if ( parent != null && index < parent.size() ) {
				return parent.get( index );
			}
			else {
				return null;
			}
		}

		@Override
		protected void doSet(JsonArray parent, JsonElement newValue) {
			fillTo( parent, index );
			parent.set( index, newValue );
		}

		private static void fillTo(JsonArray array, int index) {
			for ( int i = array.size(); i <= index; ++i ) {
				array.add( JsonNull.INSTANCE );
			}
		}

		@Override
		public String toString() {
			return new StringBuilder()
					.append( getParentAccessor() )
					.append( "[" ).append( index ).append( "]" )
					.toString();
		}
	}

}
