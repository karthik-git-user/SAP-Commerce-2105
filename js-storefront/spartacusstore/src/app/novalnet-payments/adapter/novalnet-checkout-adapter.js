import { HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { CARD_TYPE_NORMALIZER, PAYMENT_DETAILS_SERIALIZER, } from '@spartacus/checkout/core';
import { HttpParamsURIEncoder, PAYMENT_DETAILS_NORMALIZER, } from '@spartacus/core';
import { map, mergeMap } from 'rxjs/operators';
import * as i0 from "@angular/core";
import * as i1 from "@angular/common/http";
import * as i2 from "@spartacus/core";
export class NovalnetCheckoutAdapter {
    constructor(http, occEndpoints, converter) {
        console.log("11");
        this.http = http;
        this.occEndpoints = occEndpoints;
        this.converter = converter;
        if (typeof DOMParser !== 'undefined') {
            this.domparser = new DOMParser();
        }
    }
    getSetPaymentDetailsEndpoint(userId, cartId, paymentDetailsId) {
        console.log("20");
        return this.occEndpoints.buildUrl('setCartPaymentDetails', {
            urlParams: { userId, cartId },
            queryParams: { paymentDetailsId },
        });
    }
    getPaymentProviderSubInfoEndpoint(userId, cartId) {
        console.log("27");
        return this.occEndpoints.buildUrl('paymentProviderSubInfo', {
            urlParams: {
                userId,
                cartId,
            },
        });
    }
    getCreatePaymentDetailsEndpoint(userId, cartId) {
        console.log("36");
        return this.occEndpoints.buildUrl('createPaymentDetails', {
            urlParams: {
                userId,
                cartId,
            },
        });
    }
    getCardTypesEndpoint() {
        console.log("45");
        return this.occEndpoints.buildUrl('cardTypes');
    }
    create(userId, cartId, paymentDetails) {
        console.log("49");
        paymentDetails = this.converter.convert(paymentDetails, PAYMENT_DETAILS_SERIALIZER);
        return this.getProviderSubInfo(userId, cartId).pipe(map((data) => {
            const labelsMap = this.convertToMap(data.mappingLabels.entry);
            return {
                url: data.postUrl,
                parameters: this.getParamsForPaymentProvider(paymentDetails, data.parameters.entry, labelsMap),
                mappingLabels: labelsMap,
            };
        }), mergeMap((sub) => {
            // create a subscription directly with payment provider
            return this.createSubWithProvider(sub.url, sub.parameters).pipe(map((response) => this.extractPaymentDetailsFromHtml(response)), mergeMap((fromPaymentProvider) => {
                var _a;
                fromPaymentProvider['defaultPayment'] =
                    (_a = paymentDetails.defaultPayment) !== null && _a !== void 0 ? _a : false;
                fromPaymentProvider['savePaymentInfo'] = true;
                return this.createDetailsWithParameters(userId, cartId, fromPaymentProvider).pipe(this.converter.pipeable(PAYMENT_DETAILS_NORMALIZER));
            }));
        }));
    }
    set(userId, cartId, paymentDetailsId) {
        console.log("70");
        return this.http.put(this.getSetPaymentDetailsEndpoint(userId, cartId, paymentDetailsId), {});
    }
    loadCardTypes() {
        console.log("74");
        return this.http.get(this.getCardTypesEndpoint()).pipe(map((cardTypeList) => { var _a; return (_a = cardTypeList.cardTypes) !== null && _a !== void 0 ? _a : []; }), this.converter.pipeableMany(CARD_TYPE_NORMALIZER));
    }
    getProviderSubInfo(userId, cartId) {
        console.log("78");
        return this.http.get(this.getPaymentProviderSubInfoEndpoint(userId, cartId));
    }
    createSubWithProvider(postUrl, parameters) {
        console.log("82");
        const headers = new HttpHeaders({
            'Content-Type': 'application/x-www-form-urlencoded',
            Accept: 'text/html',
        });
        let httpParams = new HttpParams({ encoder: new HttpParamsURIEncoder() });
        Object.keys(parameters).forEach((key) => {
            httpParams = httpParams.append(key, parameters[key]);
        });
        return this.http.post(postUrl, httpParams, {
            headers,
            responseType: 'text',
        });
    }
    createDetailsWithParameters(userId, cartId, parameters) {
        console.log("97");
        let httpParams = new HttpParams({ encoder: new HttpParamsURIEncoder() });
        Object.keys(parameters).forEach((key) => {
            httpParams = httpParams.append(key, parameters[key]);
        });
        const headers = new HttpHeaders({
            'Content-Type': 'application/x-www-form-urlencoded',
        });
        return this.http.post(this.getCreatePaymentDetailsEndpoint(userId, cartId), httpParams, { headers });
    }
    getParamsForPaymentProvider(paymentDetails, parameters, mappingLabels) {
        console.log("108");
        var _a, _b, _c, _d, _e, _f, _g, _h, _j, _k;
        const params = this.convertToMap(parameters);
        params[mappingLabels['hybris_account_holder_name']] =
            paymentDetails.accountHolderName;
        params[mappingLabels['hybris_card_type']] = (_a = paymentDetails.cardType) === null || _a === void 0 ? void 0 : _a.code;
        params[mappingLabels['hybris_card_number']] = paymentDetails.cardNumber;
        if (mappingLabels['hybris_combined_expiry_date'] === 'true') {
            params[mappingLabels['hybris_card_expiry_date']] =
                paymentDetails.expiryMonth +
                    mappingLabels['hybris_separator_expiry_date'] +
                    paymentDetails.expiryYear;
        }
        else {
            params[mappingLabels['hybris_card_expiration_month']] =
                paymentDetails.expiryMonth;
            params[mappingLabels['hybris_card_expiration_year']] =
                paymentDetails.expiryYear;
        }
        params[mappingLabels['hybris_card_cvn']] = paymentDetails.cvn;
        // billing address
        params[mappingLabels['hybris_billTo_country']] =
            (_c = (_b = paymentDetails.billingAddress) === null || _b === void 0 ? void 0 : _b.country) === null || _c === void 0 ? void 0 : _c.isocode;
        params[mappingLabels['hybris_billTo_firstname']] =
            (_d = paymentDetails.billingAddress) === null || _d === void 0 ? void 0 : _d.firstName;
        params[mappingLabels['hybris_billTo_lastname']] =
            (_e = paymentDetails.billingAddress) === null || _e === void 0 ? void 0 : _e.lastName;
        params[mappingLabels['hybris_billTo_street1']] =
            ((_f = paymentDetails.billingAddress) === null || _f === void 0 ? void 0 : _f.line1) +
                ' ' +
                ((_g = paymentDetails.billingAddress) === null || _g === void 0 ? void 0 : _g.line2);
        params[mappingLabels['hybris_billTo_city']] =
            (_h = paymentDetails.billingAddress) === null || _h === void 0 ? void 0 : _h.town;
        if ((_j = paymentDetails.billingAddress) === null || _j === void 0 ? void 0 : _j.region) {
            params[mappingLabels['hybris_billTo_region']] =
                paymentDetails.billingAddress.region.isocodeShort;
        }
        else {
            params[mappingLabels['hybris_billTo_region']] = '';
        }
        params[mappingLabels['hybris_billTo_postalcode']] =
            (_k = paymentDetails.billingAddress) === null || _k === void 0 ? void 0 : _k.postalCode;
        return params;
    }
    extractPaymentDetailsFromHtml(html) {
        console.log("153");
        const domdoc = this.domparser.parseFromString(html, 'text/xml');
        const responseForm = domdoc.getElementsByTagName('form')[0];
        const inputs = responseForm.getElementsByTagName('input');
        const values = {};
        for (let i = 0; inputs[i]; i++) {
            const input = inputs[i];
            const name = input.getAttribute('name');
            const value = input.getAttribute('value');
            if (name && name !== '{}' && value && value !== '') {
                values[name] = value;
            }
        }
        return values;
    }
    convertToMap(paramList) {
        console.log("169");
        return paramList.reduce(function (result, item) {
            const key = item.key;
            result[key] = item.value;
            return result;
        }, {});
    }
}