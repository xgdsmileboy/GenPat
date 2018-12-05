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

/**
 * @author: Jiajun
 * @date: 2018/12/4
 */
public class PatternExtraction {

    private static PatternExtraction patternExtraction = new PatternExtraction();

    private PatternExtraction(){}

    public static Pattern extract(Node oldNode, Node newNode) {
        Pattern pattern = new Pattern();
        pattern.setOldRelationFlag(true);
        patternExtraction.process(oldNode, pattern);
        pattern.setOldRelationFlag(false);
        patternExtraction.process(newNode, pattern);
        return pattern;
    }


    public Relation visit(AnonymousClassDecl node, Pattern pattern) {

        return null;
    }

    public Relation visit(AssertStmt node, Pattern pattern) {

        return null;
    }

    public Relation visit(Blk node, Pattern pattern) {

        return null;
    }

    public Relation visit(BreakStmt node, Pattern pattern) {

        return null;
    }

    public Relation visit(CatClause node, Pattern pattern) {

        return null;
    }

    public Relation visit(ConstructorInv node, Pattern pattern) {

        return null;
    }

    public Relation visit(ContinueStmt node, Pattern pattern) {

        return null;
    }

    public Relation visit(DoStmt node, Pattern pattern) {

        return null;
    }

    public Relation visit(EmptyStmt node, Pattern pattern) {

        return null;
    }

    public Relation visit(EnhancedForStmt node, Pattern pattern) {

        return null;
    }

    public Relation visit(ExpressionStmt node, Pattern pattern) {

        return null;
    }

    public Relation visit(ForStmt node, Pattern pattern) {

        return null;
    }

    public Relation visit(IfStmt node, Pattern pattern) {

        return null;
    }

    public Relation visit(LabeledStmt node, Pattern pattern) {

        return null;
    }

    public Relation visit(ReturnStmt node, Pattern pattern) {

        return null;
    }

    public Relation visit(Stmt node, Pattern pattern) {

        return null;
    }

    public Relation visit(SuperConstructorInv node, Pattern pattern) {

        return null;
    }

    public Relation visit(SwCase node, Pattern pattern) {

        return null;
    }

    public Relation visit(SwitchStmt node, Pattern pattern) {

        return null;
    }

    public Relation visit(SynchronizedStmt node, Pattern pattern) {

        return null;
    }

    public Relation visit(ThrowStmt node, Pattern pattern) {

        return null;
    }

    public Relation visit(TryStmt node, Pattern pattern) {

        return null;
    }

    public Relation visit(TypeDeclarationStmt node, Pattern pattern) {

        return null;
    }

    public Relation visit(VarDeclarationStmt node, Pattern pattern) {

        return null;
    }

    public Relation visit(WhileStmt node, Pattern pattern) {

        return null;
    }

    public Relation visit(MethDecl node, Pattern pattern) {

        return null;
    }

    // expression bellow
    public Relation visit(AryAcc node, Pattern pattern) {

        return null;
    }

    public Relation visit(AryCreation node, Pattern pattern) {

        return null;
    }

    public Relation visit(AryInitializer node, Pattern pattern) {

        return null;
    }

    public Relation visit(Assign node, Pattern pattern) {

        return null;
    }

    public Relation visit(AssignOperator node, Pattern pattern) {

        return null;
    }

    public Relation visit(BoolLiteral node, Pattern pattern) {

        return null;
    }

    public Relation visit(CastExpr node, Pattern pattern) {

        return null;
    }

    public Relation visit(CharLiteral node, Pattern pattern) {

        return null;
    }

    public Relation visit(ClassInstCreation node, Pattern pattern) {

        return null;
    }

    public Relation visit(Comment node, Pattern pattern) {

        return null;
    }

    public Relation visit(ConditionalExpr node, Pattern pattern) {

        return null;
    }

    public Relation visit(CreationRef node, Pattern pattern) {

        return null;
    }

    public Relation visit(DoubleLiteral node, Pattern pattern) {

        return null;
    }

    public Relation visit(Expr node, Pattern pattern) {

        return null;
    }

    public Relation visit(ExpressionMethodRef node, Pattern pattern) {

        return null;
    }

    public Relation visit(ExprList node, Pattern pattern) {

        return null;
    }

    public Relation visit(FieldAcc node, Pattern pattern) {

        return null;
    }

    public Relation visit(FloatLiteral node, Pattern pattern) {

        return null;
    }

    public Relation visit(InfixExpr node, Pattern pattern) {

        return null;
    }

    public Relation visit(InfixOperator node, Pattern pattern) {

        return null;
    }

    public Relation visit(InstanceofExpr node, Pattern pattern) {

        return null;
    }

    public Relation visit(IntLiteral node, Pattern pattern) {

        return null;
    }

    public Relation visit(Label node, Pattern pattern) {

        return null;
    }

    public Relation visit(LambdaExpr node, Pattern pattern) {

        return null;
    }

    public Relation visit(LongLiteral node, Pattern pattern) {

        return null;
    }

    public Relation visit(MethodInv node, Pattern pattern) {

        return null;
    }

    public Relation visit(MethodRef node, Pattern pattern) {

        return null;
    }

    public Relation visit(MType node, Pattern pattern) {

        return null;
    }

    public Relation visit(NillLiteral node, Pattern pattern) {

        return null;
    }

    public Relation visit(NumLiteral node, Pattern pattern) {

        return null;
    }

    public Relation visit(Operator node, Pattern pattern) {

        return null;
    }

    public Relation visit(ParenthesiszedExpr node, Pattern pattern) {

        return null;
    }

    public Relation visit(PostfixExpr node, Pattern pattern) {

        return null;
    }

    public Relation visit(PostOperator node, Pattern pattern) {

        return null;
    }

    public Relation visit(PrefixExpr node, Pattern pattern) {

        return null;
    }

    public Relation visit(PrefixOperator node, Pattern pattern) {

        return null;
    }

    public Relation visit(QName node, Pattern pattern) {

        return null;
    }

    public Relation visit(SName node, Pattern pattern) {

        return null;
    }

    public Relation visit(StrLiteral node, Pattern pattern) {

        return null;
    }

    public Relation visit(SuperFieldAcc node, Pattern pattern) {

        return null;
    }

    public Relation visit(SuperMethodInv node, Pattern pattern) {

        return null;
    }

    public Relation visit(SuperMethodRef node, Pattern pattern) {

        return null;
    }

    public Relation visit(Svd node, Pattern pattern) {

        return null;
    }

    public Relation visit(ThisExpr node, Pattern pattern) {

        return null;
    }

    public Relation visit(TyLiteral node, Pattern pattern) {

        return null;
    }

    public Relation visit(TypeMethodRef node, Pattern pattern) {

        return null;
    }

    public Relation visit(VarDeclarationExpr node, Pattern pattern) {

        return null;
    }

    public Relation visit(Vdf node, Pattern pattern) {

        return null;
    }

    public Relation process(Node node, Pattern pattern) {
        switch (node.getNodeType()) {
            case METHDECL:
                return visit((MethDecl) node, pattern);
            case ARRACC:
                return visit((AryAcc) node, pattern);
            case ARRCREAT:
                return visit((AryCreation) node, pattern);
            case ARRINIT:
                return visit((AryInitializer) node, pattern);
            case ASSIGN:
                return visit((Assign) node, pattern);
            case BLITERAL:
                return visit((BoolLiteral) node, pattern);
            case CAST:
                return visit((CastExpr) node, pattern);
            case CLITERAL:
                return visit((CharLiteral) node, pattern);
            case CLASSCREATION:
                return visit((ClassInstCreation) node, pattern);
            case COMMENT:
                return visit((Comment) node, pattern);
            case CONDEXPR:
                return visit((ConditionalExpr) node, pattern);
            case DLITERAL:
                return visit((DoubleLiteral) node, pattern);
            case FIELDACC:
                return visit((FieldAcc) node, pattern);
            case FLITERAL:
                return visit((FloatLiteral) node, pattern);
            case INFIXEXPR:
                return visit((InfixExpr) node, pattern);
            case INSTANCEOF:
                return visit((InstanceofExpr) node, pattern);
            case INTLITERAL:
                return visit((IntLiteral) node, pattern);
            case LABEL:
                return visit((Label) node, pattern);
            case LLITERAL:
                return visit((LongLiteral) node, pattern);
            case MINVOCATION:
                return visit((MethodInv) node, pattern);
            case NULL:
                return visit((NillLiteral) node, pattern);
            case NUMBER:
                return visit((NumLiteral) node, pattern);
            case PARENTHESISZED:
                return visit((ParenthesiszedExpr) node, pattern);
            case POSTEXPR:
                return visit((PostfixExpr) node, pattern);
            case PREEXPR:
                return visit((PrefixExpr) node, pattern);
            case QNAME:
                return visit((QName) node, pattern);
            case SNAME:
                return visit((SName) node, pattern);
            case SLITERAL:
                return visit((StrLiteral) node, pattern);
            case SFIELDACC:
                return visit((SuperFieldAcc) node, pattern);
            case SMINVOCATION:
                return visit((SuperMethodInv) node, pattern);
            case SINGLEVARDECL:
                return visit((Svd) node, pattern);
            case THIS:
                return visit((ThisExpr) node, pattern);
            case TLITERAL:
                return visit((TyLiteral) node, pattern);
            case VARDECLEXPR:
                return visit((VarDeclarationExpr) node, pattern);
            case VARDECLFRAG:
                return visit((Vdf) node, pattern);
            case ANONYMOUSCDECL:
                return visit((AnonymousClassDecl) node, pattern);
            case ASSERT:
                return visit((AssertStmt) node, pattern);
            case BLOCK:
                return visit((Blk) node, pattern);
            case BREACK:
                return visit((BreakStmt) node, pattern);
            case CONSTRUCTORINV:
                return visit((ConstructorInv) node, pattern);
            case CONTINUE:
                return visit((ContinueStmt) node, pattern);
            case DO:
                return visit((DoStmt) node, pattern);
            case EFOR:
                return visit((EnhancedForStmt) node, pattern);
            case FOR:
                return visit((ForStmt) node, pattern);
            case IF:
                return visit((IfStmt) node, pattern);
            case RETURN:
                return visit((ReturnStmt) node, pattern);
            case SCONSTRUCTORINV:
                return visit((SuperConstructorInv) node, pattern);
            case SWCASE:
                return visit((SwCase) node, pattern);
            case SWSTMT:
                return visit((SwitchStmt) node, pattern);
            case SYNC:
                return visit((SynchronizedStmt) node, pattern);
            case THROW:
                return visit((ThrowStmt) node, pattern);
            case TRY:
                return visit((TryStmt) node, pattern);
            case CATCHCLAUSE:
                return visit((CatClause) node, pattern);
            case TYPEDECL:
                return visit((TypeDeclarationStmt) node, pattern);
            case VARDECLSTMT:
                return visit((VarDeclarationStmt) node, pattern);
            case WHILE:
                return visit((WhileStmt) node, pattern);
            case POSTOPERATOR:
                return visit((PostOperator) node, pattern);
            case INFIXOPERATOR:
                return visit((InfixOperator) node, pattern);
            case PREFIXOPERATOR:
                return visit((PrefixOperator) node, pattern);
            case ASSIGNOPERATOR:
                return visit((AssignOperator) node, pattern);
            case TYPE:
                return visit((MType) node, pattern);
            case UNKNOWN:
                LevelLogger.warn("Found an unknown node type ! ");
            default:
                LevelLogger.fatal("Cannot parse node type ! ");
        }
        return null;
    }

}
