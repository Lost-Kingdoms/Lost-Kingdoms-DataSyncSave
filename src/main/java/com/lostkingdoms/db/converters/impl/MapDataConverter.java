package com.lostkingdoms.db.converters.impl;

import java.util.HashMap;

import com.lostkingdoms.db.converters.IDataConverter;

public class MapDataConverter<K, V> implements IDataConverter<HashMap<K, V>> {

	@Override
	public String convertToRedis(HashMap<K, V> data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HashMap<K, V> convertFromRedis(String s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String convertToMongoDB(HashMap<K, V> data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HashMap<K, V> convertFromMongoDB(String s) {
		// TODO Auto-generated method stub
		return null;
	}

}
