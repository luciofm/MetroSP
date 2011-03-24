/*
 *
 *  Metro SP
 *
 *  Copyright (C) 2011  Am.Droid (www.amdroid.net). All rights reserved.
 *  Copyright (C) 2011  Lucio Maciel <luciofm@amdroid.net>. All rights 
 *  reserved.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */



package net.amdroid.metrosp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MetroSP extends Activity implements Runnable {
	ListView listview;
	private ArrayList<MetroLine> metroLines;
	private MetroLineAdapter adapter;
	private String last_refresh;
	private TextView title;
	private Button refresh_btn;
	private boolean refreshing = false;

	private static final int LOAD_ERROR = 0;
	private static final int LOAD_OK = 1;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.main);

	    AdView adView = new AdView(this, AdSize.BANNER, YOUR_ADMOB_ID_HERE);
	    LinearLayout layout = (LinearLayout)findViewById(R.id.addsLayout);
	    android.view.ViewGroup.LayoutParams params = layout.getLayoutParams();
	    layout.addView(adView);
	    AdRequest adrequest = new AdRequest();
	    adView.loadAd(adrequest);

		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.window_title);


		metroLines = new ArrayList<MetroLine>();
		int resID = R.layout.list_item;
		adapter = new MetroLineAdapter(this, resID, metroLines);

		listview = (ListView) findViewById(R.id.metroList);
		title = (TextView) findViewById(R.id.app_title);
		refresh_btn = (Button) findViewById(R.id.refresh_button);
		listview.setAdapter(adapter);

		refresh_btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d("MetroSP", "Refresh");
				refreshData();
			}
		});

		refreshData();
	}

	public void refreshData() {
		if (refreshing)
			return;

		title.setText("Carregando...");
		Thread thread = new Thread(this);
		thread.start();
	}

	public void run() {
		Message msg;

		if (refreshing)
			return;

		refreshing = true;
		metroLines.clear();
		int ret = loadData();

		msg = handler.obtainMessage();
		msg.arg1 = ret;
		handler.sendMessage(msg);
	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.arg1 == LOAD_OK) {
				title.setText(last_refresh);
				adapter.notifyDataSetChanged();
			} else {
				title.setText("Erro carregando dados.");
			}
			refreshing = false;
		}
	};

	public class MetroLine {
		private String _Line;
		private String _Color;
		private String _Status;
		private String _LongStatus;
		private String _StatusColor;

		public MetroLine(String _Line, String _Color, String _Status,
				String _LongStatus, String _StatusColor) {
			super();
			this._Line = _Line;
			this._Color = _Color;
			this._Status = _Status;
			this._LongStatus = _LongStatus;
			this._StatusColor = _StatusColor;
		}

		public String get_Line() {
			return _Line;
		}

		public String get_Color() {
			return _Color;
		}

		public String get_Status() {
			return _Status;
		}

		public String get_LongStatus() {
			return _LongStatus;
		}

		public String get_StatusColor() {
			return _StatusColor;
		}
	}

	public class MetroLineAdapter extends ArrayAdapter<MetroLine> {
		int resource;
		Context context;

		public MetroLineAdapter(Context _context, int _resource,
				List<MetroLine> _items) {
			super(_context, _resource, _items);
			context = _context;
			resource = _resource;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			RelativeLayout itemView;
			int status_res;

			MetroLine item = getItem(position);

			String line = item.get_Line();
			String color = item.get_Color();
			String status = item.get_Status();
			String long_status = item.get_LongStatus();
			String full_status = null;
			if (long_status.length() > 1)
				full_status = status + " - " + long_status;
			else
				full_status = status;

			if (convertView == null) {
				itemView = new RelativeLayout(getContext());
				String inflater = Context.LAYOUT_INFLATER_SERVICE;
				LayoutInflater vi = (LayoutInflater) getContext()
						.getSystemService(inflater);
				vi.inflate(resource, itemView, true);
			} else {
				itemView = (RelativeLayout) convertView;
			}

			itemView.setTag(new String(line));

			TextView textLinha = (TextView) itemView
					.findViewById(R.id.textLinha);
			TextView textStatus = (TextView) itemView
					.findViewById(R.id.textStatus);
			ImageView icon = (ImageView) itemView.findViewById(R.id.imageView1);

			textLinha.setText(line);
			textStatus.setText(full_status);

			Resources r = context.getResources();
			status_res = r.getIdentifier(item.get_StatusColor().toLowerCase(),
					"drawable", "net.amdroid.metrosp");
			Bitmap status_img = BitmapFactory.decodeResource(
					context.getResources(), status_res);

			icon.setImageBitmap(status_img);
			return itemView;
		}
	}

	private int loadData() {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpResponse response = null;

		HttpGet httpget = new HttpGet(
				"http://www.metro.sp.gov.br/MetroStatusLinha/diretoDoMetro.aspx");
		try {
			response = httpclient.execute(httpget);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			Log.d("MetroSP", "MetroSP() <- ClientProtocolException");
			e.printStackTrace();
			return LOAD_ERROR;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.d("MetroSP", "MetroSP() <- IOException");
			e.printStackTrace();
			return LOAD_ERROR;
		}

		HttpEntity entity = response.getEntity();
		if (entity != null) {
			try {
				parseData(entity.getContent());
				entity.consumeContent();
				return LOAD_OK;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Log.d("MetroSP", "MetroSP() <- Exception");
				e.printStackTrace();
			}
		}
		return LOAD_ERROR;
	}

	private void parseData(InputStream content) throws Exception {
		BufferedReader r = new BufferedReader(new InputStreamReader(content));
		StringBuilder total = new StringBuilder();
		String line, buffer;
		while ((line = r.readLine()) != null) {
			total.append(line);
		}
		buffer = total.toString();

		Log.d("MetroSP", "MetroSP()");

		int where = buffer.indexOf("objArrLinhas") + 15;
		int end = buffer.indexOf("function abreDetalheLinha") - 1;
		String jsonstr = buffer.substring(where, end);

		JSONArray array = (JSONArray) new JSONTokener(jsonstr).nextValue();
		for (int i = 0; i < array.length(); i++) {
			JSONObject obj = array.getJSONObject(i);

			String cor = obj.getString("cor");
			String linha = obj.getString("linha");
			String status = obj.getString("status");
			String msgstatus = Html.fromHtml(obj.getString("msgStatus"))
					.toString();
			String tmp = obj.getString("imagem");
			String status_color = tmp.substring(tmp.indexOf("sinal-") + 6,
					tmp.indexOf("-linha"));

			MetroLine item = new MetroLine(linha, cor, status, msgstatus,
					status_color);
			metroLines.add(item);
		}

		where = buffer.indexOf("dataAtualizacao\">")
				+ "dataAtualizacao\">".length();
		end = buffer.indexOf("</span>", where);
		last_refresh = Html.fromHtml(buffer.substring(where, end)).toString();

		Log.d("MetroSP", "MetroSP() -> " + jsonstr);
	}
}
