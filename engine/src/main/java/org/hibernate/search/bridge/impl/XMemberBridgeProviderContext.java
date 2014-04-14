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

import java.lang.reflect.AnnotatedElement;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.engine.service.spi.ServiceManager;

/**
 * Offer a {@code XMember} based {@code BridgeProviderContext}.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class XMemberBridgeProviderContext implements ExtendedBridgeProvider.ExtendedBridgeProviderContext {
	private final AnnotatedElement annotatedElement;
	private final Class<?> returnTypeElement;
	private final String memberName;
	private final ServiceManager serviceManager;
	private final NumericField numericField;

	public XMemberBridgeProviderContext(XMember member, NumericField numericField, ReflectionManager reflectionManager, ServiceManager serviceManager) {
		this.annotatedElement = new XMemberToAnnotatedElementAdaptor( member );
		// For arrays and collection, return the type of the contained elements
		this.returnTypeElement = reflectionManager.toClass( member.getElementClass() );
		this.memberName = member.getName();
		this.serviceManager = serviceManager;
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
	public ServiceManager getServiceManager() {
		return serviceManager;
	}

}
