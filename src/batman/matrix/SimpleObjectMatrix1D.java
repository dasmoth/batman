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
 * Straightforward implementation of ObjectMatrix1D.
 *
 * @author Thomas Down
 */

public class SimpleObjectMatrix1D implements ObjectMatrix1D, Serializable {
    private Object[] values;
    private final int _size;
    
    public SimpleObjectMatrix1D(int size) {
        this._size = size;
        values = new Object[size];
    }
    
    
    public SimpleObjectMatrix1D(ObjectMatrix1D m) {
        this(m.size());
        for (int i = 0; i < m.size(); ++i) {
            set(i, m.get(i));
        }
    }
    
    public int size() {
        return _size;
    }
    
    public Object get(int pos) {
        return values[pos];
    }
    
    public void set(int pos, Object v) {
        values[pos] = v;
    }
}
