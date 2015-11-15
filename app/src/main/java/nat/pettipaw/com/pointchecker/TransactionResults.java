package nat.pettipaw.com.pointchecker;

import android.graphics.Color;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Crash on 11/14/2015.
 */

public class TransactionResults {
    int total;
    List<Transaction> results;
    List<Transaction> traditionalPlan;
    List<Transaction> residentBucks;
    DateTime currentSemesterStart;
    double startingAmount;
    double startingTerpBucksAmount;
    double averageSpending;

    public void process(){
        Collections.sort(results, new Comparator<Transaction>() {
            @Override
            public int compare(Transaction t1, Transaction t2) {
                return t1.trans.compareTo(t2.trans);
            }
        });
        traditionalPlan = new ArrayList<>();
        residentBucks = new ArrayList<>();
        boolean nextFirstTradition = false, nextFirstTerpBucks = false;
        for (Transaction t : results){
            if (t.plan.equals("Traditional Plan")) {
                if (nextFirstTradition) {
                    currentSemesterStart = t.trans.withTime(0, 0, 0, 0);
                    nextFirstTradition = false;
                }
                if (t.amt < 0.0) {
                    nextFirstTradition = true;
                    startingAmount = t.amt * -1;
                }
                traditionalPlan.add(t);
            } else if (t.plan.equals("Resident Bucks")) {
                if (nextFirstTerpBucks)
                    nextFirstTerpBucks = false;
                if (t.amt < 0.0) {
                    nextFirstTerpBucks = true;
                    startingTerpBucksAmount = t.amt * -1;
                }
                residentBucks.add(t);
            }
        }
    }

    public double getTraditionalAmountOn(DateTime date){

        for (int i = 0; i < traditionalPlan.size(); ++i) {
            Transaction t = traditionalPlan.get(i);
            if (t.trans.isAfter(date))
                return traditionalPlan.get(i - 1).bal;
        }

        return -1;
    }

    public LineData generateDataFor(DateTime end){

        DateTime now = DateTime.now().withTime(0, 0, 0, 0);
        if (now.isAfter(end))
            now = end;
        List<Entry> points = new ArrayList<>();
        List<Entry> bucks = new ArrayList<>();
        List<Entry> recommendedPoints = new ArrayList<>();
        List<String> dates = new ArrayList<>();

        DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendPattern("MM/dd/yy").toFormatter();

        int numDays = Days.daysBetween(currentSemesterStart, end).getDays();
        double perDay = 0;
        double currentAmount = 0;
        averageSpending = 0;
        DateTime currentDate = null, dt;

        for (Transaction t : traditionalPlan) {

            if ((t.trans.isAfter(currentSemesterStart) && t.trans.isBefore(now)) || t.trans.isEqual(currentSemesterStart) || t.trans.isEqual(now)){
                DateTime timestamp = t.trans.withTime(0, 0, 0, 0);
                if (currentDate == null) {
                    dt = currentSemesterStart;
                    perDay = startingAmount / numDays;
                    while (dt.isBefore(timestamp)){
                        points.add(new Entry((float) startingAmount, points.size()));
                        recommendedPoints.add(new Entry((float) ((numDays - (Days.daysBetween(currentSemesterStart, dt).getDays() + 1)) * perDay), recommendedPoints.size()));
                        dates.add(formatter.print(dt));
                        dt = dt.plusDays(1);
                    }
                    currentDate = timestamp;
                }
                if (timestamp.isEqual(currentDate))
                    currentAmount = t.bal;
                else {
                    dt = currentDate;
                    do {
                        points.add(new Entry((float) currentAmount, points.size()));
                        recommendedPoints.add(new Entry((float) ((numDays - (Days.daysBetween(currentSemesterStart, dt).getDays() + 1)) * perDay), recommendedPoints.size()));
                        dates.add(formatter.print(dt));
                        dt = dt.plusDays(1);
                    } while (dt.isBefore(timestamp));
                    currentDate = timestamp;
                    currentAmount = t.bal;
                }
                if (currentAmount == 0 && averageSpending == 0)
                    averageSpending = startingAmount / Days.daysBetween(currentSemesterStart, timestamp).getDays();
            }
        }

        dt = currentDate;
        if (dt == null){
            dt = currentSemesterStart;
            currentAmount = startingAmount;
        }
        while (dt.isBefore(now) || dt.isEqual(now)) {
            points.add(new Entry((float) currentAmount, points.size()));
            recommendedPoints.add(new Entry((float) ((numDays - (Days.daysBetween(currentSemesterStart, dt).getDays() + 1)) * perDay), recommendedPoints.size()));
            dates.add(formatter.print(dt));
            dt = dt.plusDays(1);
        }

        if (averageSpending == 0)
            averageSpending = (startingAmount - currentAmount) / Days.daysBetween(currentSemesterStart, now).getDays();

        currentDate = null;
        currentAmount = 0;
        for (Transaction t : residentBucks){

            if ((t.trans.isAfter(currentSemesterStart) && t.trans.isBefore(now)) || t.trans.isEqual(currentSemesterStart) || t.trans.isEqual(now)){
                DateTime timestamp = t.trans.withTime(0, 0, 0, 0);
                if (currentDate == null) {
                    dt = currentSemesterStart;
                    while (dt.isBefore(timestamp)){
                        bucks.add(new Entry((float) startingTerpBucksAmount, bucks.size()));
                        dt = dt.plusDays(1);
                    }
                    currentDate = timestamp;
                }
                if (timestamp.isEqual(currentDate))
                    currentAmount = t.bal;
                else {
                    dt = currentDate;
                    do {
                        bucks.add(new Entry((float) currentAmount, bucks.size()));
                        dt = dt.plusDays(1);
                    } while (dt.isBefore(timestamp));
                    currentDate = timestamp;
                    currentAmount = t.bal;
                }
            }

        }

        dt = currentDate;
        if (dt == null) {
            dt = currentSemesterStart;
            currentAmount = startingTerpBucksAmount;
        }
        while (dt.isBefore(now) || dt.isEqual(now)) {
            bucks.add(new Entry((float) currentAmount, bucks.size()));
            dt = dt.plusDays(1);
        }

        ArrayList<LineDataSet> dataSets = new ArrayList<>();

        LineDataSet dataSet = new LineDataSet(points, "Your Points");
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSets.add(dataSet);
        dataSet = new LineDataSet(recommendedPoints, "Recommended Points");
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSet.setCircleColor(Color.RED);
        dataSet.setColor(Color.RED);
        dataSets.add(dataSet);
        dataSet = new LineDataSet(bucks, "Your Terp Bucks");
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSet.setCircleColor(Color.BLUE);
        dataSet.setColor(Color.BLUE);
        dataSets.add(dataSet);
        return new LineData(dates, dataSets);

    }

}

