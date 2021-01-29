package com.lostkingdoms.db.sync;

import java.util.UUID;

/**
 * The representation of a message to update a hashslot
 *
 * @author Tim Küchler (https://github.com/TimK1998)
 *
 */
public final class DataSyncMessage {

	/** Unique ID to identify the cache instance that initiated the data sync */
	private final UUID senderInstanceID;
	
	/** Represents the hashslot of the key that is to be updated */
	private final int keyHashSlot;



	/**
	 * Constructor.
	 * Creates a new {@link DataSyncMessage}
	 * 
	 * @param senderInstanceID the id of the cache instance
	 * @param keyHashSlot the hashslot of the key that will be updated
	 */
	public DataSyncMessage(UUID senderInstanceID, int keyHashSlot) {
		this.senderInstanceID = senderInstanceID;
		this.keyHashSlot = keyHashSlot;
	}
	
	/**
	 * Get the sender instance ID
	 * 
	 * @return the instance of the sender
	 */
	public UUID getSenderInstanceID() {
		return this.senderInstanceID;
	}
	
	/**
	 * Get the updated hashslot
	 * 
	 * @return the updated hashslot
	 */
	public int getHashSlot() {
		return this.keyHashSlot;
	}

	/**
	 * Serializes this {@link DataSyncMessage} to an sendable string
	 *
	 * @return the serialized string
	 */
	public String serialize() {
		//Serialize uuid of sender
		String senderInstanceID = this.senderInstanceID.toString();

		//Serialize hashslot
		String keyHashSlot = String.valueOf(this.keyHashSlot);

		return senderInstanceID + ":" + keyHashSlot;
	}

	/**
	 * Deserializes an string to {@link DataSyncMessage}
	 *
	 * @param message the message to deserialize
	 * @return the created {@link DataSyncMessage}
	 */
	public static DataSyncMessage deserialize(String message) {
		String[] split = message.split(":");

		// Construct uuid of sender
		UUID senderInstanceID = UUID.fromString(split[0]);

		// Construct updated hashslot
		int keyHashSlot = Integer.parseInt(split[1]);

		return new DataSyncMessage(senderInstanceID, keyHashSlot);
	}

}
