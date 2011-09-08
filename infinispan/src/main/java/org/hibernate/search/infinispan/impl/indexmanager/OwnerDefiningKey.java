/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.infinispan.impl.indexmanager;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import org.hibernate.search.infinispan.ExternalizerIds;
import org.infinispan.marshall.AbstractExternalizer;
import org.infinispan.util.Util;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public final class OwnerDefiningKey {

	private final String indexName;
	private final long creationTimestamp;
	private final int hashCode;

	public OwnerDefiningKey(String indexName) {
		this( indexName, System.currentTimeMillis() );
	}

	private OwnerDefiningKey(final String indexName, final long creationTimestamp) {
		if ( indexName == null ) {
			throw new IllegalArgumentException( "indexName must not be null" );
		}
		this.indexName = indexName;
		this.creationTimestamp = creationTimestamp;
		this.hashCode = indexName.hashCode();
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( OwnerDefiningKey.class != obj.getClass() ) {
			return false;
		}
		OwnerDefiningKey other = (OwnerDefiningKey) obj;
		return this.indexName.equals( other.indexName );
	}

	/**
	 * Implement toString in a way which is consistent with the style of
	 * other keys stored in an Infinispan Lucene Directory.
	 * @see LuceneKey2StringMapper#getKeyMapping(String)
	 */
	@Override
	public String toString() {
		return indexName + "|HSO|" + creationTimestamp;
	}

	public static final class Externalizer extends AbstractExternalizer<OwnerDefiningKey> {

		@Override
		public void writeObject(final ObjectOutput output, final OwnerDefiningKey key) throws IOException {
			output.writeUTF( key.indexName );
			output.writeLong( key.creationTimestamp );
		}

		@Override
		public OwnerDefiningKey readObject(final ObjectInput input) throws IOException {
			final String indexName = input.readUTF();
			final long creationTimestamp = input.readLong();
			return new OwnerDefiningKey( indexName, creationTimestamp );
		}

		@Override
		public Integer getId() {
			return ExternalizerIds.INDEX_OWNER_KEY;
		}

		@Override
		public Set<Class<? extends OwnerDefiningKey>> getTypeClasses() {
			return Util.<Class<? extends OwnerDefiningKey>> asSet( OwnerDefiningKey.class );
		}
	}

}
