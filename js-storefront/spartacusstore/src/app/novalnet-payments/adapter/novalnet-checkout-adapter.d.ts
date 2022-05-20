import {CardType, ConverterService, OccEndpointsService, PaymentDetails,} from "@spartacus/core";
import { Injectable } from "@angular/core";
import { Observable, of } from "rxjs";
import { CheckoutPaymentAdapter } from "@spartacus/checkout/core";
import { HttpClient } from "@angular/common/http";
  
  @Injectable()
  export class NovalnetCheckoutAdapter implements CheckoutPaymentAdapter {
    protected http: HttpClient;
    protected occEndpoints: OccEndpointsService;
    protected converter: ConverterService;
    constructor(http: HttpClient, occEndpoints: OccEndpointsService, converter: ConverterService);
    private domparser;
    protected getSetPaymentDetailsEndpoint(userId: string, cartId: string, paymentDetailsId: string): string;
    protected getPaymentProviderSubInfoEndpoint(userId: string, cartId: string): string;
    protected getCreatePaymentDetailsEndpoint(userId: string, cartId: string): string;
    protected getCardTypesEndpoint(): string;
    create(userId: string, cartId: string, paymentDetails: PaymentDetails): Observable<PaymentDetails>;
    set(userId: string, cartId: string, paymentDetailsId: string): Observable<any>;
    loadCardTypes(): Observable<CardType[]>;
    protected getProviderSubInfo(userId: string, cartId: string): Observable<any>;
    protected createSubWithProvider(postUrl: string, parameters: any): Observable<any>;
    protected createDetailsWithParameters(userId: string, cartId: string, parameters: any): Observable<PaymentDetails>;
    private getParamsForPaymentProvider;
    private extractPaymentDetailsFromHtml;
    private convertToMap;
  }
  