/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse.match.metric;

/**
 * 
 * @author Jiajun
 * @date Oct 15, 2017
 */
public class FVector {
	
	public static final int INDEX_STRUCT_FOR = 0;
	public static final int INDEX_STRUCT_ENFOR = 1;
	public static final int INDEX_STRUCT_WHILE = 2;
	public static final int INDEX_STRUCT_DO = 3;
	public static final int INDEX_STRUCT_IF = 4; 
	public static final int INDEX_STRUCT_SWC = 5;
	public static final int INDEX_STRUCT_COND = 6;
	public static final int INDEX_STRUCT_TRY = 7;
	public static final int INDEX_STRUCT_SYC = 8;
	public static final int INDEX_STRUCT_OTHER = 9;
	
	public static final int INDEX_OP_ARITH = 10;
	public static final int INDEX_OP_BIT = 11;
	public static final int INDEX_OP_COMP = 12;
	public static final int INDEX_OP_UNARY = 13;
	public static final int INDEX_OP_LOGIC = 14;
	
	public static final int INDEX_OP_ACC = 15;
	public static final int INDEX_OP_ASSIGN = 16;
	public static final int INDEX_MCALL = 17;
	public static final int INDEX_VAR = 18;
	public static final int INDEX_LITERAL = 19;
	public static final int VECTOR_LEN = 20;
	
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
		case "*":
		case "/":
		case "+":
		case "-":
		case "%":
			return INDEX_OP_ARITH;
		case "<<":
		case ">>":
		case ">>>":
		case "^":
		case "&":
		case "|":
			return INDEX_OP_BIT;
		case "<":
		case ">":
		case "<=":
		case ">=":
		case "==":
		case "!=":
		case "instanceof" :
			return INDEX_OP_COMP;
		case "&&":
		case "||":
			return INDEX_OP_LOGIC;
		case "++":
		case "--":
		case "~":
		case "!":
			return INDEX_OP_UNARY;
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
		delta = Math.sqrt(delta / biggest);
		return delta;
	}
	
	@Override
	public String toString() {
		StringBuffer stringBuffer = new StringBuffer("[");
		stringBuffer.append(_vector[0]);
		for(int i = 1; i < VECTOR_LEN; i++){
			stringBuffer.append(", " + _vector[i]);
		}
		stringBuffer.append("]");
		return stringBuffer.toString();
	}
	
}
