package org.hibernate.search.cfg;

import java.util.Properties;
import java.util.Iterator;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;

/**
 * Search configuration implementation wrapping an Hibernate Core configuration
 *
 * @author Emmanuel Bernard
 */
public class SearchConfigurationFromHibernateCore implements SearchConfiguration
{
   private org.hibernate.cfg.Configuration cfg;
   private ReflectionManager reflectionManager;

   public SearchConfigurationFromHibernateCore(org.hibernate.cfg.Configuration cfg)
   {
      if (cfg == null) throw new NullPointerException("Configuration is null");
      this.cfg = cfg;
   }

   public Iterator<Class> getClassMappings()
   {
      return new ClassIterator(cfg.getClassMappings());
   }

   public Class getClassMapping(String name)
   {
      return cfg.getClassMapping(name).getMappedClass();
   }

   public String getProperty(String propertyName)
   {
      return cfg.getProperty(propertyName);
   }

   public Properties getProperties()
   {
      return cfg.getProperties();
   }

   public ReflectionManager getReflectionManager()
   {
      if (reflectionManager == null)
      {
         try
         {
            //TODO introduce a ReflectionManagerHolder interface to avoid reflection
            //I want to avoid hard link between HAN and Validator for such a simple need
            //reuse the existing reflectionManager one when possible
            reflectionManager =
                    (ReflectionManager) cfg.getClass().getMethod("getReflectionManager").invoke(cfg);

         }
         catch (Exception e)
         {
            reflectionManager = new JavaReflectionManager();
         }
      }
      return reflectionManager;
   }

   private class ClassIterator implements Iterator<Class>
   {
      private Iterator hibernatePersistentClassIterator;

      private ClassIterator(Iterator hibernatePersistentClassIterator)
      {
         this.hibernatePersistentClassIterator = hibernatePersistentClassIterator;
      }

      public boolean hasNext()
      {
         return hibernatePersistentClassIterator.hasNext();
      }

      public Class next()
      {
         PersistentClass pc = (PersistentClass) hibernatePersistentClassIterator.next();
         return pc.getMappedClass();
      }

      public void remove()
      {
         hibernatePersistentClassIterator.remove();
      }
   }
}
