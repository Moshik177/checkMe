package com.sadna.app.checkme.entities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.gson.Gson;
import com.sadna.app.checkme.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static android.support.v7.mediarouter.R.id.image;


public class GroupAdapter extends ArrayAdapter<Group>
{
    Typeface face;
    private List<Group> values; // no objects just String array
    private final Context context;
    private android.content.res.Resources resources;


    public GroupAdapter(Context context, List<Group> values) {
        super(context, 0, values);
        this.context = context;
        resources =context.getResources();
        this.values = values;
        face = Typeface.createFromAsset(context.getAssets(), "fonts/gishabd.ttf");

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        Group group = getItem(position);
        Bitmap bitmap = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.group_card_list,parent, false);
        }

        TextView groupName = (TextView) convertView.findViewById(R.id.group_name);
        ImageView groupIcon = (ImageView) convertView.findViewById(R.id.group_empty_picture);
        ImageView pointImage = (ImageView) convertView.findViewById(R.id.point_img);

        if(group.getPhoto() == null) {
            groupIcon.setImageResource(R.drawable.group_empty_picutre);
             Drawable d = groupIcon.getDrawable();
             bitmap = ((BitmapDrawable) d).getBitmap();
        }
        else bitmap =  group.getPhoto();
        Bitmap cutBitmap = getCroppedBitmap(bitmap,150,150);


        Group obj =  values.get(position);
        TextView tv = new TextView(context);
        tv.setText(obj.getName().toString());
        tv.setTypeface(face);

        pointImage.setImageResource(R.drawable.points);
        groupIcon.setImageBitmap(cutBitmap);
        groupName.setText(group.getName());

        return  convertView;

    }

    public static Bitmap getCroppedBitmap(Bitmap bitmap ,int width , int height) {

        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);

        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }
}




