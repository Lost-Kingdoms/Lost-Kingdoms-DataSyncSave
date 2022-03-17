package com.lostkingdoms.db.sync;

import com.lostkingdoms.db.DataOrganizationManager;

import redis.clients.jedis.JedisPubSub;

/**
 * The listener which listens for synchronize messages
 * 
 * @author Tim Küchler (https://github.com/TimK1998)
 *
 */
public final class DataSyncListener extends JedisPubSub {

	@Override
	public void onMessage(String channel, String message) {
		//If channel is the sync message channel
		if(channel.equals(DataOrganizationManager.syncMessageChannel)) {
			DataSyncMessage syncMessage = DataSyncMessage.deserialize(message);

			//If message is not self sent
			if(!syncMessage.getSenderInstanceID().equals(DataOrganizationManager.getInstance().getInstanceID())) {
				//Invalidate hashslot
				DataOrganizationManager.getInstance().invalidateHashSlot(syncMessage.getHashSlot());
			}
		}
	}
	
}
