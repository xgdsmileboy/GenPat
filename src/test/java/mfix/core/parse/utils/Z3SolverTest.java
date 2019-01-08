/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.utils;

import com.microsoft.z3.Context;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.Log;
import com.microsoft.z3.Optimize;
import com.microsoft.z3.Version;
import com.microsoft.z3.Z3Exception;

import java.util.HashMap;

/**
 * @author: Jiajun
 * @date: 2018/12/15
 */
public class Z3SolverTest {

    /**
     * Not run for normal testing,
     * used only for demo test
     */
    //@Test
    public void test_demo() {
        Z3Demo p = new Z3Demo();
        try {
            com.microsoft.z3.Global.ToggleWarningMessages(true);
            Log.open("test.log");

            System.out.print("Z3 Major Version: ");
            System.out.println(Version.getMajor());
            System.out.print("Z3 Full Version: ");
            System.out.println(Version.getString());


            { // These examples need model generation turned on.
                HashMap<String, String> cfg = new HashMap<String, String>();
                cfg.put("model", "true");
                cfg.put("proof", "true");
                Context ctx = new Context(cfg);

                p.optimizeAssignVMs(ctx);

            }

            Log.close();
            if (Log.isOpen())
                System.out.println("Log is still open!");
        } catch (Z3Exception ex) {
            System.out.println("Z3 Managed Exception: " + ex.getMessage());
            System.out.println("Stack trace: ");
            ex.printStackTrace(System.out);
        } catch (Exception ex) {
            System.out.println("Unknown Exception: " + ex.getMessage());
            System.out.println("Stack trace: ");
            ex.printStackTrace(System.out);
        }
    }
}

/**
 * this class is just for learning
 */
class Z3Demo {

    public void optimizeAssignVMs(Context ctx) {
        System.out.println("Optimize Assign VMs");
        Optimize optimize = ctx.mkOptimize();

        IntExpr ix11 = ctx.mkIntConst("x11");
        IntExpr ix12 = ctx.mkIntConst("x12");
        IntExpr ix13 = ctx.mkIntConst("x13");
        IntExpr ix21 = ctx.mkIntConst("x21");
        IntExpr ix22 = ctx.mkIntConst("x22");
        IntExpr ix23 = ctx.mkIntConst("x23");
        IntExpr ix31 = ctx.mkIntConst("x31");
        IntExpr ix32 = ctx.mkIntConst("x32");
        IntExpr ix33 = ctx.mkIntConst("x33");

        IntExpr iy1 = ctx.mkIntConst("y1");
        IntExpr iy2 = ctx.mkIntConst("y2");
        IntExpr iy3 = ctx.mkIntConst("y3");

        IntExpr zero = ctx.mkInt(0);
        IntExpr one = ctx.mkInt(1);

        optimize.Assert(
                ctx.mkAnd(ctx.mkGe(ix11, zero),
                        ctx.mkGe(ix12, zero),
                        ctx.mkGe(ix13, zero),
                        ctx.mkGe(ix21, zero),
                        ctx.mkGe(ix22, zero),
                        ctx.mkGe(ix23, zero),
                        ctx.mkGe(ix31, zero),
                        ctx.mkGe(ix32, zero),
                        ctx.mkGe(ix33, zero)));
        optimize.Assert(
                ctx.mkAnd(ctx.mkLe(iy1, one),
                        ctx.mkLe(iy2, one),
                        ctx.mkLe(iy3, one)));
        optimize.Assert(
                ctx.mkAnd(ctx.mkEq(ctx.mkAdd(ix11, ix12, ix13), one),
                        ctx.mkEq(ctx.mkAdd(ix21, ix22, ix23), one),
                        ctx.mkEq(ctx.mkAdd(ix31, ix32, ix33), one)));

        optimize.Assert(
                ctx.mkAnd(ctx.mkGe(iy1, ix11),
                        ctx.mkGe(iy1, ix21),
                        ctx.mkGe(iy1, ix31)));

        optimize.Assert(
                ctx.mkAnd(ctx.mkGe(iy2, ix12),
                        ctx.mkGe(iy2, ix22),
                        ctx.mkGe(iy2, ix32)));

        optimize.Assert(
                ctx.mkAnd(ctx.mkGe(iy3, ix13),
                        ctx.mkGe(iy3, ix23),
                        ctx.mkGe(iy3, ix33)));

        IntExpr int100 = ctx.mkInt(100);
        IntExpr int50 = ctx.mkInt(50);
        IntExpr int15 = ctx.mkInt(15);
        IntExpr int75 = ctx.mkInt(75);
        IntExpr int200 = ctx.mkInt(200);
        optimize.Assert(
                ctx.mkLe(ctx.mkAdd(
                        ctx.mkMul(int100, ix11),
                        ctx.mkMul(int50, ix21),
                        ctx.mkMul(int15, ix31)),
                        ctx.mkMul(int100, iy1)));

        optimize.Assert(
                ctx.mkLe(ctx.mkAdd(
                        ctx.mkMul(int100, ix12),
                        ctx.mkMul(int50, ix22),
                        ctx.mkMul(int15, ix32)),
                        ctx.mkMul(int75, iy2)));

        optimize.Assert(
                ctx.mkLe(ctx.mkAdd(
                        ctx.mkMul(int100, ix13),
                        ctx.mkMul(int50, ix23),
                        ctx.mkMul(int15, ix33)),
                        ctx.mkMul(int200, iy3)));

        IntExpr int10 = ctx.mkInt(10);
        IntExpr int5 = ctx.mkInt(5);
        IntExpr int20 = ctx.mkInt(20);

        Optimize.Handle overhead = optimize.MkMinimize(ctx.mkAdd(iy1, iy2, iy3));
        Optimize.Handle cost = optimize.MkMinimize(
                ctx.mkAdd(ctx.mkMul(int10, iy1),
                        ctx.mkMul(int5, iy2),
                        ctx.mkMul(int20, iy3)));

        System.out.println("check : " + optimize.Check());

        System.out.println(optimize.getModel().getConstInterp(iy1));
        System.out.println(optimize.getModel().getConstInterp(iy2));
        System.out.println(optimize.getModel().getConstInterp(iy3));

        System.out.println(optimize.getModel().getConstInterp(ix11));
        System.out.println(optimize.getModel().getConstInterp(ix12));
        System.out.println(optimize.getModel().getConstInterp(ix13));
    }
}
