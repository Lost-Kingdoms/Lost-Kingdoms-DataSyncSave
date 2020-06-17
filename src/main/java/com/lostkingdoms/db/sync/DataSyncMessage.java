package com.lostkingdoms.db.sync;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * The representation of a message to update a hashslot  
 */
public class DataSyncMessage {

	/**
	 * Unique ID to identify the cache instance that initiated the data sync
	 */
	private UUID senderInstanceID;
	
	/**
	 * Represents the hashslot of the key that is to be updated
	 */
	private short keyHashSlot;
	
	/**
	 * Constructor.
	 * Creates a new {@link DataSyncMessage}
	 * 
	 * @param senderInstanceID the id of the cache instance
	 * @param keyHashSlot the hashslot of the key that will be updated
	 */
	public DataSyncMessage(UUID senderInstanceID, short keyHashSlot) {
		this.senderInstanceID = senderInstanceID;
		this.keyHashSlot = keyHashSlot;
	}
	
	/**
	 * Serializes this {@linkDataSyncMessage} to an sendable list of bytes
	 * 
	 * @return
	 */
	public byte[] serialize() {
		// Convert the senderInstanceID to byte array
		ByteBuffer bb = ByteBuffer.wrap(new byte[18]);
        bb.putLong(senderInstanceID.getMostSignificantBits());
        bb.putLong(senderInstanceID.getLeastSignificantBits());
        
        byte[] messageBytes = bb.array();
        
		// Final two bytes are the key CRC in big-endian format
		messageBytes[16] = (byte)(keyHashSlot >>> 8);
		messageBytes[17] = (byte)(keyHashSlot & 0x00FF);

		return messageBytes;
	}
	
	/**
	 * Deserializes an array of bytes to {@link DataSyncMessage}
	 * 
	 * @param messageBytes
	 * @return
	 */
	public static DataSyncMessage deserialize(byte[] messageBytes) {
		if(messageBytes == null) throw new IllegalArgumentException("messageBytes is null");
		if(messageBytes.length != 18) throw new IllegalArgumentException("Invalid message length");
		
		// Construct uuid of sender
		ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
        Long high = byteBuffer.getLong();
        Long low = byteBuffer.getLong();
        UUID senderInstanceID = new UUID(high, low);
		
		// Construct updated hashslot
		short keyHashSlot = (short)((((short) (messageBytes[16] & 0xff)) << 8) + messageBytes[17]);
		
		return new DataSyncMessage(senderInstanceID, keyHashSlot);
	}
	
}
