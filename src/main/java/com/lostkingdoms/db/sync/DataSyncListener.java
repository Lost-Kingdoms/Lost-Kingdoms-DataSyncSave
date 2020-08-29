package com.lostkingdoms.db.sync;

import com.lostkingdoms.db.organization.DataOrganizationManager;

import redis.clients.jedis.JedisPubSub;

public class DataSyncListener extends JedisPubSub {

	@Override
	public void onMessage(String channel, String message) {
		if(channel.equals(DataOrganizationManager.SYNC_MESSAGE_CHANNEL)) {
			DataSyncMessage syncMessage = DataSyncMessage.deserialize(message.getBytes());
			
			if(!syncMessage.getSenderInstanceID().equals(DataOrganizationManager.getInstance().getInstanceID()))
				DataOrganizationManager.getInstance().invalidateHashSlot(syncMessage.getHashSlot());
		}
	}
	
}
