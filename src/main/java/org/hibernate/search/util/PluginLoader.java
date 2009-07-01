package org.hibernate.search.util;

import org.hibernate.search.SearchException;
import org.hibernate.util.ReflectHelper;

/**
 * 
 * @author Sanne Grinovero
 *
 * @param <T>
 */
public class PluginLoader {
	
	public static <T> T instanceFromName(Class<T> targetSuperType, String classNameToLoad,
			Class<?> caller, String componentDescription) {
		final Class<?> clazzDef;
		try {
			clazzDef = ReflectHelper.classForName( classNameToLoad, caller );
		} catch (ClassNotFoundException e) {
			throw new SearchException( "Unable to find " + componentDescription +
					" implementation class: " + classNameToLoad, e );
		}
		return instanceFromClass( targetSuperType, clazzDef, componentDescription );
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T instanceFromClass(Class<T> targetSuperType, Class<?> classToLoad, String componentDescription) {
		checkClassType( classToLoad, componentDescription );
		checkHasValidconstructor( classToLoad, componentDescription );
		Object instance;
		try {
			instance =  classToLoad.newInstance();
		}
		catch ( IllegalAccessException e ) {
			throw new SearchException(
					"Unable to instantiate " + componentDescription + " class: " + classToLoad.getName() +
					". Class or constructor is not accessible.", e );
		}
		catch ( InstantiationException e ) {
			throw new SearchException(
					"Unable to instantiate " + componentDescription + " class: " + classToLoad.getName() +
					". Verify it has a no-args public constructor and is not abstract.", e );
		}
		if ( ! targetSuperType.isInstance( instance ) ) {
			// have a proper error message according to interface implementation or subclassing
			if ( targetSuperType.isInterface() ) {
				throw new SearchException(
						"Wrong configuration of " + componentDescription + ": class " + classToLoad.getName()
						+ " does not implement interface " + targetSuperType.getName() );
			}
			else {
				throw new SearchException(
						"Wrong configuration of " + componentDescription + ": class " + classToLoad.getName()
						+ " is not a subtype of " + targetSuperType.getName() );
			}
		}
		else {
			return (T) instance;
		}
	}

	private static void checkClassType(Class<?> classToLoad, String componentDescription) {
		if ( classToLoad.isInterface() ) {
			throw new SearchException( classToLoad.getName() + " defined for component " + componentDescription
					+ " is an interface: implementation required." );
		}
	}

	public static void checkHasValidconstructor(Class<?> classToLoad, String componentDescription) {
		try {
			classToLoad.getConstructor();
		} catch (SecurityException e) {
			throw new SearchException( classToLoad.getName() + " defined for component " + componentDescription
					+ " could not be instantiated because of a security manager error", e );
		} catch (NoSuchMethodException e) {
			throw new SearchException( classToLoad.getName() + " defined for component " + componentDescription
					+ " is missing a no-arguments constructor" );
		}
	}

}
