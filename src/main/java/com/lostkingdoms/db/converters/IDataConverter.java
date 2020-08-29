package com.lostkingdoms.db.converters;

public interface IDataConverter<T> {

	public String convertToRedis(T data);
	
	public T convertFromRedis(String s);
	
	public String convertToMongoDB(T data);
	
	public T convertFromMongoDB(String s);
	
}
