package ly.priv.mobile.gui.datasource;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import ly.priv.mobile.R;
import ly.priv.mobile.Utilities;
import ly.priv.mobile.Values;
import ly.priv.mobile.gui.socialnetworks.ISocialNetworks;
import ly.priv.mobile.gui.socialnetworks.ListUsersAdapter;
import ly.priv.mobile.gui.socialnetworks.SListUsersActivity;
import ly.priv.mobile.gui.socialnetworks.SUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockFragment;
import com.facebook.Request;
import com.facebook.RequestAsyncTask;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionLoginBehavior;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;

public class FaceBookDS extends SherlockFragment implements ISocialNetworks{
	private static final String TAG = "FaceBookDS";
	private ArrayList<SUser> mListUserMess;
	private String mFaceBookUserId;
	private Session mSession;
	private Values mValues;
	private Session.StatusCallback mSessionStatusCallback;
	private SListUsersActivity mSListUsersActivity;
	private ProgressBar mProgressBar;
	/**
	 * 
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.d(TAG, "FaceBookDS");
		View view = inflater.inflate(R.layout.activity_list, container, false);
		ActionBar actionBar = getSherlockActivity().getSupportActionBar();
		actionBar.setTitle(R.string.privly_Login_Facebook);
		mProgressBar = (ProgressBar) view.findViewById(R.id.pbLoadingData);	
		mProgressBar.setVisibility(View.VISIBLE);
		mValues = new Values(getActivity());
		mSessionStatusCallback = new Session.StatusCallback() {

			@Override
			public void call(Session session, SessionState state,
					Exception exception) {
				onSessionStateChange(session, state, exception);

			}
		};			
		login();
		return view;
	}
	
	private void runSocialGui(){
		FragmentTransaction transaction = getActivity()
				.getSupportFragmentManager().beginTransaction();
		mSListUsersActivity = new SListUsersActivity();				
		mSListUsersActivity.setmISocialNetworks(this);		
		transaction.replace(R.id.container, mSListUsersActivity);
		 transaction.disallowAddToBackStack();
		//transaction.addToBackStack(null);
		transaction.commit();
	}
	
	/**
	 * Login in FaceBook
	 */
	private void login() {
		mSession = Session.getActiveSession();
		if (mSession == null) {
			mSession = new Session.Builder(getActivity()).build();
			Session.setActiveSession(mSession);
			if (!mSession.isOpened()) {
				ArrayList<String> permissions = new ArrayList<String>();
				permissions.add("read_mailbox");
				mSession.addCallback(mSessionStatusCallback);
				Session.OpenRequest openRequest = new Session.OpenRequest(
						FaceBookDS.this);
				openRequest.setLoginBehavior(SessionLoginBehavior.SUPPRESS_SSO);
				openRequest
						.setRequestCode(Session.DEFAULT_AUTHORIZE_ACTIVITY_CODE);
				openRequest.setPermissions(permissions);
				mSession.openForRead(openRequest);
			} else {
				//getInboxFromFaceBook();
				runSocialGui();
			}

		} else {
			//getInboxFromFaceBook();
			runSocialGui();
		}
		mProgressBar.setVisibility(View.INVISIBLE);
	}

	/**
	 * this method is used by the facebook API
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Session.getActiveSession().onActivityResult(getActivity(), requestCode,
				resultCode, data);
	}

	/**
	 * Manages the session state change. This method is called after the
	 * <code>login</code> method.
	 * 
	 * @param session
	 * @param state
	 * @param exception
	 */
	private void onSessionStateChange(Session session, SessionState state,
			Exception exception) {
		if (session != mSession) {
			return;
		}

		if (state.isOpened()) {
			// Log in just happened.
			Log.d(TAG, "session opened");
			makeMeRequest();
		} else if (state.isClosed()) {
			// Log out just happened. Update the UI.

			Log.d(TAG, "session closed");
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Session session = Session.getActiveSession();
		Session.saveSession(session, outState);
	}

	/**
	 * Method for get information about me
	 * 
	 */
	private void makeMeRequest() {
		Log.d(TAG, "makeMeRequest");
		mProgressBar.setVisibility(View.VISIBLE);
		// Make an API call to get user data and define a
		// new callback to handle the response.
		Bundle params = new Bundle();
		params.putString("fields", "id");
		Request request = Request.newMeRequest(mSession,
				new Request.GraphUserCallback() {
					@Override
					public void onCompleted(GraphUser user, Response response) {
						if (response.getError() != null) {
							mProgressBar.setVisibility(View.INVISIBLE);
							AlertDialog dialog = Utilities.showDialog(
									getActivity(),
									getString(R.string.error_inbox));
							dialog.show();
							return;
						}
						if (user != null) {

							mValues.setFacebookID(user.getId());
						}
						mProgressBar.setVisibility(View.INVISIBLE);
						//getInboxFromFaceBook();
						runSocialGui();
					}
				});
		request.setParameters(params);
		request.executeAsync();
		
	}

	
	@Override
	public ArrayList<SUser> getListOfUsers() {
		Log.d(TAG, "getListOfUsers");
		mListUserMess = new ArrayList<SUser>();	
		//mProgressBar.setVisibility(View.VISIBLE);
		mFaceBookUserId = mValues.getFacebookID();
		// Make an API call to get user data and define a
		// new callback to handle the response.
		Bundle params = new Bundle();
		params.putString("fields",
				"id,to.fields(id,name,picture),comments.order(chronological).limit(1)");
		// params.putString("limit", "1");
		Request request = Request.newGraphPathRequest(mSession, "me/inbox",null);
		request.setParameters(params);
		RequestAsyncTask asyncTask=request.executeAsync();
		
		try {
			 Response response= asyncTask.get().get(0);
				if (response.getError() != null) {
					Log.e(TAG, response.getError().getErrorMessage());
					AlertDialog dialog = Utilities.showDialog(
							getActivity(),
							getString(R.string.error_inbox));
					dialog.show();
					return null;
				}
				JSONArray listUsersWIthLastMessage = null;
				try {
					listUsersWIthLastMessage = response
							.getGraphObject().getInnerJSONObject()
							.getJSONArray("data");

					for (int i = 0; i < listUsersWIthLastMessage
							.length(); i++) {
						SUser sUser = new SUser();
						JSONObject dialog = listUsersWIthLastMessage
								.getJSONObject(i);
						sUser.setDialogId(dialog.getString("id"));
						sUser.setTime(Utilities.getTimeForFacebook(dialog
								.getString("updated_time")));
						JSONArray to = dialog.getJSONObject("to")
								.getJSONArray("data");
						for (int j = 0; j < to.length(); j++) {
							JSONObject oTo = to.getJSONObject(j);
							String id = oTo.getString("id");
							if (!id.equals(mFaceBookUserId)) {
								sUser.setUserName(oTo.getString("name"));
								JSONObject pic = oTo.getJSONObject(
										"picture")
										.getJSONObject("data");
								sUser.setUrlToAvatar(pic
										.getString("url"));
								break;
							}

						}
						JSONObject comment = dialog
								.getJSONObject("comments")
								.getJSONArray("data").getJSONObject(0);
						sUser.setLastUserMess(comment
								.getString("message"));
						mListUserMess.add(sUser);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			 return mListUserMess;
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
