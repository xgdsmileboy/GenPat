/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse;

import com.microsoft.z3.Context;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Optimize;
import com.microsoft.z3.Status;
import mfix.common.util.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author: Jiajun
 * @date: 2018/12/13
 */
public class Z3Solver {

    public Z3Solver() {
    }

    public Map<Integer, Integer> build(int[][] matrix, Map<String, Set<Pair<Integer, Integer>>> loc2dependencies) {
        HashMap<String, String> cfg = new HashMap<String, String>();
        cfg.put("model", "true");
        Context ctx = new Context(cfg);
        Optimize optimize = ctx.mkOptimize();

        assert matrix.length > 0 && matrix[0].length > 0;
        int rows = matrix.length;
        int columns = matrix[0].length;

        // constants
        IntExpr zero = ctx.mkInt(0);
        IntExpr one = ctx.mkInt(1);

        // cells
        IntExpr[][] vars = new IntExpr[matrix.length][];
        for(int i = 0; i < rows; i++) {
            vars[i] = new IntExpr[matrix[i].length];
            for(int j = 0; j < columns; j ++) {
                // 0 <= vars[i][j] <= matrix[i][j] : each cell can be 0 or 1
                vars[i][j] = ctx.mkIntConst("x_" + i + "_" + j);
                if(matrix[i][j] == 0) {
                    optimize.Assert(ctx.mkEq(vars[i][j], zero));
                } else {
                    optimize.Assert(ctx.mkOr(ctx.mkEq(vars[i][j], zero), ctx.mkEq(vars[i][j], one)));
                }
//                 optimize.Assert(
//                        ctx.mkOr(ctx.mkEq(vars[i][j], zero),
//                                ctx.mkEq(vars[i][j], ctx.mkInt(matrix[i][j]))));
            }
        }

        // each row contains at most one match
        for(int i = 0; i < rows; i ++) {
            optimize.Assert(ctx.mkLe(ctx.mkAdd(vars[i]), one));
        }

        // each column contains at most one match
        IntExpr[] cSum = new IntExpr[columns];
        for(int j = 0; j < columns; j++) {
            cSum[j] = ctx.mkInt(0);
            for(int i = 0; i < rows; i++) {
                cSum[j] = (IntExpr) ctx.mkAdd(vars[i][j], cSum[j]);
            }
            optimize.Assert(ctx.mkLe(cSum[j], one));
        }

        // handle dependencies
        for(int i = 0; i < rows; i ++) {
            for(int j = 0; j < columns; j++) {
                String key = i + "_" + j;
                Set<Pair<Integer, Integer>> denpencies = loc2dependencies.get(key);
                if(denpencies != null) {
                    for (Pair<Integer, Integer> pair : denpencies) {
                        optimize.Assert(ctx.mkImplies(ctx.mkEq(vars[i][j], one),
                                ctx.mkEq(vars[pair.getFirst()][pair.getSecond()], one)));
                    }
                }
            }
        }

        // maximize the number of match
        IntExpr ttSum = ctx.mkInt(0);
        for(int i = 0; i < rows; i++) {
            for(int j = 0; j < columns; j++) {
                ttSum = (IntExpr) ctx.mkAdd(vars[i][j], ttSum);
            }
        }
        Optimize.Handle matches = optimize.MkMaximize(ttSum);
        Map<Integer, Integer> row2colMap = new HashMap<>();
        if(optimize.Check() == Status.SATISFIABLE) {
            Model model = optimize.getModel();
//            System.out.println(model);
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < columns; j++) {
                    if("1".equals(model.getConstInterp(vars[i][j]).toString())) {
                        row2colMap.put(i, j);
                    }
                }
            }
        }
        return row2colMap;
    }

}
