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

package batman.matrix;


/**
 * Utility methods and views for matrices.
 *
 * @author Thomas Down
 */

public class MatrixTools {
    private MatrixTools() {
    }
    
    public static Matrix2D viewTranspose(final Matrix2D m) {
    	return new Matrix2D() {
			public int columns() {
				return m.rows();
			}

			public double get(int row, int col) {
				return m.get(col, row);
			}

			public double[] getRaw() {
				throw new UnsupportedOperationException();
			}

			public int rows() {
				return m.columns();
			}

			public void set(int row, int col, double v) {
				m.set(col, row, v);
			}
    		
    	};
    }
    
    public static Matrix1D viewRow(final Matrix2D m, final int row)
    {
        if (row < 0 || row >= m.rows()) {
            throw new IllegalArgumentException();
        }
        
        return new Matrix1D() {
            public int size() {
                return m.columns();
            }
            
            public double get(int pos) {
                return m.get(row, pos);
            }
            
            public void set(int pos, double val) {
                m.set(row, pos, val);
            }
            
            public double[] getRaw() {
                throw new UnsupportedOperationException();
            }
        } ;
    }
    
    public static ObjectMatrix1D viewRow(final ObjectMatrix2D m, final int row)
    {
        if (row < 0 || row >= m.rows()) {
            throw new IllegalArgumentException();
        }
        
        return new ObjectMatrix1D() {
            public int size() {
                return m.columns();
            }
            
            public Object get(int pos) {
                return m.get(row, pos);
            }
            
            public void set(int pos, Object val) {
                m.set(row, pos, val);
            }
        } ;
    }
    
    public static Matrix1D viewColumn(final Matrix2D m, final int col)
    {
        if (col < 0 || col >= m.columns()) {
            throw new IllegalArgumentException();
        }
        
        return new Matrix1D() {
            public int size() {
                return m.rows();
            }
            
            public double get(int pos) {
                return m.get(pos, col);
            }
            
            public void set(int pos, double val) {
                m.set(pos, col, val);
            }
            
            public double[] getRaw() {
                throw new UnsupportedOperationException();
            }
        } ;
    }
    
    public static ObjectMatrix1D viewColumn(final ObjectMatrix2D m, final int col)
    {
        if (col < 0 || col >= m.columns()) {
            throw new IllegalArgumentException();
        }
        
        return new ObjectMatrix1D() {
            public int size() {
                return m.rows();
            }
            
            public Object get(int pos) {
                return m.get(pos, col);
            }
            
            public void set(int pos, Object val) {
                m.set(pos, col, val);
            }
        } ;
    } 
    
    /**
     * Copy all elements from src to the corresponding elements in dest
     *
     * @throws IllegalArgumentException if the sizes don't match
     */
    
    public static void copy(ObjectMatrix1D dest, ObjectMatrix1D src) {
        if (dest.size() != src.size()) {
            throw new IllegalArgumentException("Matrix sizes don't match (" + dest.size() + " != " + src.size() + ")");
        }
        
        for (int i = 0; i < dest.size(); ++i) {
            dest.set(i, src.get(i));
        }
    }
    
    /**
     * Copy all elements from src to the corresponding elements in dest
     *
     * @throws IllegalArgumentException if the sizes don't match
     */
    
    public static void copy(Matrix1D dest, Matrix1D src) {
        if (dest.size() != src.size()) {
            throw new IllegalArgumentException("Matrix sizes don't match (" + dest.size() + " != " + src.size() + ")");
        }
        
        for (int i = 0; i < dest.size(); ++i) {
            dest.set(i, src.get(i));
        }
    }
    
    public static void copy(Matrix2D dest, Matrix2D src) {
        if (dest.rows() != src.rows() || dest.columns() != src.columns()) {
            throw new IllegalArgumentException("Matrix sizes don't match");
        }
        
        for (int i = 0; i < dest.rows(); ++i) {
            for (int j = 0; j < dest.columns(); ++j) {
                dest.set(i, j, src.get(i, j));
            }
        }
    }
    
    public static void copy(ObjectMatrix2D dest, ObjectMatrix2D src) {
        if (dest.rows() != src.rows() || dest.columns() != src.columns()) {
            throw new IllegalArgumentException("Matrix sizes don't match");
        }
        
        for (int i = 0; i < dest.rows(); ++i) {
            for (int j = 0; j < dest.columns(); ++j) {
                dest.set(i, j, src.get(i, j));
            }
        }
    }
}
