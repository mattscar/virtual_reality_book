package com.dreambookvr.ch05_asynctask;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;

public class AsyncTaskActivity extends Activity {
  private TextView tv;

  private class PrimeFinder extends AsyncTask<Float, String, Integer> {
    StringBuilder sb;

    @Override
    protected void onPreExecute() {
      sb = new StringBuilder();
      tv.append("Starting the task.\n");
    }

    @Override
    protected Integer doInBackground(Float... nums) {
      int N = Math.round(nums[0]);
      int knownPrime, count = 0;
      boolean almostDone = false;

      // Initial label
      sb.append("Primes less than ").append(N).append(": ");

      // Default value is false - assume every number is prime
      boolean[] isComposite = new boolean[N + 1];
      isComposite[1] = true;

      // Find composite numbers
      for(int i=2; i<=N; i++) {

        // Check whether the value has been marked
        if(!isComposite[i]) {
          count++;
          sb.append(i).append(", ");
          knownPrime = 2;
          while(i * knownPrime <= N) {
            isComposite[i * knownPrime] = true;
            knownPrime++;
          }
        }

        // Provide a progress message
        if((i > N/2) && !almostDone) {
          publishProgress("Almost done!\n");
          almostDone = true;
        }
      }

      // Remove last two characters
      int len = sb.length();
      sb.delete(len-2, len-1);
      return count;
    }

    @Override
    protected void onProgressUpdate(String... progress) {
      tv.append(progress[0]);
    }

    @Override
    protected void onPostExecute(Integer result) {
      sb.append("\nThere are ").append(result).append(" primes in total.");
      tv.append(sb.toString());
    }
  }

  public void onCreate(Bundle b) {
    super.onCreate(b);
    setContentView(R.layout.main_layout);
    tv = (TextView)findViewById(R.id.tv);

    // Create and launch the thread
    PrimeFinder primeFinder = new PrimeFinder();
    primeFinder.execute(500.0f);
  }
}