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
import org.hibernate.search.util.impl.ReflectionHelper;
import org.hibernate.search.util.impl.ScopedAnalyzerReference;

/**
 * Class containing all the meta data extracted for a single type.
 *
 * @author Hardy Ferentschik
 */
public class EmbeddedTypeMetadata extends TypeMetadata {
	private final String embeddedFieldName;
	private final String embeddedFieldPrefix;
	private final XMember embeddedGetter;
	private final Container embeddedContainer;

	private final String embeddedNullFieldName;
	private final String embeddedNullToken;
	private final FieldBridge embeddedNullFieldBridge;

	private EmbeddedTypeMetadata(Builder builder) {
		super( builder );
		this.embeddedFieldName = builder.embeddedFieldName;
		this.embeddedFieldPrefix = builder.embeddedFieldPrefix;
		this.embeddedGetter = builder.embeddedGetter;
		this.embeddedContainer = builder.embeddedContainer;

		this.embeddedNullFieldName = builder.embeddedNullFieldName;
		this.embeddedNullToken = builder.embeddedNullToken;
		this.embeddedNullFieldBridge = builder.embeddedNullFieldBridge;
	}

	public String getEmbeddedFieldName() {
		return embeddedFieldName;
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
		return embeddedGetter;
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
		sb.append( "embeddedFieldName='" ).append( embeddedFieldName ).append( '\'' );
		sb.append( ", embeddedFieldPrefix='" ).append( embeddedFieldPrefix ).append( '\'' );
		sb.append( ", embeddedGetter=" ).append( embeddedGetter );
		sb.append( ", embeddedContainer=" ).append( embeddedContainer );
		sb.append( ", embeddedNullFieldPath='" ).append( embeddedNullFieldName ).append( '\'' );
		sb.append( ", embeddedNullToken='" ).append( embeddedNullToken ).append( '\'' );
		sb.append( ", embeddedNullFieldBridge=" ).append( embeddedNullFieldBridge );
		sb.append( '}' );
		return sb.toString();
	}

	public static class Builder extends TypeMetadata.Builder {
		private final String embeddedFieldName;
		private final String embeddedFieldPrefix;
		private final XMember embeddedGetter;
		private final Container embeddedContainer;

		private String embeddedNullToken;
		private String embeddedNullFieldName;
		private FieldBridge embeddedNullFieldBridge;

		public Builder(Class<?> indexedType, XMember embeddedGetter, String embeddedFieldPrefix, ScopedAnalyzerReference.Builder scopedAnalyzerBuilder) {
			super( indexedType, scopedAnalyzerBuilder );
			ReflectionHelper.setAccessible( embeddedGetter );
			this.embeddedFieldName = embeddedGetter.getName();
			this.embeddedFieldPrefix = embeddedFieldPrefix;
			this.embeddedGetter = embeddedGetter;
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
			return new EmbeddedTypeMetadata( this );
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


