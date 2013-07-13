/*
 * NestedMICA Motif Inference Toolkit
 *
 * Copyright (c) 2004-2007: Genome Research Ltd.
 *
 * NestedMICA is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * or see the on-line version at http://www.gnu.org/copyleft/lgpl.txt
 *
 */

package batman.utils;

/**
 * General-purpose mathematical utility routines.
 *
 * @author Thomas Down
 */

public class MathsTools {
    private MathsTools() {
    }
    
    /**
     * Adds two log-space numbers.  Equivalent to
     * <code>Math.log(Math.exp(x) + Math.exp(y))</code>, but uses various
     * tricks to avoid underflows.
     */
    
    public static double addLog(double x, double y) {
        if (x == Double.NEGATIVE_INFINITY) {
            return y;
        } else if (y == Double.NEGATIVE_INFINITY) {
            return x;
        } else {
            double base = Math.max(x, y);
            return Math.log(Math.exp(x - base) + Math.exp(y - base)) + base;
        }
    }
    
    /**
     * Bound a number between minimum and maximum values.
     */
     
    public static double bound(double min, double x, double max) {
        if (x < min) {
            return min;
        } else if (x > max) {
            return max;
        } else {
            return x;
        }
    }
    
    /**
     * Bound a number between minimum and maximum values.
     */
     
    public static int bound(int min, int x, int max) {
        if (x < min) {
            return min;
        } else if (x > max) {
            return max;
        } else {
            return x;
        }
    }
    
    /**
     * Return the number of bits set in an integer.
     */
    
    public static int popcnt(int x) {
        int cnt = 0;
        while (x != 0) {
            if ((x & 1) != 0) {
                ++cnt;
            }
            x = x >> 1;
        }
        return cnt;
    }
    
    /**
     * Find the sign of a real number.
     * 
     * @return 1 if <code>d</code> is greater than zero, -1 if <code>d</code> is less that zero, or 0 otherwise.
     */
    
    public static int sign(double d) {
        if (d < 0) {
            return -1;
        } else if (d > 0) {
            return 1;
        } else {
            return 0;
        }
    }
    
    public static int randomInt(int max) {
        return (int) Math.floor(Math.random() * max);
    }

    public static int max(int... a) {
        int m = Integer.MIN_VALUE;
        for (int i : a) {
            if (i > m) {
                m = i;
            }
        }
        return m;
    }
    

	public static double max(double... ds) {
        double m = Double.MIN_VALUE;
        for (double d : ds) {
            if (d > m) {
                m = d;
            }
        }
        return m;
	}
    
    public static int sum(int... a) {
        int t = 0;
        for (int i : a) {
            t += i;
        }
        return t;
    }

	public static double sum(double... a) {
		double t = 0;
		for (double d : a) {
			t += d;
		}
		return t;
	}
	
	/**
	 * Return the integer value of <code>i/j</code> with random distribution of errors.
	 * 
	 * @param i
	 * @param j
	 * @return
	 */
	
	public static int truncateRandomly(int i, int j) {
		int x = i / j;
		int y = i % j;
		if (y > 0 || Math.random() < ((1.0 * y) / j)) {
			++x;
		}
		return x;
	}

}
