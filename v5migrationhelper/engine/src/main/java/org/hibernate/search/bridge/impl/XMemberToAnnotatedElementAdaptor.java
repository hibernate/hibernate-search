/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.bridge.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import org.hibernate.annotations.common.reflection.XMember;

/**
 * @author Emmanuel Bernard
 */
class XMemberToAnnotatedElementAdaptor implements AnnotatedElement {

	private final XMember delegate;

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
