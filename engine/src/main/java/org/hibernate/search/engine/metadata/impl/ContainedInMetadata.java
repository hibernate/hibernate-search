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

import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.search.util.impl.ReflectionHelper;

/**
 * @author Hardy Ferentschik
 */
public class ContainedInMetadata {

	private final XMember containedInMember;
	private final Integer maxDepth;

	public ContainedInMetadata(XMember containedInMember, Integer maxDepth) {
		this.containedInMember = containedInMember;
		ReflectionHelper.setAccessible( this.containedInMember );
		this.maxDepth = maxDepth;
	}

	public XMember getContainedInMember() {
		return containedInMember;
	}

	public Integer getMaxDepth() {
		return maxDepth;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "ContainedInMetadata{" );
		sb.append( "containedInMember=" ).append( containedInMember );
		sb.append( ", maxDepth=" ).append( maxDepth );
		sb.append( '}' );
		return sb.toString();
	}
}


