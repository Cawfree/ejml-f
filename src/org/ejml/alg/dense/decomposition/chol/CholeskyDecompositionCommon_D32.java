/*
 * Copyright (c) 2009-2014, Peter Abeles. All Rights Reserved.
 *
 * This file is part of Efficient Java Matrix Library (EJML).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ejml.alg.dense.decomposition.chol;


import org.ejml.data.DenseMatrix32F;
import org.ejml.interfaces.decomposition.CholeskyDecomposition;
import org.ejml.ops.CommonOps;


/**
 *
 * <p>
 * This is an abstract class for a Cholesky decomposition.  It provides the solvers, but the actual
 * decompsoition is provided in other classes.
 * </p>
 * <p>
 * A Cholesky Decomposition is a special decomposition for positive-definite symmetric matrices
 * that is more efficient than other general purposes decomposition. It refactors matrices
 * using one of the two following equations:<br>
 * <br>
 * L*L<sup>T</sup>=A<br>
 * R<sup>T</sup>*R=A<br>
 * <br>
 * where L is a lower triangular matrix and R is an upper traingular matrix.<br>
 * </p>
 *
 * @see CholeskyDecompositionInner_D32
 * @see CholeskyDecompositionBlock_D32
 * @see CholeskyDecompositionLDL_D32
 *
 * @author Peter Abeles
 */
public abstract class CholeskyDecompositionCommon_D32
        implements CholeskyDecomposition<DenseMatrix32F> {

    // it can decompose a matrix up to this width
    protected int maxWidth=-1;

    // width and height of the matrix
    protected int n;

    // the decomposed matrix
    protected DenseMatrix32F T;
    protected float[] t;

    // tempoary variable used by various functions
    protected float vv[];

    // is it a lower triangular matrix or an upper triangular matrix
    protected boolean lower;

    /**
     * Creates a CholeksyDecomposition capable of decompositong a matrix that is
     * n by n, where n is the width.
     *
     * @param lower should a lower or upper triangular matrix be used.
     */
    public CholeskyDecompositionCommon_D32(boolean lower) {
        this.lower = lower;
    }

    public void setExpectedMaxSize( int numRows , int numCols ) {
        if( numRows != numCols ) {
            throw new IllegalArgumentException("Can only decompose square matrices");
        }

        this.maxWidth = numCols;

        this.vv = new float[maxWidth];
    }

    /**
     * If true the decomposition was for a lower triangular matrix.
     * If false it was for an upper triangular matrix.
     *
     * @return True if lower, false if upper.
     */
    @Override
    public boolean isLower() {
        return lower;
    }

    /**
     * <p>
     * Performs Choleksy decomposition on the provided matrix.
     * </p>
     *
     * <p>
     * If the matrix is not positive definite then this function will return
     * false since it can't complete its computations.  Not all errors will be
     * found.  This is an efficient way to check for positive definiteness.
     * </p>
     * @param mat A symmetric positive definite matrix with n <= widthMax.
     * @return True if it was able to finish the decomposition.
     */
    @Override
    public boolean decompose( DenseMatrix32F mat ) {
        if( mat.numRows > maxWidth ) {
            setExpectedMaxSize(mat.numRows,mat.numCols);
        } else if( mat.numRows != mat.numCols ) {
            throw new IllegalArgumentException("Must be a square matrix.");
        }

        n = mat.numRows;

        T = mat;
        t = T.data;

        if(lower) {
            return decomposeLower();
        } else {
            return decomposeUpper();
        }
    }

    @Override
    public boolean inputModified() {
        return true;
    }

    /**
     * Performs an lower triangular decomposition.
     *
     * @return true if the matrix was decomposed.
     */
    protected abstract boolean decomposeLower();

    /**
     * Performs an upper triangular decomposition.
     *
     * @return true if the matrix was decomposed.
     */
    protected abstract boolean decomposeUpper();

    @Override
    public DenseMatrix32F getT( DenseMatrix32F T ) {
        // see if it needs to declare a new matrix or not
        if( T == null ) {
            T = new DenseMatrix32F(n,n);
        } else {
            if( T.numRows != n || T.numCols != n )
                throw new IllegalArgumentException("Unexpected matrix dimension for T.");

            CommonOps.fill(T, 0);
        }

        // write the values to T
        if( lower ) {
            for( int i = 0; i < n; i++ ) {
                for( int j = 0; j <= i; j++ ) {
                    T.unsafe_set(i,j,this.T.unsafe_get(i,j));
                }
            }
        } else {
            for( int i = 0; i < n; i++ ) {
                for( int j = i; j < n; j++ ) {
                    T.unsafe_set(i,j,this.T.unsafe_get(i,j));
                }
            }
        }

        return T;
    }

    /**
     * Returns the triangular matrix from the decomposition.
     *
     * @return A lower or upper triangular matrix.
     */
    public DenseMatrix32F getT() {
        return T;
    }

    public float[] _getVV() {
        return vv;
    }
}