'use strict';
var {NativeModules, Platform} = require('react-native')
var {PayPal, MFLReactNativePayPal} = NativeModules;

var constants;

if (Platform.OS === 'android') {
  constants = {};
  var constantNames = Object.keys(PayPal).filter(p => p == p.toUpperCase());
  constantNames.forEach(c => constants[c] = PayPal[c]);
} else {
  constants = {
    SANDBOX: 0,
    PRODUCTION: 1,
    NO_NETWORK: 2,

    USER_CANCELLED: 'USER_CANCELLED',
    INVALID_CONFIG: 'INVALID_CONFIG'
  }
}

var functions = {
  futurePayment(payPalParameters) {
    if (Platform.OS === 'android') {
      return new Promise(function(resolve, reject) {
        let successCallback = (result) => {
          resolve(result);
        };
        PayPal.futurePayment(payPalParameters, successCallback, reject);
      });
    } else {
      return new Promise(function(resolve, reject) {
        let callback = (result) => {
          result ? resolve(result) : reject(result);
        };
        MFLReactNativePayPal.futurePayment(
          payPalParameters.clientId,
          payPalParameters.environment,
          payPalParameters.merchantName,
          payPalParameters.policyUri,
          payPalParameters.agreementUri,
          callback
        );
      });
    }
  },

  shareProfile(payPalParameters) {
    if (Platform.OS === 'android') {
      return new Promise(function(resolve, reject) {
        let successCallback = (code) => {
          resolve({response: {code}});
        };
        PayPal.shareProfile(payPalParameters, successCallback, reject);
      });
    } else {
      return new Promise(function(resolve, reject) {
        let callback = (result) => {
          result ? resolve(result) : reject(result);
        };
        MFLReactNativePayPal.shareProfile(
          payPalParameters.clientId,
          payPalParameters.environment,
          payPalParameters.merchantName,
          payPalParameters.policyUri,
          payPalParameters.agreementUri,
          callback
        );
      });
    }
  },

  getMetadataId() {
    return new Promise(function(resolve, reject) {
      PayPal.getMetadataId(resolve, reject);
    });
  },
};

var exported = {};
Object.assign(exported, constants, functions);

module.exports = exported;
