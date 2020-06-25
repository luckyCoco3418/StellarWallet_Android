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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.stellar.sdk.AccountRequiresMemoException;
import org.stellar.sdk.AssetTypeCreditAlphaNum12;
import org.stellar.sdk.AssetTypeCreditAlphaNum4;
import org.stellar.sdk.AssetTypeNative;
import org.stellar.sdk.Asset;
import org.stellar.sdk.ChangeTrustOperation;
import org.stellar.sdk.PaymentOperation;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Server;
import org.stellar.sdk.Account;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.Network;
import org.stellar.sdk.Memo;
import org.stellar.sdk.responses.SubmitTransactionResponse;
import org.stellar.sdk.requests.ErrorResponse;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    TextView txSecret;
    TextView txAddress;
    TextView txBalance;
    EditText ebToAddress;
    EditText ebSymbol;
    EditText ebAmount;

    String seed, address;

    Server server;
    KeyPair keyPair;

    String TOKEN_CODE = "BTC"; //"RHT";
    String TOKEN_ISSUER = "GCNSGHUCG5VMGLT5RIYYZSO7VQULQKAJ62QA33DBC5PPBSO57LFWVV6P"; //"GCLNYYCC226567NWO7RYVB3DKJ5E7QEBY7R5RC3EYXWQBIRWM7ISWF24";
    String TOKEN_LIMIT = "100"; //"45000000";

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

        txSecret = (TextView) findViewById(R.id.secret);
        txAddress = (TextView) findViewById(R.id.address);
        txBalance = (TextView) findViewById(R.id.balance);
        ebToAddress = (EditText)findViewById(R.id.to_address);
        ebSymbol = (EditText)findViewById(R.id.symbol);
        ebAmount = (EditText)findViewById(R.id.send_amount);

        Button button = (Button)findViewById(R.id.send);
        button.setOnClickListener(this);

        initWallet();
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
            this.keyPair = KeyPair.random();
            this.seed = new String(this.keyPair.getSecretSeed());
            this.address = this.keyPair.getAccountId();
        } else {
            this.keyPair = KeyPair.fromSecretSeed(seed);
            this.address = this.keyPair.getAccountId();
        }

        this.txSecret.setText(this.seed);
        this.txAddress.setText(this.address);

        new AddTrustlineRunner().execute();
    }

    private AccountResponse GetAccountResponse() {
        try {
            return this.server.accounts().account(this.address);
        } catch (ErrorResponse e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private class AddTrustlineRunner extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            AccountResponse accountResponse = GetAccountResponse();
            if (accountResponse == null) {
                return false;
            }

            for (AccountResponse.Balance balance : accountResponse.getBalances()) {
                if (TOKEN_CODE.equals(balance.getAssetCode()) && TOKEN_ISSUER.equals(balance.getAssetIssuer())) {
                    return true;
                }
            }
//            return addTrustline();
            return true;
        }
        @Override
        protected void onPostExecute(Boolean trusted) {
        }
    }

    private boolean addTrustline() {
        Asset asset = new AssetTypeCreditAlphaNum4(TOKEN_CODE, TOKEN_ISSUER);

        ChangeTrustOperation operation = new ChangeTrustOperation.Builder(asset, TOKEN_LIMIT)
                .setSourceAccount(address)
                .build();

        AccountResponse accountResponse = GetAccountResponse();
        if (accountResponse == null) {
            return false;
        }
        Account account = new Account(address, accountResponse.getSequenceNumber());
        Transaction.Builder builder = new Transaction.Builder(account, Network.PUBLIC)
                .addOperation(operation)
//                .addMemo(Memo.text("Add trustline"))
                .setBaseFee(Transaction.MIN_BASE_FEE)
                .setTimeout(Transaction.Builder.TIMEOUT_INFINITE);
        Transaction transaction = builder.build();
        transaction.sign(keyPair);

        try {
            SubmitTransactionResponse response = server.submitTransaction(transaction);
            boolean success = response.isSuccess();
            return success;
        } catch (AccountRequiresMemoException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            // expect exception
            e.printStackTrace();
        }
        return false;
    }

    private String getBalance() {
        AccountResponse accountResponse = GetAccountResponse();
        if (accountResponse == null) {
            return "";
        }

        String balances = "";
        for (AccountResponse.Balance balance : accountResponse.getBalances()) {
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

    @Override
    public void onClick(View v) {
        new TransferRunner().execute();
    }

    private class TransferRunner extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            return transfer();
        }
        @Override
        protected void onPostExecute(Boolean success) {}
    }

    private boolean transfer() {
        String toAddress = ebToAddress.getText().toString();
        String amount = ebAmount.getText().toString();
        String symbol = ebSymbol.getText().toString();
        Asset asset;
        if (symbol.equals(TOKEN_CODE)) {
            asset = new AssetTypeCreditAlphaNum4(TOKEN_CODE, TOKEN_ISSUER);
        } else {
            asset = new AssetTypeNative();
        }

        PaymentOperation operation = new PaymentOperation.Builder(toAddress, asset, amount)
                .setSourceAccount(address)
                .build();

        AccountResponse accountResponse = GetAccountResponse();
        if (accountResponse == null) {
            return false;
        }
        Account account = new Account(address, accountResponse.getSequenceNumber());
        Transaction.Builder builder = new Transaction.Builder(account, Network.PUBLIC)
                .addOperation(operation)
                .addMemo(Memo.text("Test transfer"))
                .setBaseFee(Transaction.MIN_BASE_FEE)
                .setTimeout(Transaction.Builder.TIMEOUT_INFINITE);
        Transaction transaction = builder.build();
        transaction.sign(keyPair);

        try {
            SubmitTransactionResponse response = server.submitTransaction(transaction);
            boolean success = response.isSuccess();
            return success;
        } catch (AccountRequiresMemoException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            // expect exception
            e.printStackTrace();
        }
        return false;
    }

}
