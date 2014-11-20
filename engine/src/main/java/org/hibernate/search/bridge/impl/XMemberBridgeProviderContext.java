/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.bridge.impl;

import java.lang.reflect.AnnotatedElement;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;

/**
 * Offer a {@code XMember} based {@code BridgeProviderContext}.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
class XMemberBridgeProviderContext implements ExtendedBridgeProvider.ExtendedBridgeProviderContext {

	private final AnnotatedElement annotatedElement;
	private final Class<?> returnTypeElement;
	private final String memberName;
	private final ClassLoaderService classLoaderService;
	private final NumericField numericField;

	public XMemberBridgeProviderContext(XMember member, NumericField numericField, ReflectionManager reflectionManager, ClassLoaderService classLoaderService) {
		this.annotatedElement = new XMemberToAnnotatedElementAdaptor( member );
		// For arrays and collection, return the type of the contained elements
		this.returnTypeElement = reflectionManager.toClass( member.getElementClass() );
		this.memberName = member.getName();
		this.classLoaderService = classLoaderService;
		this.numericField = numericField;
	}

	@Override
	public Class<?> getReturnType() {
		return returnTypeElement;
	}

	@Override
	public AnnotatedElement getAnnotatedElement() {
		return annotatedElement;
	}

	@Override
	public NumericField getNumericField() {
		return numericField;
	}

	@Override
	public String getMemberName() {
		return memberName;
	}

	@Override
	public ClassLoaderService getClassLoaderService() {
		return classLoaderService;
	}

}
