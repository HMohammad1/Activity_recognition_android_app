package com.example.helloworld;

import android.app.Application;
import android.content.Context;

import org.acra.ACRA;
import org.acra.BuildConfig;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.data.StringFormat;
import org.acra.sender.EmailIntentSender;
import org.acra.config.MailSenderConfigurationBuilder;
// Docs: https://www.acra.ch/docs/Setup
// Used for crash reporting so logs can be analysed when bugs occur
public class MyApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        final CoreConfigurationBuilder builder = new CoreConfigurationBuilder()
                .withBuildConfigClass(BuildConfig.class)
                .withReportFormat(StringFormat.JSON)
                .withPluginConfigurations(
                        new MailSenderConfigurationBuilder()
                                .withMailTo("hasan.mohammad.gm@gmail.com")
                                .withReportAsFile(true)
                                .withReportFileName("Crash.txt")
                                // Hardcoded for demonstration purposes
                                .withSubject("My App Crash Report")
                                .withBody("Here is a crash report from My App.")
                                .build()
                );
        ACRA.init(this, builder);
    }


}

