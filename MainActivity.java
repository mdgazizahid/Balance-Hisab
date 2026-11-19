package com.example.dailycashhisab;

import android.app.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.graphics.Color;
import android.content.SharedPreferences;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    SharedPreferences prefs;
    JSONArray entries;
    TextView tvIncome, tvExpense, tvCash, tvDate, tvEmpty;
    LinearLayout list;
    EditText amount, note;
    RadioGroup type;
    double opening = 0;

    String today() { return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()); }
    String money(double n) { return String.format(Locale.getDefault(), "৳ %,.2f", n).replace(".00",""); }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences("cash_data", MODE_PRIVATE);
        opening = Double.longBitsToDouble(prefs.getLong("opening", Double.doubleToLongBits(0)));
        try { entries = new JSONArray(prefs.getString("entries", "[]")); } catch(Exception e) { entries = new JSONArray(); }
        tvIncome=findViewById(R.id.tvIncome); tvExpense=findViewById(R.id.tvExpense);
        tvCash=findViewById(R.id.tvCash); tvDate=findViewById(R.id.tvDate);
        tvEmpty=findViewById(R.id.tvEmpty); list=findViewById(R.id.listTransactions);
        amount=findViewById(R.id.etAmount); note=findViewById(R.id.etNote);
        type=findViewById(R.id.rgType);
        tvDate.setText("তারিখ: " + new SimpleDateFormat("dd MMMM yyyy", new Locale("bn","BD")).format(new Date()));
        findViewById(R.id.btnAdd).setOnClickListener(v -> addEntry());
        findViewById(R.id.btnOpening).setOnClickListener(v -> setOpening());
        refresh();
    }

    void save() { prefs.edit().putString("entries", entries.toString()).putLong("opening", Double.doubleToLongBits(opening)).apply(); }

    void addEntry() {
        String a=amount.getText().toString().trim();
        if(a.isEmpty() || Double.parseDouble(a)<=0) { amount.setError("সঠিক টাকার পরিমাণ দিন"); return; }
        double value=Double.parseDouble(a);
        try {
            JSONObject o=new JSONObject();
            o.put("date", today()); o.put("type", type.getCheckedRadioButtonId()==R.id.rbIncome ? "income":"expense");
            o.put("amount", value); o.put("note", note.getText().toString().trim().isEmpty() ? "বিবরণ নেই":note.getText().toString().trim());
            o.put("time", new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date()));
            entries.put(o); save(); amount.setText(""); note.setText(""); refresh();
            Toast.makeText(this,"হিসাব সংরক্ষণ হয়েছে",Toast.LENGTH_SHORT).show();
        } catch(Exception e) { Toast.makeText(this,"সংরক্ষণ করা যায়নি",Toast.LENGTH_SHORT).show(); }
    }

    void setOpening() {
        EditText input=new EditText(this); input.setInputType(8194); input.setHint("শুরুর ক্যাশের পরিমাণ");
        input.setText(opening==0 ? "" : String.valueOf(opening));
        new AlertDialog.Builder(this).setTitle("শুরুর ক্যাশ সেট করুন").setMessage("দিনের শুরুতে হাতে থাকা মোট টাকা লিখুন।")
          .setView(input).setNegativeButton("বাতিল",null).setPositiveButton("সংরক্ষণ",(d,w)->{
            try { opening=Math.max(0,Double.parseDouble(input.getText().toString())); save(); refresh(); }
            catch(Exception e) { Toast.makeText(this,"সঠিক পরিমাণ লিখুন",Toast.LENGTH_SHORT).show(); }
          }).show();
    }

    void refresh() {
        double inc=0, exp=0, allIncome=0, allExpense=0;
        list.removeAllViews();
        int count=0;
        for(int i=entries.length()-1;i>=0;i--) {
            JSONObject o=entries.optJSONObject(i); if(o==null) continue;
            double val=o.optDouble("amount",0);
            boolean income="income".equals(o.optString("type"));
            if(income) allIncome+=val; else allExpense+=val;
            if(!today().equals(o.optString("date"))) continue;
            if(income) inc+=val; else exp+=val;
            count++;
            LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setPadding(12,12,12,12);
            row.setBackgroundColor(Color.WHITE);
            LinearLayout info=new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL);
            TextView title=new TextView(this); title.setText(o.optString("note")); title.setTextColor(Color.rgb(35,55,46)); title.setTextSize(16);
            TextView time=new TextView(this); time.setText(o.optString("time")); time.setTextColor(Color.GRAY); time.setTextSize(12);
            info.addView(title); info.addView(time);
            TextView valText=new TextView(this); valText.setText((income?"+ ":"- ")+money(val)); valText.setTextColor(income?Color.rgb(8,127,91):Color.rgb(192,57,43)); valText.setTextSize(15); valText.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(info,new LinearLayout.LayoutParams(0,-2,1)); row.addView(valText);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.bottomMargin=6; list.addView(row,lp);
        }
        tvIncome.setText(money(inc)); tvExpense.setText(money(exp)); tvCash.setText(money(opening+allIncome-allExpense));
        tvEmpty.setVisibility(count==0?View.VISIBLE:View.GONE);
    }
}
