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

package org.ejml.factory;

import org.ejml.EjmlParameters;
import org.ejml.alg.dense.decomposition.chol.CholeskyDecompositionCommon_D32;
import org.ejml.alg.dense.decomposition.chol.CholeskyDecompositionInner_D32;
import org.ejml.alg.dense.decomposition.lu.LUDecompositionAlt_D32;
import org.ejml.alg.dense.decomposition.qr.QRColPivDecompositionHouseholderColumn_D32;
import org.ejml.alg.dense.linsol.AdjustableLinearSolver;
import org.ejml.alg.dense.linsol.chol.LinearSolverChol;
import org.ejml.alg.dense.linsol.chol.LinearSolverCholBlock32;
import org.ejml.alg.dense.linsol.lu.LinearSolverLu;
import org.ejml.alg.dense.linsol.qr.*;
import org.ejml.alg.dense.linsol.svd.SolvePseudoInverseSvd;
import org.ejml.data.DenseMatrix32F;
import org.ejml.interfaces.linsol.LinearSolver;


/**
 * A factory for generating solvers for systems of the form A*x=b, where A and B are known and x is unknown. 
 *
 * @author Peter Abeles
 */
public class LinearSolverFactory {

    /**
     * Creates a general purpose solver.  Use this if you are not sure what you need.
     *
     * @param numRows The number of rows that the decomposition is optimized for.
     * @param numCols The number of columns that the decomposition is optimized for.
     */
    public static LinearSolver<DenseMatrix32F> general( int numRows , int numCols ) {
        if( numRows == numCols )
            return linear(numRows);
        else
            return leastSquares(numRows,numCols);
    }

    /**
     * Creates a solver for linear systems.  The A matrix will have dimensions (m,m).
     *
     * @return A new linear solver.
     */
    public static LinearSolver<DenseMatrix32F> linear( int matrixSize ) {
        return new LinearSolverLu(new LUDecompositionAlt_D32());
    }

    /**
     * Creates a good general purpose solver for over determined systems and returns the optimal least-squares
     * solution.  The A matrix will have dimensions (m,n) where m &ge; n.
     *
     * @param numRows The number of rows that the decomposition is optimized for.
     * @param numCols The number of columns that the decomposition is optimized for.
     * @return A new least-squares solver for over determined systems.
     */
    public static LinearSolver<DenseMatrix32F> leastSquares( int numRows , int numCols ) {
        if(numCols < EjmlParameters.SWITCH_BLOCK64_QR )  {
            return new LinearSolverQrHouseCol();
        } else {
            if( EjmlParameters.MEMORY == EjmlParameters.MemoryUsage.FASTER )
                return new LinearSolverQrBlock32();
            else
                return new LinearSolverQrHouseCol();
        }
    }

    /**
     * Creates a solver for symmetric positive definite matrices.
     *
     * @return A new solver for symmetric positive definite matrices.
     */
    public static LinearSolver<DenseMatrix32F> symmPosDef( int matrixWidth ) {
        if(matrixWidth < EjmlParameters.SWITCH_BLOCK64_CHOLESKY )  {
            CholeskyDecompositionCommon_D32 decomp = new CholeskyDecompositionInner_D32(true);
            return new LinearSolverChol(decomp);
        } else {
            if( EjmlParameters.MEMORY == EjmlParameters.MemoryUsage.FASTER )
                return new LinearSolverCholBlock32();
            else {
                CholeskyDecompositionCommon_D32 decomp = new CholeskyDecompositionInner_D32(true);
                return new LinearSolverChol(decomp);
            }
        }
    }

    /**
     * <p>
     * Linear solver which uses QR pivot decomposition.  These solvers can handle singular systems
     * and should never fail.  For singular systems, the solution might not be as accurate as a
     * pseudo inverse that uses SVD.
     * </p>
     * 
     * <p>
     * For singular systems there are multiple correct solutions.  The optimal 2-norm solution is the
     * solution vector with the minimal 2-norm and is unique.  If the optimal solution is not computed
     * then the basic solution is returned.  See {@link org.ejml.alg.dense.linsol.qr.BaseLinearSolverQrp}
     * for details.  There is only a runtime difference for small matrices, 2-norm solution is slower.
     * </p>
     *
     * <p>
     * Two different solvers are available.  Compute Q will compute the Q matrix once then use it multiple times.
     * If the solution for a single vector is being found then this should be set to false.  If the pseudo inverse
     * is being found or the solution matrix has more than one columns AND solve is being called numerous multiples
     * times then this should be set to true.
     * </p>
     *
     * @param computeNorm2 true to compute the minimum 2-norm solution for singular systems. Try true.
     * @param computeQ Should it precompute Q or use house holder.  Try false;
     * @return Pseudo inverse type solver using QR with column pivots.
     */
    public static LinearSolver<DenseMatrix32F> leastSquaresQrPivot( boolean computeNorm2 , boolean computeQ ) {
        QRColPivDecompositionHouseholderColumn_D32 decomposition =
                new QRColPivDecompositionHouseholderColumn_D32();

        if( computeQ )
            return new SolvePseudoInverseQrp(decomposition,computeNorm2);
        else
            return new LinearSolverQrpHouseCol(decomposition,computeNorm2);
    }

    /**
     * <p>
     * Returns a solver which uses the pseudo inverse.  Useful when a matrix
     * needs to be inverted which is singular.  Two variants of pseudo inverse are provided.  SVD
     * will tend to be the most robust but the slowest and QR decomposition with column pivots will
     * be faster, but less robust.
     * </p>
     * 
     * <p>
     * See {@link #leastSquaresQrPivot} for additional options specific to QR decomposition based
     * pseudo inverse.  These options allow for better runtime performance in different situations.
     * </p>
     *
     * @param useSVD If true SVD will be used, otherwise QR with column pivot will be used.
     * @return Solver for singular matrices.
     */
    public static LinearSolver<DenseMatrix32F> pseudoInverse( boolean useSVD ) {
        if( useSVD )
            return new SolvePseudoInverseSvd();
        else
            return leastSquaresQrPivot(true,false);
    }

    /**
     * Create a solver which can efficiently add and remove elements instead of recomputing
     * everything from scratch.
     */
    public static AdjustableLinearSolver adjustable() {
        return new AdjLinearSolverQr();
    }
}
