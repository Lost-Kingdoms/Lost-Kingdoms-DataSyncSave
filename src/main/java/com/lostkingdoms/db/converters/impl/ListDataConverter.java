package com.lostkingdoms.db.converters.impl;

import java.util.ArrayList;

import com.lostkingdoms.db.converters.IDataConverter;

public class ListDataConverter<T> implements IDataConverter<ArrayList<T>> {

	@Override
	public String convertToRedis(ArrayList<T> data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<T> convertFromRedis(String s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String convertToMongoDB(ArrayList<T> data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<T> convertFromMongoDB(String s) {
		// TODO Auto-generated method stub
		return null;
	}

}
