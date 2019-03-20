/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.match.metric;

import java.io.Serializable;

/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class FVector implements Serializable {


    //Arithmetic
    public static final int ARITH_PLUS = 0;
    public static final int ARITH_MINUS = 1;
    public static final int ARITH_PLUS_2 = 2;
    public static final int ARITH_MINUS_2 = 3;
    public static final int ARITH_TIMES = 4;
    public static final int ARITH_DIV = 5;
    public static final int ARITH_MOD = 6;
    public static final int ARITH_ASSIGN = 7;
    //Logical
    public static final int LOGI_GT = 8;
    public static final int LOGI_GE = 9;
    public static final int LOGI_LT = 10;
    public static final int LOGI_LE= 11;
    public static final int LOGI_EQ = 12;
    public static final int LOGI_NEQ = 13;
    public static final int LOGI_NOT = 14;
    public static final int LOGI_INSOF = 15;
    public static final int LOGI_AND = 16;
    public static final int LOGI_OR = 17;
    //Bit
    public static final int BIT_AND = 18;
    public static final int BIT_OR = 19;
    public static final int BIT_XOR = 20;
    public static final int BIT_NOT = 21;
    public static final int BIT_SHIFT_L = 22;
    public static final int BIT_SHIFT_R = 23;
    public static final int BIT_SHIFT_RR = 24;
    //bracket
    public static final int BRAKET_SQL = 25;
    public static final int BRAKET_SQR = 26;
    //keyword
    public static final int KEY_THIS = 27;
    public static final int KEY_SUPER = 28;
    public static final int KEY_CAST = 29;
    public static final int KEY_NEW = 30;
    public static final int KEY_ASSERT = 31;
    public static final int KEY_BREAK = 32;
    public static final int KEY_CATCH = 33;
    public static final int KEY_CONTINUE = 34;
    public static final int KEY_DO = 35;
    public static final int KEY_WHILE = 36;
    public static final int KEY_ENFOR = 37;
    public static final int KEY_FOR = 38;
    public static final int KEY_IF = 39;
    public static final int KEY_ELSE = 40;
    public static final int KEY_RET = 41;
    public static final int KEY_SWITCH = 42;
    public static final int KEY_CASE = 43;
    public static final int KEY_SYNC = 44;
    public static final int KEY_THROW = 45;
    public static final int KEY_TRY = 46;
    //node type
    public static final int E_AACC = 47;
    public static final int E_ACREAR = 48;
    public static final int E_AINIT = 49;
    public static final int E_NUMBER = 50;
    public static final int E_CHAR = 51;
    public static final int E_BOOL = 52;
    public static final int E_STR = 53;
    public static final int E_NULL = 54;
    public static final int E_ASSIGN = 55;
    public static final int E_COND = 56;
    public static final int E_FACC = 57;
    public static final int E_MINV = 58;
    public static final int E_CLASS = 59;
    public static final int E_TYPE = 60;
    public static final int E_POSTFIX = 61;
    public static final int E_PREFIX = 62;
    public static final int E_VAR = 63;
    public static final int E_ANONY = 64;
    public static final int E_EXPLIST = 65;
    public static final int E_VARDEF = 66;
    public static final int OTHER = 67;
	public static final int VECTOR_LEN = 68;
	private static final long serialVersionUID = -2160573999272597201L;

	private int[] _vector = new int[VECTOR_LEN];
	
	public FVector() {
		for(int i = 0; i < VECTOR_LEN; i++){
			_vector[i] = 0;
		}
	}
	
	public void inc(int index){
		_vector[index] ++;
	}
	
	public void inc(String operator) {
		int index = parseOperator(operator);
		if(index != -1){
			_vector[index] ++;
		}
	}
	
	@Deprecated
	public void setFeature(int number, int index){
		_vector[index] = number;
	}
	
	@Deprecated
	public void setFeature(int number, String operator){
		int index = parseOperator(operator);
		if(index != -1){
			_vector[index] = number;
		}
	}
	
	public void combineFeature(FVector fVector){
		for(int i = 0; i < VECTOR_LEN; i++){
			_vector[i] += fVector._vector[i];
		}
	}
	
	private int parseOperator(String op){
		switch(op){
		case "*": return ARITH_TIMES;
		case "/": return ARITH_DIV;
		case "+": return ARITH_PLUS;
		case "-": return ARITH_MINUS;
		case "%": return ARITH_MOD;
		case "<<": return BIT_SHIFT_L;
		case ">>": return BIT_SHIFT_R;
		case ">>>": return BIT_SHIFT_RR;
		case "^": return BIT_XOR;
		case "&": return BIT_AND;
		case "|": return BIT_OR;
		case "<": return LOGI_LT;
		case ">": return LOGI_GT;
		case "<=": return LOGI_LE;
		case ">=": return LOGI_GE;
		case "==": return LOGI_EQ;
		case "!=": return LOGI_NEQ;
		case "instanceof" : return LOGI_INSOF;
		case "&&": return LOGI_AND;
		case "||": return LOGI_OR;
		case "++": return ARITH_PLUS_2;
		case "--": return ARITH_MINUS_2;
		case "~": return BIT_NOT;
		case "!": return LOGI_NOT;
		}
		return -1;
	}
	

	/**************************compute*********************************/
	
	public enum ALGO{
		NORM_1,
		NORM_2,
		COSINE
	}
	
	public double computeSimilarity(FVector fVector, ALGO alg){
		if(fVector == null){
            switch (alg) {
                case NORM_1:
                case NORM_2:
                    return 1.0;
                case COSINE:
                default:
            }
		    return 0.0;
		} else {
			switch (alg) {
			case NORM_1:
				return norm_1(fVector);
			case NORM_2:
				return norm_2(fVector);
			case COSINE:
				return cosine(fVector);
			default:
				break;
			}
		}
		return 0.0;
	}
	
	private double cosine(FVector fVector){
		double product = 0.0;
		double squareAsum = 0.0;
		double squareBsum = 0.0;
		for(int i = 0; i < VECTOR_LEN; i++){
			product += _vector[i] * fVector._vector[i];
			squareAsum += _vector[i] * _vector[i];
			squareBsum += fVector._vector[i] * fVector._vector[i];
		}
		double denorminator = Math.sqrt(squareAsum) * Math.sqrt(squareBsum);
		if(denorminator == 0){
			return 0.0;
		}
		// positive value 0 - 1: 0 means exactly non-similar, 1 means most-similar
		double delta = product / denorminator;
		return delta;
	}
	
	//Manhattan Distance
	private double norm_1(FVector fVector){
		double delta = 0.0;
		double biggest = 0.0;
		for(int i = 0; i < VECTOR_LEN; i ++){
			delta += Math.abs(_vector[i] - fVector._vector[i]);
			biggest += _vector[i] > fVector._vector[i] ? _vector[i] : fVector._vector[i];
		}
		if(biggest == 0){
			return 0.0;
		}
		// the smaller the better
		return delta / biggest;
	}
	
	private double norm_2(FVector fVector) {
		double delta = 0.0;
		double biggest = 0.0;
		for(int i = 0; i < VECTOR_LEN; i ++){
			delta += Math.pow(_vector[i] - fVector._vector[i], 2.0);
			double big = _vector[i] > fVector._vector[i] ? _vector[i] : fVector._vector[i];
			biggest += big * big;
		}
		if(biggest == 0){
			return 0.0;
		}
		// the smaller the better
		delta = Math.sqrt(delta / biggest);
		return delta;
	}


	public static FVector from(String string) {
		FVector fVector = new FVector();
		if (string == null) return fVector;
		if (string.startsWith("[") && string.endsWith("]")) {
			String[] src = string.split(",");
			if (src.length == VECTOR_LEN) {
				try {
					for (int i = 0; i < src.length; i++) {
						fVector._vector[i] = Integer.parseInt(src[i]);
					}
				} catch (Exception e) {}
			}
		}
		return fVector;
	}

	@Override
	public String toString() {
		StringBuffer stringBuffer = new StringBuffer("[");
		stringBuffer.append(_vector[0]);
		for(int i = 1; i < VECTOR_LEN; i++){
			stringBuffer.append("," + _vector[i]);
		}
		stringBuffer.append("]");
		return stringBuffer.toString();
	}
	
}
