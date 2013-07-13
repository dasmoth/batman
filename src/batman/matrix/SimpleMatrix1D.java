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

import java.io.Serializable;

/**
 * Straightforward implementation of Matrix1D.
 *
 * @author Thomas Down
 */

public class SimpleMatrix1D implements Matrix1D, Serializable {
    private double[] values;
    private final int _size;
    
    public SimpleMatrix1D(int size) {
        this._size = size;
        values = new double[size];
    }
    
    public SimpleMatrix1D(int size, double value) {
        this(size);
        for (int i = 0; i < values.length; ++i) {
            values[i] = value;
        }
    }
    
    public SimpleMatrix1D(Matrix1D m) {
        int size = m.size();
        this._size = size;
        this.values = new double[size];
        for (int i = 0; i < _size; ++i) {
            set(i, m.get(i));
        }
    }
    
    /**
     */
    public SimpleMatrix1D(double[] backing) {
        values = backing;
        _size = backing.length;
    }

    public int size() {
        return _size;
    }
    
    public double get(int pos) {
        return values[pos];
    }
    
    public void set(int pos, double v) {
        values[pos] = v;
    }
    
    public double[] getRaw() {
        return values;
    }
}
