//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.8-b130911.1802 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2018.04.01 à 01:58:36 AM CEST 
//

package com.apple.propertylist;

import javax.xml.bind.annotation.XmlRegistry;

/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the com.apple.propertylist package.
 * <p>
 * An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups. Factory methods for each of these are
 * provided in this class.
 * -
 * IMPORTED METHOD:
 * xjc -dtd -p com.apple.propertylist http://www.apple.com/DTDs/PropertyList-1.0.dtd
 */
@XmlRegistry
public class ObjectFactory {
	
	/**
	 * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.apple.propertylist
	 */
	public ObjectFactory() {
	}
	
	/**
	 * Create an instance of {@link Date }
	 */
	public Date createDate() {
		return new Date();
	}
	
	/**
	 * Create an instance of {@link Plist }
	 */
	public Plist createPlist() {
		return new Plist();
	}
	
	/**
	 * Create an instance of {@link Array }
	 */
	public Array createArray() {
		return new Array();
	}
	
	/**
	 * Create an instance of {@link Data }
	 */
	public Data createData() {
		return new Data();
	}
	
	/**
	 * Create an instance of {@link Dict }
	 */
	public Dict createDict() {
		return new Dict();
	}
	
	/**
	 * Create an instance of {@link Real }
	 */
	public Real createReal() {
		return new Real();
	}
	
	/**
	 * Create an instance of {@link Integer }
	 */
	public Integer createInteger() {
		return new Integer();
	}
	
	/**
	 * Create an instance of {@link String }
	 */
	public String createString() {
		return new String();
	}
	
	/**
	 * Create an instance of {@link True }
	 */
	public True createTrue() {
		return new True();
	}
	
	/**
	 * Create an instance of {@link False }
	 */
	public False createFalse() {
		return new False();
	}
	
	/**
	 * Create an instance of {@link Key }
	 */
	public Key createKey() {
		return new Key();
	}
	
}
