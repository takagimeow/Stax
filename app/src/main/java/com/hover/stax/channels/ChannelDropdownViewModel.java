package com.hover.stax.channels;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessaging;
import com.hover.stax.actions.Action;
import com.hover.stax.database.DatabaseRepo;
import com.hover.stax.languages.SelectLanguageActivity;
import com.hover.stax.sims.Sim;
import com.hover.stax.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.hover.stax.database.Constants.LANGUAGE_CHECK;

public class ChannelDropdownViewModel extends AndroidViewModel {
	public final static String TAG = "ChannelDropdownVM";

	private DatabaseRepo repo;
	private MutableLiveData<String> type = new MutableLiveData<>();

	private MutableLiveData<List<Sim>> sims;
	private LiveData<List<String>> simHniList = new MutableLiveData<>();

	private LiveData<List<Channel>> allChannels;
	private LiveData<List<Channel>> selectedChannels;
	private MediatorLiveData<List<Channel>> simChannels;
	private MediatorLiveData<Channel> activeChannel = new MediatorLiveData<>();
	private LiveData<List<Action>> actions = new MediatorLiveData<>();

	public ChannelDropdownViewModel(Application application) {
		super(application);
		repo = new DatabaseRepo(application);
		type.setValue(Action.BALANCE);

		loadChannels();
		loadSims();

		simHniList = Transformations.map(sims, this::getHnisAndSubscribeToEachOnFirebase);

		simChannels = new MediatorLiveData<>();
		simChannels.addSource(allChannels, this::onChannelsUpdateHnis);
		simChannels.addSource(simHniList, this::onSimUpdate);

		activeChannel.addSource(selectedChannels, this::setActiveChannelIfNull);

		actions = Transformations.switchMap(type, this::loadActions);
		actions = Transformations.switchMap(selectedChannels, this::loadActions);
		actions = Transformations.switchMap(activeChannel, this::loadActions);
	}

	private void setType(String t) { type.setValue(t); }

	private void loadChannels() {
		if (allChannels == null) { allChannels = new MutableLiveData<>(); }
		if (selectedChannels == null) { selectedChannels = new MutableLiveData<>(); }
		allChannels = repo.getAllChannels();
		selectedChannels = repo.getSelected();
	}

	public LiveData<List<Channel>> getChannels() {
		if (allChannels == null) {
			allChannels = new MutableLiveData<>();
		}
		return allChannels;
	}

	public LiveData<List<Channel>> getSelectedChannels() {
		if (selectedChannels == null) { selectedChannels = new MutableLiveData<>(); }
		return selectedChannels;
	}

	void loadSims() {
		if (sims == null) {
			sims = new MutableLiveData<>();
		}
		new Thread(() -> sims.postValue(repo.getSims())).start();
		LocalBroadcastManager.getInstance(getApplication())
				.registerReceiver(simReceiver, new IntentFilter(Utils.getPackage(getApplication()) + ".NEW_SIM_INFO_ACTION"));
	}

	private final BroadcastReceiver simReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			new Thread(() -> sims.postValue(repo.getSims())).start();
		}
	};

	private List<String> getHnisAndSubscribeToEachOnFirebase(List<Sim> sims) {
		if (sims == null) return null;
		List<String> hniList = new ArrayList<>();
		for (Sim sim : sims) {
			if (!hniList.contains(sim.hni)) {
				FirebaseMessaging.getInstance().subscribeToTopic("sim-" + sim.hni);
				FirebaseMessaging.getInstance().subscribeToTopic(sim.country_iso);
				hniList.add(sim.hni);
			}
		}
		return hniList;
	}

	private void onChannelsUpdateHnis(List<Channel> channels) {
		updateSimChannels(simChannels, channels, simHniList.getValue());
	}

	private void onSimUpdate(List<String> hniList) {
		updateSimChannels(simChannels, allChannels.getValue(), hniList);
	}

	public void updateSimChannels(MediatorLiveData<List<Channel>> simChannels, List<Channel> channels, List<String> hniList) {
		if (channels == null || hniList == null) return;
		List<Channel> simChannelList = new ArrayList<>();
		for (int i = 0; i < channels.size(); i++) {
			String[] hniArr = channels.get(i).hniList.split(",");
			for (String s : hniArr) {
				if (hniList.contains(Utils.stripHniString(s))) {
					if (!simChannelList.contains(channels.get(i)))
						simChannelList.add(channels.get(i));
				}
			}
		}
		simChannels.setValue(simChannelList);
	}

	public LiveData<List<Channel>> getSimChannels() {
		return simChannels;
	}

	protected void setActiveChannelIfNull(List<Channel> channels) {
		if (channels != null && channels.size() > 0 && activeChannel.getValue() == null) {
			for (Channel c: channels)
				if (c.defaultAccount) { activeChannel.setValue(c); }
		}
	}

	public void setActiveChannel(int channel_id) {
//		activeChannel.setValue(getChannelById(channel_id));
	}
	public void setActiveChannel(Channel channel) {
		activeChannel.setValue(channel);
	}
	public LiveData<Channel> getActiveChannel() { return activeChannel; }

	public LiveData<List<Action>> loadActions(String t) {
		if ((t.equals(Action.BALANCE) && selectedChannels.getValue() == null) || (!t.equals(Action.BALANCE) && activeChannel.getValue() == null)) return null;
		if (t.equals(Action.BALANCE))
			return loadActions(selectedChannels.getValue(), t);
		else
			return loadActions(activeChannel.getValue(), t);
	}

	public LiveData<List<Action>> loadActions(Channel channel) {
		return repo.getLiveActions(channel.id, type.getValue());
	}

	private LiveData<List<Action>> loadActions(Channel c, String t) {
		return repo.getLiveActions(c.id, t);
	}

	public LiveData<List<Action>> loadActions(List<Channel> channels) {
		if (type.getValue().equals(Action.BALANCE))
			return loadActions(channels, type.getValue());
		else return actions;
	}

	public LiveData<List<Action>> loadActions(List<Channel> channels, String t) {
		if (type.getValue() == null || !type.getValue().equals(Action.BALANCE)) return actions;
		Log.e(TAG, "attempting to load " + channels.size() + " balance actions");
		int[] ids = new int[channels.size()];
		for (int c = 0; c < channels.size(); c++)
			ids[c] = channels.get(c).id;
		Log.e(TAG, "attempting to load balance actions for channels: " + Arrays.toString(ids));
		return repo.getLiveActions(ids, t);
	}

	public LiveData<List<Action>> getActions() { return actions; }

	public void selectChannel(Channel channel, Context c) {
		if (channel == null) return;
		Log.e(TAG, "saving selected channel: " + channel);
//		logChoice(channel, c);
		channel.selected = true;
		channel.defaultAccount = selectedChannels.getValue() == null || selectedChannels.getValue().size() == 0;
		FirebaseMessaging.getInstance().subscribeToTopic("channel-" + channel.id);
		repo.update(channel);
	}

	@Override
	protected void onCleared() {
		try {
			LocalBroadcastManager.getInstance(getApplication()).unregisterReceiver(simReceiver);
		} catch (Exception ignored) {
		}
		super.onCleared();
	}
}
