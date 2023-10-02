/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.gson.impl;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.gson.impl.AbstractExtraPropertiesJsonAdapter.ExtraPropertyAdapter;
import org.hibernate.search.backend.elasticsearch.gson.impl.AbstractExtraPropertiesJsonAdapter.FieldAdapter;
import org.hibernate.search.util.common.AssertionFailure;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public abstract class AbstractConfiguredExtraPropertiesJsonAdapterFactory implements TypeAdapterFactory {

	@Override
	public final <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
		Builder<T> builder = new Builder<>( gson, type );
		addFields( builder );
		return builder.build();
	}

	protected abstract <T> void addFields(Builder<T> builder);

	protected static final class Builder<T> {

		private final Gson gson;
		private final TypeToken<T> type;
		private final Map<String, FieldAdapter<? super T>> fieldAdapters = new LinkedHashMap<>();

		private Builder(Gson gson, TypeToken<T> type) {
			super();
			this.gson = gson;
			this.type = type;
		}

		public <F> Builder<T> add(String fieldName, Class<F> fieldType) {
			return add( fieldName, TypeToken.get( fieldType ) );
		}

		public <F> Builder<T> add(String fieldName, TypeToken<F> fieldType) {
			TypeAdapter<F> adapter = gson.getAdapter( fieldType );
			return add( fieldName, adapter );
		}

		public <F> Builder<T> add(String fieldName, TypeAdapter<F> adapter) {
			Field field = getField( type, fieldName );

			boolean first = true;
			for ( String name : getFieldNames( field ) ) {
				boolean serialized = first;
				fieldAdapters.put( name, new ReflectiveFieldAdapter<>( field, adapter, serialized ) );
				first = false;
			}

			return this;
		}

		private TypeAdapter<T> build() {
			return new Adapter<>( fieldAdapters, getExtraPropertyAdapter( gson, type ), getConstructor( type ) );
		}
	}

	private static Field getField(TypeToken<?> type, String fieldName) {
		Class<?> rawType = type.getRawType();
		while ( rawType != null ) {
			try {
				Field field = rawType.getDeclaredField( fieldName );
				field.setAccessible( true );
				return field;
			}
			catch (NoSuchFieldException ignored) {
			}
			rawType = rawType.getSuperclass();
		}
		throw new AssertionFailure( "Missing or inaccessible field " + fieldName + " on type " + type );
	}

	private static <T> ExtraPropertyAdapter<T> getExtraPropertyAdapter(Gson gson, TypeToken<T> type) {
		Class<?> rawType = type.getRawType();
		while ( rawType != null ) {
			for ( Field field : rawType.getDeclaredFields() ) {
				SerializeExtraProperties annotation = field.getAnnotation( SerializeExtraProperties.class );
				if ( annotation != null ) {
					field.setAccessible( true );
					return new ReflectiveExtraPropertyAdapter<>( field, gson );
				}
			}
			rawType = rawType.getSuperclass();
		}
		throw new AssertionFailure(
				"Missing or inaccessible field annotated with " + SerializeExtraProperties.class + " on type " + type );
	}

	@SuppressWarnings("unchecked")
	private static <T> Constructor<T> getConstructor(TypeToken<T> type) {
		try {
			return (Constructor<T>) type.getRawType().getConstructor();
		}
		catch (NoSuchMethodException | SecurityException e) {
			throw new AssertionFailure( "Missing or inaccessible no-arg constructor on type " + type );
		}
	}

	private static List<String> getFieldNames(Field f) {
		SerializedName serializedName = f.getAnnotation( SerializedName.class );
		List<String> fieldNames = new LinkedList<>();
		if ( serializedName == null ) {
			fieldNames.add( f.getName() );
		}
		else {
			fieldNames.add( serializedName.value() );
			Collections.addAll( fieldNames, serializedName.alternate() );
		}
		return fieldNames;
	}

	private static class ReflectiveExtraPropertyAdapter<T> implements ExtraPropertyAdapter<T> {

		private final Field extraPropertiesField;
		private final TypeAdapter<JsonElement> propertyValueAdapter;

		public ReflectiveExtraPropertyAdapter(Field extraPropertiesField, Gson gson) {
			super();
			this.extraPropertiesField = extraPropertiesField;
			this.propertyValueAdapter = gson.getAdapter( JsonElement.class );
		}

		@Override
		public void readOne(JsonReader in, String name, T instance) throws IOException {
			JsonElement propertyValue = propertyValueAdapter.read( in );
			Map<String, JsonElement> extraProperties = getExtraProperties( instance );
			if ( extraProperties == null ) {
				extraProperties = new LinkedHashMap<>();
				setExtraProperties( instance, extraProperties );
			}
			extraProperties.put( name, propertyValue );
		}

		@Override
		public void writeAll(JsonWriter out, T instance) throws IOException {
			Map<String, JsonElement> extraProperties = getExtraProperties( instance );
			if ( extraProperties == null ) {
				return;
			}
			for ( Map.Entry<String, JsonElement> entry : extraProperties.entrySet() ) {
				out.name( entry.getKey() );
				propertyValueAdapter.write( out, entry.getValue() );
			}
		}

		private void setExtraProperties(T instance, Map<String, JsonElement> extraProperties) {
			try {
				extraPropertiesField.set( instance, extraProperties );
			}
			catch (IllegalArgumentException e) {
				throw new AssertionFailure( "Field " + extraPropertiesField + " annotated with "
						+ SerializeExtraProperties.class + " has the wrong type on " + instance.getClass() );
			}
			catch (IllegalAccessException e) {
				throw new AssertionFailure( "Field " + extraPropertiesField + " annotated with "
						+ SerializeExtraProperties.class + " is inaccessible on " + instance.getClass() );
			}
		}

		@SuppressWarnings("unchecked")
		private Map<String, JsonElement> getExtraProperties(T instance) {
			try {
				return (Map<String, JsonElement>) extraPropertiesField.get( instance );
			}
			catch (IllegalAccessException e) {
				throw new AssertionFailure( "Field " + extraPropertiesField + " annotated with "
						+ SerializeExtraProperties.class + " is inaccessible on " + instance.getClass() );
			}
		}
	}

	private static class ReflectiveFieldAdapter<T, F> implements FieldAdapter<T> {

		private final Field field;
		private final TypeAdapter<F> typeAdapter;
		private final boolean serialized;

		public ReflectiveFieldAdapter(Field field, TypeAdapter<F> typeAdapter, boolean serialized) {
			super();
			this.field = field;
			this.typeAdapter = typeAdapter;
			this.serialized = serialized;
		}

		@Override
		public void read(JsonReader in, T instance) throws IOException {
			try {
				field.set( instance, typeAdapter.read( in ) );
			}
			catch (IllegalAccessException e) {
				throw new AssertionFailure( "Field " + field + " is not accessible.", e );
			}
		}

		@Override
		public boolean serialized() {
			return serialized;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void write(JsonWriter out, T instance) throws IOException {
			if ( !serialized ) {
				throw new AssertionFailure( "The property with this name should not be serialized" );
			}
			try {
				typeAdapter.write( out, (F) field.get( instance ) );
			}
			catch (IllegalAccessException e) {
				throw new AssertionFailure( "Field " + field + " is not accessible.", e );
			}
		}
	}

	private static class Adapter<T> extends AbstractExtraPropertiesJsonAdapter<T> {

		private final Constructor<T> constructor;

		public Adapter(Map<String, ? extends FieldAdapter<? super T>> fieldAdapters,
				ExtraPropertyAdapter<? super T> extraPropertyAdapter,
				Constructor<T> constructor) {
			super( fieldAdapters, extraPropertyAdapter );
			this.constructor = constructor;
		}

		@Override
		protected T createInstance() {
			try {
				return constructor.newInstance();
			}
			catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
				throw new AssertionFailure( "Constructor " + constructor + " is not accessible.", e );
			}
		}
	}

}
