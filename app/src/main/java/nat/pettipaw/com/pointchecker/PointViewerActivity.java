package nat.pettipaw.com.pointchecker;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.cengalabs.flatui.FlatUI;
import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.datepicker.DatePickerBuilder;
import com.codetroopers.betterpickers.datepicker.DatePickerDialogFragment;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.XAxisValueFormatter;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.tz.DateTimeZoneBuilder;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;


public class PointViewerActivity extends ActionBarActivity implements CalendarDatePickerDialogFragment.OnDateSetListener {

    Button endOfSemesterButton, viewAllButton;
    LineChart chart;
    Handler handler;
    Gson parser;
    TextView chartTitle;
    TextView pointsInfo;
    TextView chartProgress;

    CalendarDatePickerDialogFragment calendarDatePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_point_viewer);

        endOfSemesterButton = (Button) findViewById(R.id.setEndOfSemester);
        viewAllButton = (Button) findViewById(R.id.viewAllTransactions);
        chartProgress = (TextView) findViewById(R.id.chartProgressText);

        chart = (LineChart) findViewById(R.id.pointsChart);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDoubleTapToZoomEnabled(true);

        chart.getLegend().setTextColor(Color.WHITE);
        chart.setDescription("");
        chart.setNoDataText("");

        chartTitle = (TextView) findViewById(R.id.chartTitle);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);

        YAxis yAxis = chart.getAxisRight();
        yAxis.setDrawGridLines(false);
        yAxis.setDrawLabels(false);

        yAxis = chart.getAxisLeft();
        yAxis.setTextColor(Color.WHITE);
        yAxis.setAxisMinValue(0);
        yAxis.setLabelCount(11, false);

        pointsInfo = (TextView) findViewById(R.id.pointsInfo);

        parser = Converters.registerDateTime(new GsonBuilder()).create();

        handler = new Handler();

        WebView view = (WebView) findViewById(R.id.dataWebView);
        view.getSettings().setJavaScriptEnabled(true);
        view.addJavascriptInterface(new JavascriptDataHandler(), "android");
        view.setWebViewClient(new WebViewClient() {
            int loadTimes = 0;

            @Override
            public void onPageFinished(WebView view, String url) {
                if (loadTimes == 0) {
                    view.loadUrl("https://dsonline.umd.edu/student/form/oop/service/activity_manager.php?startdt=01/01/2014&enddt=12/31/2030");
                } else if (loadTimes == 1) {
                    view.loadUrl("javascript:" +
                            "(function(){" +
                            "window.android.onGetPurchaseData(document.body.innerHTML);" +
                            "})();");
                } else if (loadTimes == 2) {
                    view.loadUrl("https://dsonline.umd.edu/student/form/oop/dining_bought.php?_dc=" + System.currentTimeMillis());
                } else if (loadTimes == 3) {
                    view.loadUrl("javascript:" +
                            "(function(){" +
                            "window.android.onGetDiningBalances(document.body.innerHTML);" +
                            "})();");
                }
                loadTimes++;
            }
        });

        DateTime now = DateTime.now();
        calendarDatePicker = CalendarDatePickerDialogFragment.newInstance(this, now.getYear(), now.getMonthOfYear() - 1, now.getDayOfMonth());
        calendarDatePicker.setThemeCustom(com.codetroopers.betterpickers.R.style.BetterPickersRadialTimePickerDialog);
        if (DataHandler.g.results == null) {
            chart.setVisibility(View.INVISIBLE);
            chartProgress.setText("Loading transaction data...");
            viewAllButton.setEnabled(false);
            endOfSemesterButton.setEnabled(false);
            view.loadUrl("https://dsonline.umd.edu/student/form/oop/service/activity_manager.php?startdt=01/01/2014&enddt=12/31/2030");
        } else if (DataHandler.g.endOfSemester == null) {
            chart.setVisibility(View.INVISIBLE);
            chartProgress.setText("Please set your final semester date");
        } else {
            updateChart(DataHandler.g.endOfSemester);
        }


    }

    @Override
    public void onResume() {
        // Example of reattaching to the fragment
        super.onResume();
        CalendarDatePickerDialogFragment calendarDatePickerDialogFragment = (CalendarDatePickerDialogFragment) getSupportFragmentManager()
                .findFragmentByTag("end_date_fragment");
        if (calendarDatePickerDialogFragment != null)
            calendarDatePickerDialogFragment.setOnDateSetListener(this);
    }

    @Override
    public void onDateSet(CalendarDatePickerDialogFragment dialog, int year, int monthOfYear, int dayOfMonth) {

        TransactionResults results = DataHandler.g.results;
        DateTime newEnd = new DateTime(year, monthOfYear + 1, dayOfMonth, 0, 0, 0);
        if (newEnd.isBefore(results.currentSemesterStart))
            newEnd = results.currentSemesterStart.plusDays(1);
        DataHandler.g.endOfSemester = newEnd;
        updateChart(DataHandler.g.endOfSemester);
        endOfSemesterButton.setEnabled(true);

    }

    public void onSetEndOfSemester(View v){

        v.setEnabled(false);
        calendarDatePicker.show(getSupportFragmentManager(), "end_date_fragment");

    }

    public void onViewAll(View v){

        Intent intent = new Intent(PointViewerActivity.this, TransactionViewerActivity.class);
        startActivity(intent);

    }

    public void updateChart(DateTime end){

        chart.setVisibility(View.VISIBLE);

        TransactionResults results = DataHandler.g.results;
        String semester = results.currentSemesterStart.getMonthOfYear() <= 6 ? "Spring" : "Fall";
        String description = semester + " " + results.currentSemesterStart.getYear() + " Semester, Ending on " + new DateTimeFormatterBuilder().appendPattern("MM/dd/yy").toFormatter().print(end);

        chartTitle.setText(description);

        DateTime now = DateTime.now();
        if (now.isAfter(end))
            now = end;

        int totalDays = Days.daysBetween(results.currentSemesterStart, end).getDays();
        double amountLeft = results.getTraditionalAmountOn(now);
        double startingAmount = results.startingAmount;
        double recommendedAmount = (startingAmount - ((Days.daysBetween(results.currentSemesterStart, now).getDays() + 1) * (startingAmount / totalDays)));
        if (recommendedAmount < 0)
            recommendedAmount = 0;
        double difference = amountLeft - recommendedAmount;
        chart.setData(results.generateDataFor(end));
        chart.invalidate();

        pointsInfo.setText(Html.fromHtml("You must spend an average of <b>" + String.format("%.02f", startingAmount / totalDays) + "</b> points to last to the end of the semester." +
                "<br>Current spending rate: <b>" + String.format("%.02f", results.averageSpending) + "</b> points per day." +
                "<br>You have <b>" + amountLeft + "</b> points to spend as of " + (new DateTimeFormatterBuilder().appendPattern("MM/dd/yyyy").toFormatter().print(now)) +
                "<br>You are <font color=" + (difference < 0 ? "#FF0000" : "#00FF00") + "><b>" + String.format("%.02f", difference) + "</b></font> points away from the recommended amount."));
        pointsInfo.setTextColor(Color.WHITE);
    }

    private class JavascriptDataHandler {

        @android.webkit.JavascriptInterface
        public void onGetPurchaseData(final String json) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    DataHandler.g.results = parser.fromJson(json, TransactionResults.class);
                    DataHandler.g.results.process();
                    if (DataHandler.g.endOfSemester == null) {
                        chart.setVisibility(View.INVISIBLE);
                        PointViewerActivity.this.chartProgress.setText("Please enter your final semester date");
                    } else
                        PointViewerActivity.this.updateChart(DataHandler.g.endOfSemester);
                    PointViewerActivity.this.endOfSemesterButton.setEnabled(true);
                    PointViewerActivity.this.viewAllButton.setEnabled(true);
                    PointViewerActivity.this.chart.getAxisLeft().setAxisMaxValue((int) DataHandler.g.results.startingAmount + 100);
                }
            }, 50);

        }

        public void onGetDiningBalances(String json){

        }
    }

 }
