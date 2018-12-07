/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse;

import mfix.common.util.LevelLogger;
import mfix.core.parse.node.MethDecl;
import mfix.core.parse.node.Node;
import mfix.core.parse.node.expr.*;
import mfix.core.parse.node.stmt.*;
import mfix.core.parse.relation.Pattern;
import mfix.core.parse.relation.Relation;

import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/12/4
 */
public class PatternExtraction {

    public static Pattern extract(Node node) {


        return null;
    }


    public void visit(AnonymousClassDecl node, List<Relation> relations) {

    }

    public void visit(AssertStmt node, List<Relation> relations) {

    }

    public void visit(Blk node, List<Relation> relations) {
    }

    public void visit(BreakStmt node, List<Relation> relations) {
    }

    public void visit(CatClause node, List<Relation> relations) {
    }

    public void visit(ConstructorInv node, List<Relation> relations) {
    }

    public void visit(ContinueStmt node, List<Relation> relations) {
    }

    public void visit(DoStmt node, List<Relation> relations) {
    }

    public void visit(EmptyStmt node, List<Relation> relations) {
    }

    public void visit(EnhancedForStmt node, List<Relation> relations) {
    }

    public void visit(ExpressionStmt node, List<Relation> relations) {
    }

    public void visit(ForStmt node, List<Relation> relations) {
    }

    public void visit(IfStmt node, List<Relation> relations) {
    }

    public void visit(LabeledStmt node, List<Relation> relations) {
    }

    public void visit(ReturnStmt node, List<Relation> relations) {
    }

    public void visit(Stmt node, List<Relation> relations) {
    }

    public void visit(SuperConstructorInv node, List<Relation> relations) {
    }

    public void visit(SwCase node, List<Relation> relations) {
    }

    public void visit(SwitchStmt node, List<Relation> relations) {
    }

    public void visit(SynchronizedStmt node, List<Relation> relations) {
    }

    public void visit(ThrowStmt node, List<Relation> relations) {
    }

    public void visit(TryStmt node, List<Relation> relations) {
    }

    public void visit(TypeDeclarationStmt node, List<Relation> relations) {
    }

    public void visit(VarDeclarationStmt node, List<Relation> relations) {
    }

    public void visit(WhileStmt node, List<Relation> relations) {
    }

    public void visit(MethDecl node, List<Relation> relations) {
    }

    // expression bellow
    public void visit(AryAcc node, List<Relation> relations) {
    }

    public void visit(AryCreation node, List<Relation> relations) {
    }

    public void visit(AryInitializer node, List<Relation> relations) {
    }

    public void visit(Assign node, List<Relation> relations) {
    }

    public void visit(AssignOperator node, List<Relation> relations) {
    }

    public void visit(BoolLiteral node, List<Relation> relations) {
    }

    public void visit(CastExpr node, List<Relation> relations) {
    }

    public void visit(CharLiteral node, List<Relation> relations) {
    }

    public void visit(ClassInstCreation node, List<Relation> relations) {
    }

    public void visit(Comment node, List<Relation> relations) {
    }

    public void visit(ConditionalExpr node, List<Relation> relations) {
    }

    public void visit(CreationRef node, List<Relation> relations) {
    }

    public void visit(DoubleLiteral node, List<Relation> relations) {
    }

    public void visit(Expr node, List<Relation> relations) {
    }

    public void visit(ExpressionMethodRef node, List<Relation> relations) {
    }

    public void visit(ExprList node, List<Relation> relations) {
    }

    public void visit(FieldAcc node, List<Relation> relations) {
    }

    public void visit(FloatLiteral node, List<Relation> relations) {
    }

    public void visit(InfixExpr node, List<Relation> relations) {
    }

    public void visit(InfixOperator node, List<Relation> relations) {
    }

    public void visit(InstanceofExpr node, List<Relation> relations) {
    }

    public void visit(IntLiteral node, List<Relation> relations) {
    }

    public void visit(Label node, List<Relation> relations) {
    }

    public void visit(LambdaExpr node, List<Relation> relations) {
    }

    public void visit(LongLiteral node, List<Relation> relations) {
    }

    public void visit(MethodInv node, List<Relation> relations) {
    }

    public void visit(MethodRef node, List<Relation> relations) {
    }

    public void visit(MType node, List<Relation> relations) {
    }

    public void visit(NillLiteral node, List<Relation> relations) {
    }

    public void visit(NumLiteral node, List<Relation> relations) {
    }

    public void visit(Operator node, List<Relation> relations) {

    }

    public void visit(ParenthesiszedExpr node, List<Relation> relations) {
    }

    public void visit(PostfixExpr node, List<Relation> relations) {
    }

    public void visit(PostOperator node, List<Relation> relations) {
    }

    public void visit(PrefixExpr node, List<Relation> relations) {
    }

    public void visit(PrefixOperator node, List<Relation> relations) {
    }

    public void visit(QName node, List<Relation> relations) {
    }

    public void visit(SName node, List<Relation> relations) {
    }

    public void visit(StrLiteral node, List<Relation> relations) {
    }

    public void visit(SuperFieldAcc node, List<Relation> relations) {
    }

    public void visit(SuperMethodInv node, List<Relation> relations) {
    }

    public void visit(SuperMethodRef node, List<Relation> relations) {
    }

    public void visit(Svd node, List<Relation> relations) {
    }

    public void visit(ThisExpr node, List<Relation> relations) {
    }

    public void visit(TyLiteral node, List<Relation> relations) {
    }

    public void visit(TypeMethodRef node, List<Relation> relations) {
    }

    public void visit(VarDeclarationExpr node, List<Relation> relations) {
    }

    public void visit(Vdf node, List<Relation> relations) {
    }


    public void process(Node node, List<Relation> relations) {
        switch (node.getNodeType()) {
            case METHDECL:
                visit((MethDecl) node, relations);
                break;
            case ARRACC:
                visit((AryAcc) node, relations);
                break;
            case ARRCREAT:
                visit((AryCreation) node, relations);
                break;
            case ARRINIT:
                visit((AryInitializer) node, relations);
                break;
            case ASSIGN:
                visit((Assign) node, relations);
                break;
            case BLITERAL:
                visit((BoolLiteral) node, relations);
                break;
            case CAST:
                visit((CastExpr) node, relations);
                break;
            case CLITERAL:
                visit((CharLiteral) node, relations);
                break;
            case CLASSCREATION:
                visit((ClassInstCreation) node, relations);
                break;
            case COMMENT:
                visit((Comment) node, relations);
                break;
            case CONDEXPR:
                visit((ConditionalExpr) node, relations);
                break;
            case DLITERAL:
                visit((DoubleLiteral) node, relations);
                break;
            case FIELDACC:
                visit((FieldAcc) node, relations);
                break;
            case FLITERAL:
                visit((FloatLiteral) node, relations);
                break;
            case INFIXEXPR:
                visit((InfixExpr) node, relations);
                break;
            case INSTANCEOF:
                visit((InstanceofExpr) node, relations);
                break;
            case INTLITERAL:
                visit((IntLiteral) node, relations);
                break;
            case LABEL:
                visit((Label) node, relations);
                break;
            case LLITERAL:
                visit((LongLiteral) node, relations);
                break;
            case MINVOCATION:
                visit((MethodInv) node, relations);
                break;
            case NULL:
                visit((NillLiteral) node, relations);
                break;
            case NUMBER:
                visit((NumLiteral) node, relations);
                break;
            case PARENTHESISZED:
                visit((ParenthesiszedExpr) node, relations);
                break;
            case POSTEXPR:
                visit((PostfixExpr) node, relations);
                break;
            case PREEXPR:
                visit((PrefixExpr) node, relations);
                break;
            case QNAME:
                visit((QName) node, relations);
                break;
            case SNAME:
                visit((SName) node, relations);
                break;
            case SLITERAL:
                visit((StrLiteral) node, relations);
                break;
            case SFIELDACC:
                visit((SuperFieldAcc) node, relations);
                break;
            case SMINVOCATION:
                visit((SuperMethodInv) node, relations);
                break;
            case SINGLEVARDECL:
                visit((Svd) node, relations);
                break;
            case THIS:
                visit((ThisExpr) node, relations);
                break;
            case TLITERAL:
                visit((TyLiteral) node, relations);
                break;
            case VARDECLEXPR:
                visit((VarDeclarationExpr) node, relations);
                break;
            case VARDECLFRAG:
                visit((Vdf) node, relations);
                break;
            case ANONYMOUSCDECL:
                visit((AnonymousClassDecl) node, relations);
                break;
            case ASSERT:
                visit((AssertStmt) node, relations);
                break;
            case BLOCK:
                visit((Blk) node, relations);
                break;
            case BREACK:
                visit((BreakStmt) node, relations);
                break;
            case CONSTRUCTORINV:
                visit((ConstructorInv) node, relations);
                break;
            case CONTINUE:
                visit((ContinueStmt) node, relations);
                break;
            case DO:
                visit((DoStmt) node, relations);
                break;
            case EFOR:
                visit((EnhancedForStmt) node, relations);
                break;
            case FOR:
                visit((ForStmt) node, relations);
                break;
            case IF:
                visit((IfStmt) node, relations);
                break;
            case RETURN:
                visit((ReturnStmt) node, relations);
                break;
            case SCONSTRUCTORINV:
                visit((SuperConstructorInv) node, relations);
                break;
            case SWCASE:
                visit((SwCase) node, relations);
                break;
            case SWSTMT:
                visit((SwitchStmt) node, relations);
                break;
            case SYNC:
                visit((SynchronizedStmt) node, relations);
                break;
            case THROW:
                visit((ThrowStmt) node, relations);
                break;
            case TRY:
                visit((TryStmt) node, relations);
                break;
            case CATCHCLAUSE:
                visit((CatClause) node, relations);
                break;
            case TYPEDECL:
                visit((TypeDeclarationStmt) node, relations);
                break;
            case VARDECLSTMT:
                visit((VarDeclarationStmt) node, relations);
                break;
            case WHILE:
                visit((WhileStmt) node, relations);
                break;
            case POSTOPERATOR:
                visit((PostOperator) node, relations);
                break;
            case INFIXOPERATOR:
                visit((InfixOperator) node, relations);
                break;
            case PREFIXOPERATOR:
                visit((PrefixOperator) node, relations);
                break;
            case ASSIGNOPERATOR:
                visit((AssignOperator) node, relations);
                break;
            case TYPE:
                visit((MType) node, relations);
                break;
            case UNKNOWN:
                LevelLogger.warn("Found an unknown node type ! ");
                break;
            default:
                LevelLogger.fatal("Cannot parse node type ! ");
        }


    }

}
