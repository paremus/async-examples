package com.paremus.demo.fractal.api;

/**
 * A colour map provides colour information for displaying fractals.
 */
public interface ColourMap {
	/**
	 * The service property which contains the name of this colour scheme.
	 */
	public static final String PROFILE_NAME = "profile.name";

	/**
	 * Obtain The complete set of colours in this colour scheme. 
	 * <p>
	 * Each colour should be in a css-friendly form, e.g. #112233
	 * </p>
	 * @return An array of colour strings, never <code>null</code>
	 */
	String[] getSpectrum();
	
}
