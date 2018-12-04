/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse.node;

import mfix.core.parse.node.expr.*;
import mfix.core.parse.node.stmt.*;

/**
 * @author: Jiajun
 * @date: 2018/12/4
 */
public abstract class NodeVisitor {

    public boolean preVisit(Node node) { return true; }
    public boolean postVisit(Node node) { return true; }

    public final boolean visit(Node node) { return true; }

    // statement bellow
    public boolean visit(AnonymousClassDecl node) { return true; }

    public boolean visit(AssertStmt node) { return true; }

    public boolean visit(Blk node) { return true; }

    public boolean visit(BreakStmt node) { return true; }

    public boolean visit(CatClause node) { return true; }

    public boolean visit(ConstructorInv node) { return true; }

    public boolean visit(ContinueStmt node) { return true; }

    public boolean visit(DoStmt node) {return true; }

    public boolean visit(EmptyStmt node) { return true; }

    public boolean visit(EnhancedForStmt node) {return true; }

    public boolean visit(ExpressionStmt node) { return true; }

    public boolean visit(ForStmt node) { return true; }

    public boolean visit(IfStmt node) { return true; }

    public boolean visit(LabeledStmt node) { return true; }

    public boolean visit(ReturnStmt node) { return true; }

    public boolean visit(Stmt node) {return true; }

    public boolean visit(SuperConstructorInv node) { return true; }

    public boolean visit(SwCase node) { return true; }

    public boolean visit(SwitchStmt node) { return true; }

    public boolean visit(SynchronizedStmt node) { return true; }

    public boolean visit(ThrowStmt node) { return true; }

    public boolean visit(TryStmt node) { return true; }

    public boolean visit(TypeDeclarationStmt node) { return true; }

    public boolean visit(VarDeclarationStmt node) { return true; }

    public boolean visit(WhileStmt node) { return true; }

    public boolean visit(MethDecl node) { return true; }

    // expression bellow
    public boolean visit(ArrayAcc node) { return true; }

    public boolean visit(ArrayCreate node) { return true; }

    public boolean visit(ArrayInitial node) { return true; }

    public boolean visit(Assign node) { return true; }

    public boolean visit(AssignOperator node) { return true; }

    public boolean visit(BoolLiteral node) { return true; }

    public boolean visit(CastExpr node) { return true; }

    public boolean visit(CharLiteral node) { return true; }

    public boolean visit(ClassInstanceCreate node) { return true; }

    public boolean visit(Comment node) { return true; }

    public boolean visit(ConditionalExpr node) { return true; }

    public boolean visit(CreationRef node) { return true; }

    public boolean visit(DoubleLiteral node) { return true; }

    public boolean visit(Expr node) { return true; }

    public boolean visit(ExpressionMethodRef node) { return true; }

    public boolean visit(ExprList node) { return true; }

    public boolean visit(FieldAcc node) { return true; }

    public boolean visit(FloatLiteral node) { return true; }

    public boolean visit(InfixExpr node) { return true; }

    public boolean visit(InfixOperator node) { return true; }

    public boolean visit(InstanceofExpr node) { return true; }

    public boolean visit(IntLiteral node) { return true; }

    public boolean visit(Label node) { return true; }

    public boolean visit(LambdaExpr node) { return true; }

    public boolean visit(LongLiteral node) { return true; }

    public boolean visit(MethodInv node) { return true; }

    public boolean visit(MethodRef node) { return true; }

    public boolean visit(MType node) { return true; }

    public boolean visit(NillLiteral node) { return true; }

    public boolean visit(NumLiteral node) { return true; }

    public boolean visit(Operator node) { return true; }

    public boolean visit(ParenthesiszedExpr node) { return true; }

    public boolean visit(PostfixExpr node) { return true; }

    public boolean visit(PostOperator node) { return true; }

    public boolean visit(PrefixExpr node) { return true; }

    public boolean visit(PrefixOperator node) { return true; }

    public boolean visit(QName node) { return true; }

    public boolean visit(SName node) { return true; }

    public boolean visit(StrLiteral node) { return true; }

    public boolean visit(SuperFieldAcc node) { return true; }

    public boolean visit(SuperMethodInv node) { return true; }

    public boolean visit(SuperMethodRef node) { return true; }

    public boolean visit(Svd node) { return true; }

    public boolean visit(ThisExpr node) { return true; }

    public boolean visit(TyLiteral node) { return true; }

    public boolean visit(TypeMethodRef node) { return true; }

    public boolean visit(VarDeclarationExpr node) { return true; }

    public boolean visit(Vdf node) { return true; }


    public final void endVisit(Node node) { }

    public void endVisit(AnonymousClassDecl node) { }

    public void endVisit(AssertStmt node) { }

    public void endVisit(Blk node) {  }

    public void endVisit(BreakStmt node) {  }

    public void endVisit(CatClause node) {  }

    public void endVisit(ConstructorInv node) {  }

    public void endVisit(ContinueStmt node) {  }

    public void endVisit(DoStmt node) { }

    public void endVisit(EmptyStmt node) {  }

    public void endVisit(EnhancedForStmt node) { }

    public void endVisit(ExpressionStmt node) {  }

    public void endVisit(ForStmt node) {  }

    public void endVisit(IfStmt node) {  }

    public void endVisit(LabeledStmt node) {  }

    public void endVisit(ReturnStmt node) {  }

    public void endVisit(Stmt node) { }

    public void endVisit(SuperConstructorInv node) {  }

    public void endVisit(SwCase node) {  }

    public void endVisit(SwitchStmt node) {  }

    public void endVisit(SynchronizedStmt node) {  }

    public void endVisit(ThrowStmt node) {  }

    public void endVisit(TryStmt node) {  }

    public void endVisit(TypeDeclarationStmt node) {  }

    public void endVisit(VarDeclarationStmt node) {  }

    public void endVisit(WhileStmt node) {  }

    public void endVisit(MethDecl node) {  }

    // expression bellow
    public void endVisit(ArrayAcc node) {  }

    public void endVisit(ArrayCreate node) {  }

    public void endVisit(ArrayInitial node) {  }

    public void endVisit(Assign node) {  }

    public void endVisit(AssignOperator node) {  }

    public void endVisit(BoolLiteral node) {  }

    public void endVisit(CastExpr node) {  }

    public void endVisit(CharLiteral node) {  }

    public void endVisit(ClassInstanceCreate node) {  }

    public void endVisit(Comment node) {  }

    public void endVisit(ConditionalExpr node) {  }

    public void endVisit(CreationRef node) {  }

    public void endVisit(DoubleLiteral node) {  }

    public void endVisit(Expr node) {  }

    public void endVisit(ExpressionMethodRef node) {  }

    public void endVisit(ExprList node) {  }

    public void endVisit(FieldAcc node) {  }

    public void endVisit(FloatLiteral node) {  }

    public void endVisit(InfixExpr node) {  }

    public void endVisit(InfixOperator node) {  }

    public void endVisit(InstanceofExpr node) {  }

    public void endVisit(IntLiteral node) {  }

    public void endVisit(Label node) {  }

    public void endVisit(LambdaExpr node) {  }

    public void endVisit(LongLiteral node) {  }

    public void endVisit(MethodInv node) {  }

    public void endVisit(MethodRef node) {  }

    public void endVisit(MType node) {  }

    public void endVisit(NillLiteral node) {  }

    public void endVisit(NumLiteral node) {  }

    public void endVisit(Operator node) {  }

    public void endVisit(ParenthesiszedExpr node) {  }

    public void endVisit(PostfixExpr node) {  }

    public void endVisit(PostOperator node) {  }

    public void endVisit(PrefixExpr node) {  }

    public void endVisit(PrefixOperator node) {  }

    public void endVisit(QName node) {  }

    public void endVisit(SName node) {  }

    public void endVisit(StrLiteral node) {  }

    public void endVisit(SuperFieldAcc node) {  }

    public void endVisit(SuperMethodInv node) {  }

    public void endVisit(SuperMethodRef node) {  }

    public void endVisit(Svd node) {  }

    public void endVisit(ThisExpr node) {  }

    public void endVisit(TyLiteral node) {  }

    public void endVisit(TypeMethodRef node) {  }

    public void endVisit(VarDeclarationExpr node) {  }

    public void endVisit(Vdf node) {  }

}
