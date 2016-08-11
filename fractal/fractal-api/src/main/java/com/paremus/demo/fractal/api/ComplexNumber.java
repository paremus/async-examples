package com.paremus.demo.fractal.api;

import java.io.Serializable;

/**
 * A simple implementation of a complex number
 */
public class ComplexNumber implements Serializable, Comparable<ComplexNumber> {

    private static final long serialVersionUID = 1L;

    private double real;
    private double imaginary;

    public ComplexNumber(double real, double imaginary) {
        this.real = real;
        this.imaginary = imaginary;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(imaginary);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(real);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ComplexNumber other = (ComplexNumber) obj;
        if (Double.doubleToLongBits(imaginary) != Double.doubleToLongBits(other.imaginary))
            return false;
        if (Double.doubleToLongBits(real) != Double.doubleToLongBits(other.real))
            return false;
        return true;
    }

    public int compareTo(ComplexNumber o) {
        int c = compare(real, o.real);

        if (c == 0) {
            c = compare(imaginary, o.imaginary);
        }

        return c;
    }

    private int compare(double d1, double d2) {
        double diff = d1 - d2;

        if (diff < 0) {
            return -1;
        }
        else if (diff > 0) {
            return 1;
        }
        else {
            return 0;
        }
    }

    public double getImaginary() {
        return imaginary;
    }

    public double getReal() {
        return real;
    }

    public double getMagnitude() {
        return Math.sqrt((real * real) + (imaginary * imaginary));
    }

    public ComplexNumber square() {
        double r = (real * real) - (imaginary * imaginary);
        double i = 2 * (real * imaginary);
        return new ComplexNumber(r, i);
    }

    public ComplexNumber plus(ComplexNumber z) {
        return new ComplexNumber(real + z.real, imaginary + z.imaginary);
    }

    public String toString() {
        return real + "+i" + imaginary;
    }
}
