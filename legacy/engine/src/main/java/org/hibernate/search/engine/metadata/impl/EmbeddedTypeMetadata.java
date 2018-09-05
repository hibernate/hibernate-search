/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.engine.metadata.impl;

import java.util.Map;

import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.util.impl.ReflectionHelper;

/**
 * Class containing all the meta data extracted for a single type.
 *
 * @author Hardy Ferentschik
 */
public class EmbeddedTypeMetadata extends TypeMetadata {
	private final BackReference<PropertyMetadata> sourceProperty;

	private final String embeddedFieldPrefix;
	private final Container embeddedContainer;

	private final String embeddedNullFieldName;
	private final String embeddedNullToken;
	private final FieldBridge embeddedNullFieldBridge;

	private EmbeddedTypeMetadata(Builder builder) {
		super( builder );
		this.sourceProperty = builder.sourceProperty;

		this.embeddedFieldPrefix = builder.embeddedFieldPrefix;
		this.embeddedContainer = builder.embeddedContainer;

		this.embeddedNullFieldName = builder.embeddedNullFieldName;
		this.embeddedNullToken = builder.embeddedNullToken;
		this.embeddedNullFieldBridge = builder.embeddedNullFieldBridge;
	}

	/**
	 * @return The property from which the value for this embedded is extracted.
	 */
	public PropertyMetadata getSourceProperty() {
		return sourceProperty.get();
	}

	/**
	 * @return The name of the Java property holding this embedded.
	 */
	public String getEmbeddedPropertyName() {
		return getSourceProperty().getPropertyAccessorName();
	}

	/**
	 * @return The field prefix, i.e. the string that should be concatenated to
	 * prefixes of containing embeddeds and to the local field name to give the
	 * full index field name.
	 */
	public String getEmbeddedFieldPrefix() {
		return embeddedFieldPrefix;
	}

	public XMember getEmbeddedGetter() {
		return getSourceProperty().getPropertyAccessor();
	}

	public Container getEmbeddedContainer() {
		return embeddedContainer;
	}

	public String getEmbeddedNullFieldName() {
		return embeddedNullFieldName;
	}

	public String getEmbeddedNullToken() {
		return embeddedNullToken;
	}

	public FieldBridge getEmbeddedNullFieldBridge() {
		return embeddedNullFieldBridge;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "EmbeddedTypeMetadata{" );
		sb.append( "embeddedPropertyName='" ).append( getEmbeddedPropertyName() ).append( '\'' );
		sb.append( ", sourceProperty='" ).append( sourceProperty ).append( '\'' );
		sb.append( ", embeddedFieldPrefix='" ).append( embeddedFieldPrefix ).append( '\'' );
		sb.append( ", embeddedGetter=" ).append( getEmbeddedGetter() );
		sb.append( ", embeddedContainer=" ).append( embeddedContainer );
		sb.append( ", embeddedNullFieldPath='" ).append( embeddedNullFieldName ).append( '\'' );
		sb.append( ", embeddedNullToken='" ).append( embeddedNullToken ).append( '\'' );
		sb.append( ", embeddedNullFieldBridge=" ).append( embeddedNullFieldBridge );
		sb.append( '}' );
		return sb.toString();
	}

	public static class Builder extends TypeMetadata.Builder {
		private final BackReference<PropertyMetadata> sourceProperty;

		private final String embeddedFieldPrefix;
		private final Container embeddedContainer;

		private String embeddedNullToken;
		private String embeddedNullFieldName;
		private FieldBridge embeddedNullFieldBridge;

		public Builder(TypeMetadata.Builder parentTypeBuilder,
				IndexedTypeIdentifier indexedType, BackReference<PropertyMetadata> sourceProperty, XMember embeddedGetter,
				String embeddedFieldPrefix) {
			super( indexedType, parentTypeBuilder );
			this.sourceProperty = sourceProperty;
			ReflectionHelper.setAccessible( embeddedGetter );
			this.embeddedFieldPrefix = embeddedFieldPrefix;
			this.embeddedContainer = determineContainerType( embeddedGetter );
		}

		public Builder indexNullToken(String embeddedNullToken, String embeddedNullFieldName, FieldBridge embeddedNullFieldBridge) {
			this.embeddedNullToken = embeddedNullToken;
			this.embeddedNullFieldName = embeddedNullFieldName;
			this.embeddedNullFieldBridge = embeddedNullFieldBridge;
			return this;
		}

		public Container getEmbeddedContainerType() {
			return embeddedContainer;
		}

		@Override
		public EmbeddedTypeMetadata build() {
			EmbeddedTypeMetadata result = new EmbeddedTypeMetadata( this );
			resultReference.initialize( result );
			return result;
		}

		private Container determineContainerType(XMember member) {
			/**
			 * We will only index the "expected" type but that's OK, HQL cannot do down-casting either
			 */
			if ( member.isArray() ) {
				return Container.ARRAY;
			}
			else if ( member.isCollection() ) {
				if ( Map.class.equals( member.getCollectionClass() ) ) {
					//hum subclasses etc etc??
					return Container.MAP;
				}
				else {
					return Container.COLLECTION;
				}
			}
			else {
				return Container.OBJECT;
			}
		}
	}

	public enum Container {
		OBJECT,
		COLLECTION,
		MAP,
		ARRAY
	}
}


