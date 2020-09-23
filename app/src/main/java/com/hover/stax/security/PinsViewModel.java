package com.hover.stax.security;

import android.app.Application;
import android.content.Context;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.hover.stax.channels.Channel;
import com.hover.stax.database.DatabaseRepo;
import com.hover.stax.database.KeyStoreExecutor;

import java.util.ArrayList;
import java.util.List;

public class PinsViewModel extends AndroidViewModel {

	private DatabaseRepo repo;
	private LiveData<List<Channel>> channels;

	public PinsViewModel(Application application) {
		super(application);
		repo = new DatabaseRepo(application);
		loadSelectedChannels();
	}

	public LiveData<List<Channel>> getSelectedChannels() {
		return channels;
	}

	private void loadSelectedChannels() {
		if (channels == null) {
			channels = new MutableLiveData<>();
		}
		channels = repo.getSelected();
	}

	void setPin(int id, String pin) {
		List<Channel> allChannels = channels.getValue() != null ? channels.getValue() : new ArrayList<>();
		for (Channel channel : allChannels) {
			if (channel.id == id) {
				channel.pin = pin;
			}
		}
	}

	void savePins(Context c) {
		List<Channel> selectedChannels = channels.getValue() != null ? channels.getValue() : new ArrayList<>();
		for (Channel channel : selectedChannels) {
			if (channel.pin != null && !channel.pin.isEmpty()) {
				channel.pin = KeyStoreExecutor.createNewKey(channel.pin, c);
				repo.update(channel);
			}
		}
	}

	public void clearAllPins() {
		List<Channel> selectedChannels = channels.getValue() != null ? channels.getValue() : new ArrayList<>();
		for (Channel channel : selectedChannels) {
			channel.pin = null;
			repo.update(channel);
		}
	}

	public void setDefaultAccount(Channel channel) {
		repo.update(channel);
	}
}