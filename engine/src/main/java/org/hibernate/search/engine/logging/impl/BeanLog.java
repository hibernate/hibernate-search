/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.logging.impl;

import static org.hibernate.search.engine.logging.impl.EngineLog.ID_OFFSET;
import static org.hibernate.search.engine.logging.impl.EngineLog.ID_OFFSET_LEGACY;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.spi.BeanNotFoundException;
import org.hibernate.search.engine.environment.classpath.spi.ClassLoadingException;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Suppressed;

@CategorizedLogger(
		category = BeanLog.CATEGORY_NAME
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface BeanLog {
	String CATEGORY_NAME = "org.hibernate.search.bean";

	BeanLog INSTANCE = LoggerFactory.make( BeanLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	@Message(id = ID_OFFSET_LEGACY + 242,
			value = "Invalid type '%1$s': missing constructor. The type must expose a public, no-arguments constructor.")
	SearchException noPublicNoArgConstructor(@FormatWith(ClassFormatter.class) Class<?> clazz);

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	@Message(id = ID_OFFSET + 30, value = "Unable to load class '%1$s': %2$s")
	ClassLoadingException unableToLoadTheClass(String className, String causeMessage, @Cause Throwable cause);

	@Message(id = ID_OFFSET + 40, value = "Unable to instantiate class '%1$s': %2$s")
	SearchException unableToInstantiateClass(String className, String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 42, value = "Invalid type '%1$s': this type cannot be assigned to type '%2$s'.")
	SearchException subtypeExpected(@FormatWith(ClassFormatter.class) Class<?> classToLoad,
			@FormatWith(ClassFormatter.class) Class<?> superType);

	@Message(id = ID_OFFSET + 43,
			value = "Invalid type '%1$s': this type is an interface. An implementation class is required.")
	SearchException implementationRequired(@FormatWith(ClassFormatter.class) Class<?> classToLoad);

	@Message(id = ID_OFFSET + 44,
			value = "Invalid type '%1$s': missing constructor. The type must expose a public constructor with a single parameter of type Map.")
	SearchException noPublicMapArgConstructor(@FormatWith(ClassFormatter.class) Class<?> classToLoad);

	@Message(id = ID_OFFSET + 76,
			value = "Ambiguous bean reference to type '%1$s':"
					+ " multiple beans are explicitly defined for this type in Hibernate Search's internal registry."
					+ " Explicitly defined beans: %2$s.")
	BeanNotFoundException multipleConfiguredBeanReferencesForType(
			@FormatWith(ClassFormatter.class) Class<?> exposedType,
			List<? extends BeanReference<?>> references);

	@Message(id = ID_OFFSET + 77,
			value = "No beans defined for type '%1$s' in Hibernate Search's internal registry.")
	BeanNotFoundException noConfiguredBeanReferenceForType(@FormatWith(ClassFormatter.class) Class<?> exposedType);

	@Message(id = ID_OFFSET + 78,
			value = "No beans defined for type '%1$s' and name '%2$s' in Hibernate Search's internal registry.")
	BeanNotFoundException noConfiguredBeanReferenceForTypeAndName(
			@FormatWith(ClassFormatter.class) Class<?> exposedType,
			String nameReference);

	@Message(id = ID_OFFSET + 79,
			value = "Unable to resolve bean reference to type '%1$s' and name '%2$s'. %3$s")
	BeanNotFoundException cannotResolveBeanReference(@FormatWith(ClassFormatter.class) Class<?> typeReference,
			String nameReference, String failureMessages, @Cause RuntimeException mainFailure,
			@Suppressed Collection<? extends RuntimeException> otherFailures);

	@Message(id = ID_OFFSET + 80,
			value = "Unable to resolve bean reference to type '%1$s'. %2$s")
	BeanNotFoundException cannotResolveBeanReference(@FormatWith(ClassFormatter.class) Class<?> typeReference,
			String failureMessages, @Cause RuntimeException beanProviderFailure,
			@Suppressed Collection<? extends RuntimeException> otherFailures);

	// No ID here: this message is always embedded in one of the two exceptions above
	@Message(value = "Failed to resolve bean from Hibernate Search's internal registry with exception: %1$s")
	String failedToResolveBeanUsingInternalRegistry(String exceptionMessage);

	// No ID here: this message is always embedded in one of the two exceptions above
	@Message(value = "Failed to resolve bean from bean manager with exception: %1$s")
	String failedToResolveBeanUsingBeanManager(String exceptionMessage);

	// No ID here: this message is always embedded in one of the two exceptions above
	@Message(value = "Failed to resolve bean using reflection with exception: %1$s")
	String failedToResolveBeanUsingReflection(String exceptionMessage);

	@Message(id = ID_OFFSET + 89, value = "Unable to create bean using reflection: %1$s")
	BeanNotFoundException unableToCreateBeanUsingReflection(String causeMessage, @Cause Exception e);

	@Message(id = ID_OFFSET + 90, value = "No configured bean manager.")
	BeanNotFoundException noConfiguredBeanManager();

	@Message(id = ID_OFFSET + 91, value = "Unable to resolve '%2$s' to a class extending '%1$s': %3$s")
	BeanNotFoundException unableToResolveToClassName(@FormatWith(ClassFormatter.class) Class<?> typReference,
			String nameReference, String causeMessage, @Cause Exception e);

	@Message(id = ID_OFFSET + 92, value = "Invalid bean reference: '%1$s'."
			+ " The reference is prefixed with '%2$s', which is not a valid bean retrieval prefix."
			+ " If you want to reference a bean by name, and the name contains a colon, use 'bean:%1$s'."
			+ " Otherwise, use a valid bean retrieval prefix among the following: %3$s.")
	BeanNotFoundException invalidBeanRetrieval(String beanReference, String invalidPrefix,
			List<String> validPrefixes, @Cause Exception e);
}
