package com.variamos.syntaxsupport.metametamodel;

/**
 * @author Juan Carlos Mu�oz 2014
 *  part of the PhD work at CRI - Universite Paris 1
 *
 * Definition of semantics for REFAS
 */
public class ConfigurationAttribute extends AbstractAttribute {
	 /**
	 * 
	 */
	private static final long serialVersionUID = -7430454253717334119L;

	/**
	 * 
	 */
	 
	 public ConfigurationAttribute(String name, String type, boolean affectProperties, String displayName, Object defaultValue)
	 {
		 super(name, type, affectProperties, displayName, defaultValue);
	 }
	 
	 public ConfigurationAttribute(String name, String type,  boolean affectProperties, String displayName, String enumType, Object defaultValue)
	 {
		 super(name, type, affectProperties, displayName, enumType, defaultValue);
	 }
		
}
