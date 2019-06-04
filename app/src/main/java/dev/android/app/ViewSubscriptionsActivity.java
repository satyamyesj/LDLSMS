package dev.android.app;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import dev.android.app.Adapters.ViewAdapter;
import dev.android.app.Adapters.ViewAdapterForSubscriptionsList;

public class ViewSubscriptionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_subscriptions);

        int count=getIntent().getIntExtra("count",0);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        ((LinearLayoutManager) layoutManager).setOrientation(LinearLayoutManager.VERTICAL);
        RecyclerView scrollView = findViewById(R.id.scrollView);
        scrollView.setLayoutManager(layoutManager);
        scrollView.setHasFixedSize(true);
        ViewAdapterForSubscriptionsList viewAdapter = new ViewAdapterForSubscriptionsList(this, count);
        scrollView.setAdapter(viewAdapter);
    }
}
