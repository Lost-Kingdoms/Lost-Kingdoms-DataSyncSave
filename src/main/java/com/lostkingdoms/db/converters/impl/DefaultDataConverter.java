package com.lostkingdoms.db.converters.impl;

import java.lang.reflect.Field;
import java.util.UUID;

import com.lostkingdoms.db.converters.IDataConverter;
import com.lostkingdoms.db.organization.enums.Identifier;
import com.lostkingdoms.db.organization.enums.OrganizedEntity;

public class DefaultDataConverter implements IDataConverter {

	@Override
	public String convertToRedis(Object data) {
		Class<?> clazz = data.getClass();
		
		if(clazz == int.class) {
			return String.valueOf((int)data);
		}
		if(clazz == Integer.class) {
			return ((Integer)data).toString();
		}
		if(clazz == double.class) {
			return String.valueOf((double)data);
		}
		if(clazz == Double.class) {
			return ((Double)data).toString();
		}
		if(clazz == long.class) {
			return String.valueOf((long)data);
		}
		if(clazz == Long.class) {
			return ((Long)data).toString();
		}
		if(clazz == short.class) {
			return String.valueOf((short)data);
		}
		if(clazz == Short.class) {
			return ((Short)data).toString();
		}
		if(clazz == byte.class) {
			return String.valueOf((byte)data);
		}
		if(clazz == Byte.class) {
			return ((Byte)data).toString();
		}
		if(clazz == boolean.class) {
			return String.valueOf((boolean)data);
		}
		if(clazz == Boolean.class) {
			return ((Boolean)data).toString();
		}
		if(clazz == float.class) {
			return String.valueOf((float)data);
		}
		if(clazz == Float.class) {
			return ((Float)data).toString();
		}
		if(clazz == char.class) {
			return String.valueOf((char)data);
		}
		if(clazz == Character.class) {
			return ((Character)data).toString();
		}
		if(clazz == String.class) {
			return (String) data;
		}
		if(clazz.getAnnotation(OrganizedEntity.class) != null) {
			for(Field f : data.getClass().getDeclaredFields()) {
				if(f.getAnnotation(Identifier.class) != null) {
					f.setAccessible(true);
					UUID uuid = null;
					try {
						uuid = ((UUID)f.get(data));
					} catch (IllegalArgumentException | IllegalAccessException e) {
						e.printStackTrace();
					}
					f.setAccessible(false);
					return uuid.toString();
				}
			}
		}
		
		return null;
	}

	@Override
	public Object convertFromRedis(Class<?> clazz, String s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String convertToMongoDB(Object data) {
		Class<?> clazz = data.getClass();
		
		if(clazz == int.class) {
			return String.valueOf((int)data);
		}
		if(clazz == Integer.class) {
			return ((Integer)data).toString();
		}
		if(clazz == double.class) {
			return String.valueOf((double)data);
		}
		if(clazz == Double.class) {
			return ((Double)data).toString();
		}
		if(clazz == long.class) {
			return String.valueOf((long)data);
		}
		if(clazz == Long.class) {
			return ((Long)data).toString();
		}
		if(clazz == short.class) {
			return String.valueOf((short)data);
		}
		if(clazz == Short.class) {
			return ((Short)data).toString();
		}
		if(clazz == byte.class) {
			return String.valueOf((byte)data);
		}
		if(clazz == Byte.class) {
			return ((Byte)data).toString();
		}
		if(clazz == boolean.class) {
			return String.valueOf((boolean)data);
		}
		if(clazz == Boolean.class) {
			return ((Boolean)data).toString();
		}
		if(clazz == float.class) {
			return String.valueOf((float)data);
		}
		if(clazz == Float.class) {
			return ((Float)data).toString();
		}
		if(clazz == char.class) {
			return String.valueOf((char)data);
		}
		if(clazz == Character.class) {
			return ((Character)data).toString();
		}
		if(clazz == String.class) {
			return (String) data;
		}
		if(clazz.getAnnotation(OrganizedEntity.class) != null) {
			for(Field f : data.getClass().getDeclaredFields()) {
				if(f.getAnnotation(Identifier.class) != null) {
					f.setAccessible(true);
					UUID uuid = null;
					try {
						uuid = ((UUID)f.get(data));
					} catch (IllegalArgumentException | IllegalAccessException e) {
						e.printStackTrace();
					}
					f.setAccessible(false);
					return uuid.toString();
				}
			}
		}
		
		return null;
	}

	@Override
	public Object convertFromMongoDB(String s) {
		// TODO Auto-generated method stub
		return null;
	}

}
