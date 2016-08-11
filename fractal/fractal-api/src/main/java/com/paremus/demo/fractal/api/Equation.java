package com.paremus.demo.fractal.api;

/**
 * An equation for generating a fractal
 */
public interface Equation {
	/**
	 * The service property which contains the name of this equation.
	 */
	public static final String EQUATION_TYPE = "equation.type";
	
	/**
	 * Calculate pixel colours over the X Y plane, using the fractal 
	 * generating function represented by this {@link Equation}
	 * 
	 * @param width  - the width, in pixels, that should be generated
	 * @param height - the height, in pixels, that should be generated
	 * @param startX - the minimum value of X (the point on the real line)
	 * @param deltaX - the increment of X between pixels
	 * @param startY - the minimum value of Y (the point on the imaginary line)
	 * @param deltaY - the increment of Y between pixels
	 * @param maxIterations - the maximum number of iterations to make before assuming convergence
	 * @param colourDepth - the number of colours available in the display
	 * 
	 * @return An array of size int[width][height]. Location [0][0] represents the top-left 
	 *    corner of the image, i.e. the <em>minimum</em> X position, and the <em>maximum</em>
	 *    Y position. Each int in the array represents the colour of a pixel, and must be
	 *    between 0 and colourDepth - 1.
	 */
    int[][] execute(int width, int height, double startX, double deltaX, 
    		double startY, double deltaY, int maxIterations, int colourDepth);
}
