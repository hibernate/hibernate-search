/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.search.engine.metadata.impl;

import java.util.Map;

import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.impl.ConfigContext;
import org.hibernate.search.util.impl.ReflectionHelper;
import org.hibernate.search.util.impl.ScopedAnalyzer;

/**
 * Class containing all the meta data extracted for a single type.
 *
 * @author Hardy Ferentschik
 */
public class EmbeddedTypeMetadata extends TypeMetadata {
	private final String embeddedFieldName;
	private final XMember embeddedGetter;
	private final Container embeddedContainer;

	private final String embeddedNullFieldName;
	private final String embeddedNullToken;
	private final FieldBridge embeddedNullFieldBridge;

	private EmbeddedTypeMetadata(Builder builder) {
		super( builder );
		this.embeddedFieldName = builder.embeddedFieldName;
		this.embeddedGetter = builder.embeddedGetter;
		this.embeddedContainer = builder.embeddedContainer;

		this.embeddedNullFieldName = builder.embeddedNullFieldName;
		this.embeddedNullToken = builder.embeddedNullToken;
		this.embeddedNullFieldBridge = builder.embeddedNullFieldBridge;
	}

	public String getEmbeddedFieldName() {
		return embeddedFieldName;
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
		sb.append( ", embeddedGetter=" ).append( embeddedGetter );
		sb.append( ", embeddedContainer=" ).append( embeddedContainer );
		sb.append( ", embeddedNullFieldName='" ).append( embeddedNullFieldName ).append( '\'' );
		sb.append( ", embeddedNullToken='" ).append( embeddedNullToken ).append( '\'' );
		sb.append( ", embeddedNullFieldBridge=" ).append( embeddedNullFieldBridge );
		sb.append( '}' );
		return sb.toString();
	}

	public static class Builder extends TypeMetadata.Builder {
		private String embeddedFieldName;
		private XMember embeddedGetter;
		private Container embeddedContainer;

		private String embeddedNullFieldName;
		private String embeddedNullToken;
		private FieldBridge embeddedNullFieldBridge;

		public Builder(Class<?> indexedType, XMember embeddedGetter, ConfigContext configContext, ScopedAnalyzer scopedAnalyzer) {
			super( indexedType, configContext, scopedAnalyzer );
			ReflectionHelper.setAccessible( embeddedGetter );
			this.embeddedFieldName = embeddedGetter.getName();
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


