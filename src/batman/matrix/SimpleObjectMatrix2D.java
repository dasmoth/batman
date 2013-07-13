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
 * Straightforward implementation of ObjectMatrix2D.
 *
 * @author Thomas Down
 */

public class SimpleObjectMatrix2D implements ObjectMatrix2D, Serializable {
    private Object[] values;
    private final int _rows;
    private final int _columns;
    
    public SimpleObjectMatrix2D(int rows, int columns) {
        this._rows = rows;
        this._columns = columns;
        values = new Object[rows * columns];
    }
    
    public SimpleObjectMatrix2D(ObjectMatrix2D m) {
        this(m.rows(), m.columns());
        for (int i = 0; i < m.rows(); ++i) {
            for (int j = 0; j < m.columns(); ++j) {
                set(i, j, m.get(i, j));
            }
        }
    }
    
    public int rows() {
        return _rows;
    }
    
    public int columns() {
        return _columns;
    }
    
    public Object get(int row, int col) {
        return values[row * _columns + col];
    }
    
    public void set(int row, int col, Object v) {
        values[row * _columns + col] = v;
    }
}
