package com.example.stellarwallet_android;

import android.os.Bundle;
import android.os.AsyncTask;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Server;
import org.stellar.sdk.responses.AccountResponse;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    TextView txSecret;
    TextView txAddress;
    TextView txBalance;

    String seed, address;
    Server server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        this.txSecret = (TextView) findViewById(R.id.secret);
        this.txAddress = (TextView) findViewById(R.id.address);
        this.txBalance = (TextView) findViewById(R.id.balance);

        this.initWallet();
        new GetBalanceRunner().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void saveSeed(String seed) {
        // You have to save the seed to secure storage.
    }

    private String loadSeed() {
        // Please replace seed with code which load from secure storage.
        String seed = "SAUR3QQWFUAYGCECQEXQVK36T6AAD6HJ3BJBSTV2W2QAS2E23HICPLQU";
//        String seed = "";

        return seed;
    }

    private void initWallet() {
        this.server = new Server("https://horizon.stellar.org");
        this.seed = this.loadSeed();

        if (this.seed.length() == 0) {
            // create a new pair
            KeyPair pair = KeyPair.random();
            this.seed = new String(pair.getSecretSeed());
            this.address = pair.getAccountId();
        } else {
            KeyPair pair = KeyPair.fromSecretSeed(seed);
            this.address = pair.getAccountId();
        }

        this.txSecret.setText(this.seed);
        this.txAddress.setText(this.address);
    }

    private String getBalance() {
        String balances = "";
        AccountResponse account = null;
        try {
            account = this.server.accounts().account(this.address);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
        for (AccountResponse.Balance balance : account.getBalances()) {
            balances = balances + String.format("Type: %s, Code: %s, Balance: %s\n",
                    balance.getAssetType(),
                    balance.getAssetCode(),
                    balance.getBalance());
        }

        return balances;
    }

    private class GetBalanceRunner extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            return getBalance();
        }
        @Override
        protected void onPostExecute(String balances) {
            txBalance.setText(balances);
        }
    }
}
