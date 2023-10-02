/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common;

import static org.assertj.core.api.Assertions.entry;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.impl.StreamHelper;

/**
 * Utils allowing to normalize data coming from different backends.
 * <p>
 * Mainly useful in search result assertions to compare different types of {@link DocumentReference},
 * but also in {@link MockUtils#projectionMatcher(Object...) mock argument matchers},
 * or in tests inspecting BigDecimals returned from the backend
 * (since the BigDecimals can have trailing zeros that do not matter).
 */
public final class NormalizationUtils {

	private NormalizationUtils() {
	}

	public static DocumentReference reference(String typeName, String id) {
		return new NormalizedDocumentReference( typeName, id );
	}

	// The casts will work fine as long as we don't reference subclasses of the normalized classes in our tests
	@SuppressWarnings("unchecked")
	public static <T> T normalize(T object) {
		if ( object == null ) {
			return null;
		}
		else if ( object instanceof DocumentReference ) {
			return (T) normalize( (DocumentReference) object );
		}
		else if ( object instanceof BigDecimal ) {
			return (T) normalize( (BigDecimal) object );
		}
		else if ( object instanceof Range ) {
			return (T) normalize( (Range<?>) object );
		}
		else if ( object instanceof Map.Entry ) {
			return (T) normalize( (Map.Entry<?, ?>) object );
		}
		else if ( object instanceof List ) {
			return (T) normalize( (List<?>) object );
		}
		else if ( object instanceof Map ) {
			return (T) normalize( (Map<?, ?>) object );
		}
		else if ( object.getClass().isArray() ) {
			// Primitive arrays not supported, since we don't need them yet.
			Object[] array = (Object[]) object;
			Object[] copy = Arrays.copyOf( array, array.length );
			for ( int i = 0; i < copy.length; i++ ) {
				copy[i] = normalize( copy[i] );
			}
			return (T) copy;
		}
		else if ( object instanceof Normalizable ) {
			return (T) ( (Normalizable<?>) object ).normalize();
		}
		else {
			return object;
		}
	}

	public static DocumentReference normalize(DocumentReference original) {
		return original == null ? null : reference( original.typeName(), original.id() );
	}

	public static BigDecimal normalize(BigDecimal original) {
		return original == null ? null : original.stripTrailingZeros();
	}

	public static <T> Range<T> normalize(Range<T> original) {
		return original == null ? null : original.map( NormalizationUtils::normalize );
	}

	public static <K, V> Map.Entry<K, V> normalize(Map.Entry<K, V> original) {
		return original == null
				? null
				: entry(
						normalize( original.getKey() ),
						normalize( original.getValue() )
				);
	}

	public static <T> List<T> normalize(List<T> original) {
		return original == null
				? null
				: original.stream()
						.map( NormalizationUtils::normalize )
						.collect( Collectors.toList() );
	}

	public static <K, V> Map<K, V> normalize(Map<K, V> original) {
		return original == null
				? null
				: original.entrySet().stream()
						.map( e -> entry(
								normalize( e.getKey() ),
								normalize( e.getValue() )
						) )
						.collect( StreamHelper.toMap( Map.Entry::getKey, Map.Entry::getValue, LinkedHashMap::new ) );
	}

	/**
	 * A pivot format for document references, allowing to compare multiple implementations of {@link DocumentReference}
	 * with each other.
	 */
	private static class NormalizedDocumentReference implements DocumentReference {
		private final String typeName;
		private final String id;

		private NormalizedDocumentReference(String typeName, String id) {
			this.typeName = typeName;
			this.id = id;
		}

		@Override
		public String typeName() {
			return typeName;
		}

		@Override
		public String id() {
			return id;
		}

		@Override
		public boolean equals(Object obj) {
			if ( obj == null || !NormalizedDocumentReference.class.equals( obj.getClass() ) ) {
				return false;
			}
			NormalizedDocumentReference other = (NormalizedDocumentReference) obj;
			return Objects.equals( typeName, other.typeName )
					&& Objects.equals( id, other.id );
		}

		@Override
		public int hashCode() {
			return Objects.hash( typeName, id );
		}

		@Override
		public String toString() {
			return "DocRef:" + typeName + "/" + id;
		}
	}

}
