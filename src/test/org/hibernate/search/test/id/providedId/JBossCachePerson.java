package org.hibernate.search.test.id.providedId;

import org.hibernate.search.annotations.*;
import org.hibernate.annotations.Entity;

import java.io.Serializable;


/**
 @author Navin Surtani (<a href="mailto:nsurtani@redhat.com">nsurtani@redhat.com</a>)
 */
@Entity
@ProvidedId
@Indexed
public class JBossCachePerson implements Serializable
{

   @Field (index = Index.TOKENIZED, store = Store.YES)
   private String name;
   @Field (index = Index.TOKENIZED, store = Store.YES)
   private String blurb;
   @Field (index = Index.UN_TOKENIZED, store = Store.YES)
   private int age;

   public String getName()
   {
      return name;
   }

   public void setName(String name)
   {
      this.name = name;
   }

   public String getBlurb()
   {
      return blurb;
   }

   public void setBlurb(String blurb)
   {
      this.blurb = blurb;
   }

   public int getAge()
   {
      return age;
   }

   public void setAge(int age)
   {
      this.age = age;
   }

}
