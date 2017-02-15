package br.com.vizir.rn.paypal;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.Promise;

import com.paypal.android.sdk.payments.PayPalAuthorization;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;
import com.paypal.android.sdk.payments.PayPalFuturePaymentActivity;
import com.paypal.android.sdk.payments.PayPalProfileSharingActivity;
import com.paypal.android.sdk.payments.PayPalOAuthScopes;

import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PayPal extends ReactContextBaseJavaModule {
  private static final String ERROR_USER_CANCELLED = "USER_CANCELLED";
  private static final String ERROR_INVALID_CONFIG = "INVALID_CONFIG";

  private Callback successCallback;
  private Callback errorCallback;

  private Context activityContext;
  private Activity currentActivity;

  private static final int REQUEST_CODE_PAYMENT = 179 + 1;
  private static final int REQUEST_CODE_FUTURE_PAYMENT = 179 + 2;
  private static final int REQUEST_CODE_PROFILE_SHARING = 179 + 3;

  public PayPal(ReactApplicationContext reactContext, Context activityContext) {
    super(reactContext);
    this.activityContext = activityContext;
    this.currentActivity = (Activity)activityContext;
  }

  @Override
  public String getName() {
    return "PayPal";
  }

  @Override public Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap<>();

    constants.put("NO_NETWORK", PayPalConfiguration.ENVIRONMENT_NO_NETWORK);
    constants.put("SANDBOX", PayPalConfiguration.ENVIRONMENT_SANDBOX);
    constants.put("PRODUCTION", PayPalConfiguration.ENVIRONMENT_PRODUCTION);
    constants.put(ERROR_USER_CANCELLED, ERROR_USER_CANCELLED);
    constants.put(ERROR_INVALID_CONFIG, ERROR_INVALID_CONFIG);

    return constants;
  }

  @ReactMethod
  public void getMetadataId(
    final Callback successCallback,
    final Callback errorCallback
  ) {
    try {
      String metadataId = PayPalConfiguration.getClientMetadataId(currentActivity);
      successCallback.invoke(metadataId);
    } catch (Exception e) {
      errorCallback.invoke(e);
    }
  }

  @ReactMethod
  public void shareProfile(
    final ReadableMap payPalParameters,
    final Callback successCallback,
    final Callback errorCallback
  ) {
    this.successCallback = successCallback;
    this.errorCallback = errorCallback;

    final String environment = payPalParameters.getString("environment");
    final String clientId = payPalParameters.getString("clientId");
    final String merchantName = payPalParameters.getString("merchantName");
    final String policyUri = payPalParameters.getString("policyUri");
    final String agreementUri = payPalParameters.getString("agreementUri");

    PayPalConfiguration config = new PayPalConfiguration()
      .environment(environment)
      .clientId(clientId)
      .merchantName(merchantName)
      .merchantPrivacyPolicyUri(Uri.parse(policyUri))
      .merchantUserAgreementUri(Uri.parse(agreementUri));

    // start service
    Intent serviceIntent = new Intent(currentActivity, PayPalService.class);
    serviceIntent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
    currentActivity.startService(serviceIntent);

    // // start activity
    Intent activityIntent =
      new Intent(activityContext, PayPalProfileSharingActivity.class)
        .putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config)
        .putExtra(PayPalProfileSharingActivity.EXTRA_REQUESTED_SCOPES, getOauthScopes());

    currentActivity.startActivityForResult(activityIntent, REQUEST_CODE_PROFILE_SHARING);
  }

  @ReactMethod
  public void futurePayment(
    final ReadableMap payPalParameters,
    final Callback successCallback,
    final Callback errorCallback
  ) {
    this.successCallback = successCallback;
    this.errorCallback = errorCallback;

    final String environment = payPalParameters.getString("environment");
    final String clientId = payPalParameters.getString("clientId");
    final String merchantName = payPalParameters.getString("merchantName");
    final String policyUri = payPalParameters.getString("policyUri");
    final String agreementUri = payPalParameters.getString("agreementUri");

    PayPalConfiguration config = new PayPalConfiguration()
      .environment(environment)
      .clientId(clientId)
      .merchantName(merchantName)
      .merchantPrivacyPolicyUri(Uri.parse(policyUri))
      .merchantUserAgreementUri(Uri.parse(agreementUri));

    // start service
    Intent serviceIntent = new Intent(currentActivity, PayPalService.class);
    serviceIntent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
    currentActivity.startService(serviceIntent);

    // // start activity
    Intent activityIntent =
      new Intent(activityContext, PayPalFuturePaymentActivity.class)
        .putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);

    currentActivity.startActivityForResult(activityIntent, REQUEST_CODE_FUTURE_PAYMENT);
  }

  @ReactMethod
  public void paymentRequest(
    final ReadableMap payPalParameters,
    final Callback successCallback,
    final Callback errorCallback
  ) {
    this.successCallback = successCallback;
    this.errorCallback = errorCallback;

    final String environment = payPalParameters.getString("environment");
    final String clientId = payPalParameters.getString("clientId");
    final String price = payPalParameters.getString("price");
    final String currency = payPalParameters.getString("currency");
    final String description = payPalParameters.getString("description");

    PayPalConfiguration config =
      new PayPalConfiguration().environment(environment).clientId(clientId);

    startPayPalService(config);

    PayPalPayment thingToBuy =
      new PayPalPayment(new BigDecimal(price), currency, description,
                        PayPalPayment.PAYMENT_INTENT_SALE);

    Intent intent =
      new Intent(activityContext, PaymentActivity.class)
      .putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config)
      .putExtra(PaymentActivity.EXTRA_PAYMENT, thingToBuy);

    currentActivity.startActivityForResult(intent, REQUEST_CODE_PAYMENT);
  }

  private void startPayPalService(PayPalConfiguration config) {
    Intent intent = new Intent(currentActivity, PayPalService.class);
    intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
    currentActivity.startService(intent);
  }

  private PayPalOAuthScopes getOauthScopes() {
      /* create the set of required scopes
       * Note: see https://developer.paypal.com/docs/integration/direct/identity/attributes/ for mapping between the
       * attributes you select for this app in the PayPal developer portal and the scopes required here.
       */
      Set<String> scopes = new HashSet<String>(
              Arrays.asList(PayPalOAuthScopes.PAYPAL_SCOPE_EMAIL, PayPalOAuthScopes.PAYPAL_SCOPE_ADDRESS) );
      return new PayPalOAuthScopes(scopes);
  }

  private void handleShareProfileActivityResult(final int resultCode, final Intent data) {
    if (resultCode == Activity.RESULT_OK) {
      PayPalAuthorization auth = data
        .getParcelableExtra(PayPalProfileSharingActivity.EXTRA_RESULT_AUTHORIZATION);
      if (auth != null) {
        String authorization_code = auth.getAuthorizationCode();
        successCallback.invoke(authorization_code);
      }
    } else if (resultCode == Activity.RESULT_CANCELED) {
      errorCallback.invoke(ERROR_USER_CANCELLED);
    } else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
      errorCallback.invoke(ERROR_INVALID_CONFIG);
    }
  }

  private void handleFutureActivityResult(final int resultCode, final Intent data) {
    if (resultCode == Activity.RESULT_OK) {
      PayPalAuthorization auth = data
        .getParcelableExtra(PayPalFuturePaymentActivity.EXTRA_RESULT_AUTHORIZATION);
      if (auth != null) {
        String authorization_code = auth.getAuthorizationCode();
        successCallback.invoke(authorization_code);
      }
    } else if (resultCode == Activity.RESULT_CANCELED) {
      errorCallback.invoke(ERROR_USER_CANCELLED);
    } else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
      errorCallback.invoke(ERROR_INVALID_CONFIG);
    }
  }

  private void handlePaymentActivityResult(final int resultCode, final Intent data) {
    if (resultCode == Activity.RESULT_OK) {
      PaymentConfirmation confirm =
        data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
      if (confirm != null) {
        successCallback.invoke(
          confirm.toJSONObject().toString(),
          confirm.getPayment().toJSONObject().toString()
        );
      }
    } else if (resultCode == Activity.RESULT_CANCELED) {
      errorCallback.invoke(ERROR_USER_CANCELLED);
    } else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
      errorCallback.invoke(ERROR_INVALID_CONFIG);
    }
  }

  public void handleActivityResult(final int requestCode, final int resultCode, final Intent data) {
    if (requestCode == REQUEST_CODE_FUTURE_PAYMENT) {
      handleFutureActivityResult(resultCode, data);
    } else if (requestCode == REQUEST_CODE_PAYMENT) {
      handlePaymentActivityResult(resultCode, data);
    } else if (requestCode == REQUEST_CODE_PROFILE_SHARING) {
      handleShareProfileActivityResult(resultCode, data);
    } else {
      return;
    }

    currentActivity.stopService(new Intent(currentActivity, PayPalService.class));
  }
}
