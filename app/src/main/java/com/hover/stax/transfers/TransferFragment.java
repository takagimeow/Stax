package com.hover.stax.transfers;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.hover.stax.R;
import com.hover.stax.actions.Action;
import com.hover.stax.actions.ActionSelect;
import com.hover.stax.channels.ChannelDropdownViewModel;
import com.hover.stax.contacts.StaxContact;
import com.hover.stax.contacts.StaxContactArrayAdapter;
import com.hover.stax.database.Constants;
import com.hover.stax.utils.StagedFragment;
import com.hover.stax.utils.StagedViewModel;
import com.hover.stax.utils.UIHelper;
import com.hover.stax.utils.Utils;
import com.hover.stax.views.Stax2LineItem;

public class TransferFragment extends StagedFragment implements ActionSelect.HighlightListener {
	private static final String TAG = "TransferFragment";

	private TransferViewModel transferViewModel;

	private TextInputLayout recipientLabel, amountEntry;
	private EditText amountInput, noteInput;
	private ActionSelect actionSelect;
	private RadioGroup isSelfRadioGroup;
	private AutoCompleteTextView recipientAutocomplete;
	private ImageButton contactButton;

	private ExtendedFloatingActionButton fab;

	private TextView amountValue, noteValue;
	private Stax2LineItem recipientValue;

	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		channelDropdownViewModel = new ViewModelProvider(requireActivity()).get(ChannelDropdownViewModel.class);
		stagedViewModel = new ViewModelProvider(requireActivity()).get(TransferViewModel.class);
		transferViewModel = (TransferViewModel) stagedViewModel;

		View root = inflater.inflate(R.layout.fragment_transfer, container, false);
		init(root);
		startObservers(root);
		startListeners();
		return root;
	}

	@Override
	protected void init(View root) {
		editCard = root.findViewById(R.id.editCard);
		summaryCard = root.findViewById(R.id.summaryCard);
		setTitle(root);

		amountValue = root.findViewById(R.id.amountValue);
		recipientValue = root.findViewById(R.id.recipientValue);
		noteValue = root.findViewById(R.id.reasonValue);

		amountEntry = root.findViewById(R.id.amountEntry);
		amountInput = root.findViewById(R.id.amount_input);
		amountInput.setText(transferViewModel.getAmount().getValue());
		actionSelect = root.findViewById(R.id.action_select);
		isSelfRadioGroup = root.findViewById(R.id.isSelfRadioGroup);
		recipientLabel = root.findViewById(R.id.recipientLabel);
		recipientAutocomplete = root.findViewById(R.id.recipient_autocomplete);
		contactButton = root.findViewById(R.id.contact_button);
		noteInput = root.findViewById(R.id.note_input);
		noteInput.setText(transferViewModel.getNote().getValue());

		fab = root.findViewById(R.id.fab);
		root.findViewById(R.id.mainLayout).requestFocus();

		super.init(root);
	}

	private void setTitle(View root) {
		TextView formCardTitle = root.findViewById(R.id.editCard).findViewById(R.id.title);
		TextView summaryCardTitle = root.findViewById(R.id.summaryCard).findViewById(R.id.title);
		if (summaryCardTitle != null) { summaryCardTitle.setText(getString(transferViewModel.getType().equals(Action.AIRTIME) ? R.string.fab_airtime : R.string.fab_transfer)); }
		if (formCardTitle != null) { formCardTitle.setText(getString(transferViewModel.getType().equals(Action.AIRTIME) ? R.string.fab_airtime : R.string.fab_transfer)); }
	}

	protected void startObservers(View root) {
		transferViewModel.getActiveAction().observe(getViewLifecycleOwner(), this::setRecipientHint);

		channelDropdownViewModel.getActiveChannel().observe(getViewLifecycleOwner(), channel -> {
			actionSelect.setVisibility(channel == null ? View.GONE : View.VISIBLE);
		});

		channelDropdownViewModel.getActions().observe(getViewLifecycleOwner(), actions -> {
			actionSelect.setVisibility(actions == null || actions.size() <= 1 ? View.GONE : View.VISIBLE);
			if (actions == null || actions.size() == 0) return;
			transferViewModel.setActions(actions);
			actionSelect.updateActions(actions);
		});
		channelDropdownViewModel.getError().observe(getViewLifecycleOwner(), error -> channelDropdown.setError(error != null ? getString(error) : null));

		transferViewModel.getActiveActionError().observe(getViewLifecycleOwner(), error -> actionSelect.setError(error));

		transferViewModel.getAmount().observe(getViewLifecycleOwner(), amount -> amountValue.setText(Utils.formatAmount(amount)));
		transferViewModel.getAmountError().observe(getViewLifecycleOwner(), amountError -> {
			amountEntry.setError((amountError != null ? getString(amountError) : null));
		});

		transferViewModel.getRecentContacts().observe(getViewLifecycleOwner(), contacts -> {
			ArrayAdapter<StaxContact> adapter = new StaxContactArrayAdapter(requireActivity(), contacts);
			recipientAutocomplete.setAdapter(adapter);
			if (transferViewModel.getContact().getValue() != null)
				recipientAutocomplete.setText(transferViewModel.getContact().getValue().toString());
		});
		transferViewModel.getContact().observe(getViewLifecycleOwner(), contact -> {
			recipientValue.setContact(contact, transferViewModel.getRequest().getValue() != null && transferViewModel.getRequest().getValue().hasRequesterInfo());
		});

		transferViewModel.getRecipientError().observe(getViewLifecycleOwner(), recipientError -> {
			recipientLabel.setError((recipientError != null ? getString(recipientError) : null));
			recipientLabel.setErrorIconDrawable(0);
		});

		transferViewModel.getNote().observe(getViewLifecycleOwner(), reason -> noteValue.setText(reason));
	}

	protected void startListeners() {
		actionSelect.setListener(this);

		recipientAutocomplete.setOnItemClickListener((adapterView, view, pos, id) -> {
			StaxContact contact = (StaxContact) adapterView.getItemAtPosition(pos);
			transferViewModel.setContact(contact);
		});

		amountInput.addTextChangedListener(amountWatcher);
		recipientAutocomplete.addTextChangedListener(recipientWatcher);
		contactButton.setOnClickListener(view -> contactPicker(Constants.GET_CONTACT, view.getContext()));
		noteInput.addTextChangedListener(noteWatcher);

		fab.setOnClickListener(v -> fabClicked());
	}

	@Override
	public void highlightAction(Action a) { transferViewModel.setActiveAction(a); }

	private void fabClicked() {
		if (transferViewModel.getIsEditing().getValue()) {
			if (!channelDropdownViewModel.validates() || !transferViewModel.validates()) {
				UIHelper.flashMessage(getContext(), getString(R.string.toast_pleasefix));
				return;
			}
			transferViewModel.setEditing(false);
		} else
			((TransferActivity) getActivity()).submit();
	}

	private void setRecipientHint(Action action) {
		Log.e(TAG, "update hint to " + action + ":" + action.getPronoun(getContext()));
		Log.e(TAG, "requires recipient? " + action.requiresRecipient());
		editCard.findViewById(R.id.recipient_entry).setVisibility(action.requiresRecipient() ? View.VISIBLE : View.GONE);
		summaryCard.findViewById(R.id.recipientRow).setVisibility(action.requiresRecipient() ? View.VISIBLE : View.GONE);
		if (!action.requiresRecipient())
			recipientValue.setContent(getString(R.string.self_choice), "");
		else
			recipientLabel.setHint(action.getRequiredParams().contains(Action.ACCOUNT_KEY) ? getString(R.string.recipientacct_label) : getString(R.string.recipientphone_label));
	}

	protected void onContactSelected(int requestCode, StaxContact contact) {
		transferViewModel.setContact(contact);
		recipientAutocomplete.setText(contact.toString());
	}

	private TextWatcher amountWatcher = new TextWatcher() {
		@Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
		@Override public void afterTextChanged(Editable editable) { }
		@Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
			transferViewModel.setAmount(charSequence.toString());
		}
	};

	private TextWatcher recipientWatcher = new TextWatcher() {
		@Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
		@Override public void afterTextChanged(Editable editable) { }
		@Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
			transferViewModel.setRecipient(charSequence.toString());
		}
	};

	private TextWatcher noteWatcher = new TextWatcher() {
		@Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
		@Override public void afterTextChanged(Editable editable) { }
		@Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
			transferViewModel.setNote(charSequence.toString());
		}
	};
}