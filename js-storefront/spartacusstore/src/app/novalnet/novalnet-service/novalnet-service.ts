import { Injectable } from '@angular/core';
import { Renderer2, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { AnonymousConsentOpenDialogComponent } from '@spartacus/storefront';

@Injectable({
  providedIn: 'root'
})

export class NovalnetService {
  
  paymentTypeResponse: any;
  addressDetails : any;
  order: any;

  constructor() { 
     
  }

  setSelectedPaymentResponse(data : any){
    this.paymentTypeResponse = data;
  }

  getSelectedPaymentResponse(){
    return this.paymentTypeResponse;
  }

  setOrder(order: any) {
    this.order = order;
  }

  getOrder() {
    return this.order;
  }

  setAddresDetails(request: {}) {
    this.addressDetails = request;
  }

  getAddressDetails() {
    return this.addressDetails;
  }

  
  }