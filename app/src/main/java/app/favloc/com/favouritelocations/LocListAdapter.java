package app.favloc.com.favouritelocations;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by vaam on 24-05-2017.
 */

public class LocListAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<HashMap<String, String>> locations;
    private static LayoutInflater inflater = null;
    AlertDialog.Builder alertDialog;

    public LocListAdapter(Context Context, ArrayList<HashMap<String, String>> Locations) {
        mContext = Context;
        locations = Locations;
    }

    @Override
    public int getCount() {
        return locations.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder;

        if(view == null || view.getTag() == null)
        {
            inflater = LayoutInflater.from(mContext);
            view = inflater.inflate(R.layout.list_row, null);
            holder = new ViewHolder();

            holder.locationName = (TextView) view.findViewById(R.id.locNameID);
            holder.locationLandMark = (TextView) view.findViewById(R.id.locLandMarkID);
            holder.locationLat = (TextView) view.findViewById(R.id.locLatID);
            holder.locationLng = (TextView) view.findViewById(R.id.locLngID);

            view.setTag(holder);
        }
        else
            holder = (ViewHolder) view.getTag();

        final HashMap<String, String> mLocations;
        mLocations = locations.get(position);

        final String latitude = "Latitude: " + mLocations.get("LatKey");
        final String longitude = "Longitude: " + mLocations.get("LngKey");
        final String refKey = mLocations.get("refKey");

        holder.locationName.setText(mLocations.get("LocNameKey"));
        holder.locationLandMark.setText(mLocations.get("LocLandMarkKey"));
        holder.locationLat.setText(latitude);
        holder.locationLng.setText(longitude);

        ImageView deleteIcon = (ImageView) view.findViewById(R.id.deleteIconID);
        deleteIcon.setTag(position);
        final View finalView = view;
        deleteIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog = new AlertDialog.Builder(finalView.getRootView().getContext());
                alertDialog.setMessage("Are you sure to delete this location?");
                alertDialog.setIcon(R.drawable.deleteicon2);
                alertDialog.setTitle("Delete Location");
                alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Locations");
                        databaseReference.child(refKey).removeValue();
                        Toast.makeText(mContext,"Location deleted successfully!", Toast.LENGTH_LONG).show();
                    }
                });
                alertDialog.setNegativeButton("Cancel", null);
                alertDialog.create().show();
            }
        });

        view.findViewById(R.id.mapIconID).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mContext, "Opening Google Maps...", Toast.LENGTH_LONG).show();
                double latitude = Double.parseDouble(mLocations.get("LatKey"));
                double longitude = Double.parseDouble(mLocations.get("LngKey"));
                String label = mLocations.get("LocNameKey");
                String uriBegin = "geo:" + latitude + "," + longitude;
                String query = latitude + "," + longitude + "(" + label + ")";
                String encodedQuery = Uri.encode(query);
                String uriString = uriBegin + "?q=" + encodedQuery + "&z=16";
                Uri uri = Uri.parse(uriString);
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW, uri);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                mContext.startActivity(intent);

                /*Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + mLocations.get("LatKey") + "," + mLocations.get("Lngkey")));
                mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                mContext.startActivity(mapIntent);*/
            }
        });

        view.findViewById(R.id.shareIconID).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mContext, "Sharing location...", Toast.LENGTH_LONG).show();
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT,
                        "Sharing location from Favourite Locations \n \n" + "Location Name: " +
                        mLocations.get("LocNameKey") + "\n" +
                                "Landmark: " +
                                mLocations.get("LocLandMarkKey") + "\n" +
                                latitude + "\n" + longitude + "\n \n"
                                + "Click link below to navigate: \n"
                                + "https://www.google.co.in/maps/dir//" + mLocations.get("LatKey") + "," + mLocations.get("LngKey")+"/@" + mLocations.get("LatKey") + "," + mLocations.get("LngKey") + ",17z");
                sendIntent.setType("text/plain");
                sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                mContext.startActivity(sendIntent);
            }
        });

        return view;
    }

    private class ViewHolder
    {
        TextView locationName, locationLandMark, locationLat, locationLng;
        ImageView mapIcon, shareIcon, deleteIcon;
    }
}
