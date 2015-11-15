package nat.pettipaw.com.pointchecker;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

import java.util.ArrayList;
import java.util.List;

public class TransactionViewerActivity extends AppCompatActivity {

    LinearLayout transactionListLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_viewer);

        transactionListLayout = (LinearLayout) findViewById(R.id.transactionListLayout);

        List<String> transactions = new ArrayList<>();
        DateTime currentDay = null;
        double currentSpentTraditional = 0, currentSpentTerpBucks = 0, startingTraditional = -1, startingTerpBucks = -1, endingTraditionalBalance = -1, endingTerpBucksBalance = -1;
        TransactionResults results = DataHandler.g.results;
        DateTime endRange = results.currentSemesterStart.plusMonths(6);
        DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendPattern("hh:mm:ss a").toFormatter();

        for (Transaction t : results.results){

            DateTime time = t.trans.withTime(0, 0, 0, 0);

            if ((time.isAfter(results.currentSemesterStart) && time.isBefore(endRange)) || time.isEqual(results.currentSemesterStart) || time.isEqual(endRange)) {

                if (currentDay == null)
                    currentDay = time;

                if (!currentDay.isEqual(time)){

                    String date = new DateTimeFormatterBuilder().appendPattern("MM/dd/yy").toFormatter().print(currentDay);
                    addText(transactions.size() + " transactions on " + date, 20f);
                    addDivider();

                    if (startingTraditional == -1)
                        startingTraditional = endingTraditionalBalance;
                    if (startingTerpBucks == -1)
                        startingTerpBucks = endingTerpBucksBalance;

                    if (startingTraditional == -1)
                        startingTraditional = endingTraditionalBalance = results.startingAmount;
                    if (startingTerpBucks == -1)
                        startingTerpBucks = endingTerpBucksBalance = results.startingTerpBucksAmount;

                    addText(currentSpentTraditional + " points total, (" + String.format("%.02f", startingTraditional) + " --> " + String.format("%.02f", endingTraditionalBalance) + ")", 16f);
                    addText(currentSpentTerpBucks + " Terp Bucks total, (" + String.format("%.02f", startingTerpBucks) + " --> " + String.format("%.02f", endingTerpBucksBalance) + ")", 16f);
                    addText(" ");
                    for (String text : transactions)
                        addText(text);
                    addText(" ");
                    addText(" ");

                    transactions.clear();
                    startingTraditional = startingTerpBucks = -1;
                    currentSpentTerpBucks = currentSpentTraditional = 0;
                    currentDay = time;

                }

                if (t.plan.equals("Traditional Plan")) {

                    if (startingTraditional == -1)
                        startingTraditional = t.amt + t.bal;

                    endingTraditionalBalance = t.bal;
                    currentSpentTraditional += t.amt;
                    transactions.add("Spent " + t.amt + " points at " + t.loc + " at " + formatter.print(t.trans));

                } else if (t.plan.equals("Resident Bucks")) {

                    if (startingTerpBucks == -1)
                        startingTerpBucks = t.amt + t.bal;

                    endingTerpBucksBalance = t.bal;
                    currentSpentTerpBucks += t.amt;
                    transactions.add("Spent " + t.amt + " bucks at " + t.loc + " at " + formatter.print(t.trans));

                }

            }
        }

    }

    public void onBackToPointGraph(View v){

        Intent intent = new Intent(TransactionViewerActivity.this, PointViewerActivity.class);
        startActivity(intent);

    }

    public void addDivider(){

        View v = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 3);
        params.setMargins(0, 30, 0, 30);
        v.setLayoutParams(params);
        v.setBackgroundColor(Color.WHITE);
        transactionListLayout.addView(v);

    }

    public void addText(String text){
        addText(text, 14f);
    }

    public void addText(String text, float size){

        TextView tv = new TextView(this);
        tv.setLayoutParams(new WindowManager.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        tv.setTextColor(Color.WHITE);
        tv.setText(text);
        tv.setBackgroundColor(Color.TRANSPARENT);
        tv.setTextSize(size);
        transactionListLayout.addView(tv);

    }

}
