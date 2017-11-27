package nextrev.perception.adapters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import android.app.Activity;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import nextrev.perception.R;
import nextrev.perception.activities.BLEScanActivity;

public class DeviceListAdapter extends BaseAdapter {

    private Activity activity;
    private ArrayList<HashMap<String, String>> data;
    private static LayoutInflater inflater=null;

    public DeviceListAdapter(Activity activity, ArrayList<HashMap<String, String>> data) {
        this.activity = activity;
        this.data = data;
        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount() {
        return data.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if(convertView == null)
            view = inflater.inflate(R.layout.device_list_row, null);

        TextView name = view.findViewById(R.id.name); // device name
        TextView address = view.findViewById(R.id.address); // device address
        ImageView connection_status = view.findViewById(R.id.connection_status); // connection status

        HashMap<String, String> deviceInfo = data.get(position);

        name.setText(deviceInfo.get(BLEScanActivity.KEY_NAME));
        address.setText(deviceInfo.get(BLEScanActivity.KEY_ADDRESS));
        String connectionStatus = deviceInfo.get(BLEScanActivity.KEY_CONNECTION_STATUS);

        if (Objects.equals(connectionStatus, "connected"))
            connection_status.setImageDrawable(ContextCompat.getDrawable(this.activity.getApplicationContext(), R.drawable.ic_status_connected));
        else if (Objects.equals(connectionStatus, "connecting"))
            connection_status.setImageDrawable(ContextCompat.getDrawable(this.activity.getApplicationContext(), R.drawable.ic_status_connecting));
        else // if (connectionStatus == "disconnected")
            connection_status.setImageDrawable(ContextCompat.getDrawable(this.activity.getApplicationContext(), R.drawable.ic_status_disconnected));


        return view;
    }
}