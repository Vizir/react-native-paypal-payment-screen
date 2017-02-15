//
//  MFLReactNativePayPal.m
//  ReactPaypal
//
//  Created by Tj on 6/22/15.
//  Copyright (c) 2015 Facebook. All rights reserved.
//

#import "MFLReactNativePayPal.h"
#import "RCTBridge.h"
#import "PayPalMobile.h"


NSString * const kPayPalPaymentStatusKey              = @"status";
NSString * const kPayPalPaymentConfirmationKey        = @"confirmation";

@interface MFLReactNativePayPal () <RCTBridgeModule>

@property (copy) RCTResponseSenderBlock flowCompletedCallback;
@property(nonatomic, strong, readwrite) PayPalConfiguration *payPalConfig;

@end

@implementation MFLReactNativePayPal

RCT_EXPORT_MODULE();

RCT_EXPORT_METHOD(futurePayment:(NSString *)clientId
                  forEnv:(int)environment
                  forMerchantName:(NSString *)merchantName
                  forMerchantPolicy:(NSString *)merchantPrivacyPolicyURL
                  forMerchantAgreement:(NSString *)merchantUserAgreementURL
                  forCallback:(RCTResponseSenderBlock)flowCompletedCallback)

{
  NSString *envString = [self stringFromEnvironmentEnum:environment];

  NSLog(clientId);

  [PayPalMobile initializeWithClientIdsForEnvironments:@{envString : clientId}];
  [PayPalMobile preconnectWithEnvironment:envString];

  _payPalConfig = [[PayPalConfiguration alloc] init];

  _payPalConfig.acceptCreditCards = YES;
  _payPalConfig.merchantName = merchantName;
  _payPalConfig.merchantPrivacyPolicyURL = [NSURL URLWithString:merchantPrivacyPolicyURL];
  _payPalConfig.merchantUserAgreementURL = [NSURL URLWithString:merchantUserAgreementURL];

  self.flowCompletedCallback = flowCompletedCallback;

  PayPalFuturePaymentViewController *vc = [[PayPalFuturePaymentViewController alloc] initWithConfiguration:_payPalConfig delegate:self];

  UIViewController *visibleVC = [[[UIApplication sharedApplication] keyWindow] rootViewController];
  do {
    if ([visibleVC isKindOfClass:[UINavigationController class]]) {
      visibleVC = [(UINavigationController *)visibleVC visibleViewController];
    } else if (visibleVC.presentedViewController) {
      visibleVC = visibleVC.presentedViewController;
    }
  } while (visibleVC.presentedViewController);

  [visibleVC presentViewController:vc animated:YES completion:^{
    // self.flowCompletedCallback(@[[NSNull null]]);
  }];
}

RCT_EXPORT_METHOD(shareProfile:(NSString *)clientId
                  forEnv:(int)environment
                  forMerchantName:(NSString *)merchantName
                  forMerchantPolicy:(NSString *)merchantPrivacyPolicyURL
                  forMerchantAgreement:(NSString *)merchantUserAgreementURL
                  forCallback:(RCTResponseSenderBlock)flowCompletedCallback)

{
  NSString *envString = [self stringFromEnvironmentEnum:environment];

  [PayPalMobile initializeWithClientIdsForEnvironments:@{envString : clientId}];
  [PayPalMobile preconnectWithEnvironment:envString];

  NSLog(@"SHARE!");

  _payPalConfig = [[PayPalConfiguration alloc] init];

  _payPalConfig.merchantName = merchantName;
  _payPalConfig.merchantPrivacyPolicyURL = [NSURL URLWithString:merchantPrivacyPolicyURL];
  _payPalConfig.merchantUserAgreementURL = [NSURL URLWithString:merchantUserAgreementURL];

  self.flowCompletedCallback = flowCompletedCallback;

  NSSet *scopeValues = [NSSet setWithArray:@[kPayPalOAuth2ScopeEmail, kPayPalOAuth2ScopeFuturePayments, kPayPalOAuth2ScopePhone]];
  PayPalProfileSharingViewController *vc = [[PayPalProfileSharingViewController alloc] initWithScopeValues:scopeValues configuration:_payPalConfig delegate:self];

  UIViewController *visibleVC = [[[UIApplication sharedApplication] keyWindow] rootViewController];
  do {
    if ([visibleVC isKindOfClass:[UINavigationController class]]) {
      visibleVC = [(UINavigationController *)visibleVC visibleViewController];
    } else if (visibleVC.presentedViewController) {
      visibleVC = visibleVC.presentedViewController;
    }
  } while (visibleVC.presentedViewController);

  [visibleVC presentViewController:vc animated:YES completion:^{
    // self.flowCompletedCallback(@[[NSNull null]]);
  }];
}

- (void)payPalProfileSharingViewController:(PayPalProfileSharingViewController *)profileSharingViewController
             userDidLogInWithAuthorization:(NSDictionary *)profileSharingAuthorization {
  [profileSharingViewController.presentingViewController dismissViewControllerAnimated:YES completion:^{
    NSLog(@"SUCCESS");

    if (self.flowCompletedCallback) {
      self.flowCompletedCallback(@[profileSharingAuthorization]);
    }
  }];
}

- (void)userDidCancelPayPalProfileSharingViewController:(PayPalProfileSharingViewController *)profileSharingViewController {
  NSLog(@"PayPal Profile Sharing Authorization Canceled");
  [profileSharingViewController.presentingViewController dismissViewControllerAnimated:YES completion:^{
    self.flowCompletedCallback(@[[NSNull null]]);
  }];
}

- (void)payPalFuturePaymentViewController:(PayPalFuturePaymentViewController *)futurePaymentViewController
                 didAuthorizeFuturePayment:(NSDictionary *)futurePaymentAuthorization
{
  [futurePaymentViewController.presentingViewController dismissViewControllerAnimated:YES completion:^{
    if (self.flowCompletedCallback) {
      self.flowCompletedCallback(@[futurePaymentAuthorization]);
    }
  }];
}

- (void)payPalFuturePaymentDidCancel:(PayPalFuturePaymentViewController *)futurePaymentViewController {
  [futurePaymentViewController.presentingViewController dismissViewControllerAnimated:YES completion:^{
    self.flowCompletedCallback(@[[NSNull null]]);
  }];
}

- (NSString *)stringFromEnvironmentEnum:(PayPalEnvironment)env
{
  switch (env) {
    case kPayPalEnvironmentProduction: return PayPalEnvironmentProduction;
    case kPayPalEnvironmentSandbox: return PayPalEnvironmentSandbox;
    case kPayPalEnvironmentSandboxNoNetwork: return PayPalEnvironmentNoNetwork;
  }
}

@end