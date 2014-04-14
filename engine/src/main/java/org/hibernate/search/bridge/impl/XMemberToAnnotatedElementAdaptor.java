/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

package org.hibernate.search.bridge.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import org.hibernate.annotations.common.reflection.XMember;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class XMemberToAnnotatedElementAdaptor implements AnnotatedElement {
	private XMember delegate;

	public XMemberToAnnotatedElementAdaptor(XMember member) {
		this.delegate = member;
	}

	@Override
	public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
		return delegate.isAnnotationPresent( annotationClass );
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
		return delegate.getAnnotation( annotationClass );
	}

	@Override
	public Annotation[] getAnnotations() {
		return delegate.getAnnotations();
	}

	@Override
	public Annotation[] getDeclaredAnnotations() {
		// Not an awesome delegate but XAnnotatedElement does not deal with the notion of declared annotaions
		return delegate.getAnnotations();
	}
}
