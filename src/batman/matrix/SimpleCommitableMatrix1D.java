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
 * Matrix object which supports commit and rollback operations.
 * 
 * <p>
 * <strong>Not threadsafe</strong>
 * </p>
 *
 * @author Thomas Down
 */

public class SimpleCommitableMatrix1D implements CommitableMatrix1D {
    private static final int EDIT_LIST_SIZE = 5;
    
    private final int size;
    private double[] background;
    private double[] foreground;
    
    private int editCount;
    private int[] editIndex;
    
    public SimpleCommitableMatrix1D(int size) {
        this.size = size;
        this.background = new double[size];
        this.foreground = new double[size];
        editCount = 0;
        editIndex = new int[EDIT_LIST_SIZE];
    }
    
    public SimpleCommitableMatrix1D(int size, double v) {
        this(size);
        for (int i = 0; i < size; ++i) {
            background[i] = foreground[i] = v;
        }
    }
    
    public SimpleCommitableMatrix1D(Matrix1D m) {
        this(m.size());
        for (int i = 0; i < size; ++i) {
            foreground[i] = background[i] = m.get(i);
        }
    }
    
    public int size() {
        return size;
    }
    
    public double get(int pos) {
        return foreground[pos];
    }
    
    public double getCommitted(int pos) {
        return background[pos];
    }
    
    public synchronized void set(int pos, double d) {
        foreground[pos] = d;
        if (editCount < EDIT_LIST_SIZE) {
            editIndex[editCount] = pos;
        }
        ++editCount;
    }
    
    public void commit() {
        sync(background, foreground);
    }
    
    public void rollback() {
        sync(foreground, background);
    }
    
    public boolean isDirty() {
        return editCount > 0;
    }
    
    private synchronized void sync(double[] to, double[] from) {
        if (editCount <= EDIT_LIST_SIZE) {
            for (int e = 0; e < editCount; ++e) {
                int i = editIndex[e];
                to[i] = from[i];
            }
        } else {
            System.arraycopy(from, 0, to, 0, to.length);
        }
        editCount = 0;
    }
    
    public double[] getRaw() {
        return foreground;
    }
}
