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
 * Facade around a matrix.
 * Not really useful on its own, but allows matrix behaviour to be customized.
 *
 * @author Thomas Down
 */

public class ObjectMatrixWrapper1D implements ObjectMatrix1D {
    private ObjectMatrix1D m;
    
    public ObjectMatrixWrapper1D(ObjectMatrix1D m) {
        this.m = m;
    }
    
    public ObjectMatrix1D getWrappedMatrix() {
        return m;
    }
    
    public void setWrappedMatrix(ObjectMatrix1D m) {
        this.m = m;
    }
    
    public int size() {
        return m.size();
    }
   
    public Object get(int pos) {
        return m.get(pos);
    }
    
    public void set(int pos, Object v) {
        m.set(pos, v);
    }
}
