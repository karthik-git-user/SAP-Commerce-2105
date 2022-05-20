import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PaymentDetailsFormComponent } from './payment-details-form/payment-details-form.component';
import { CardType, CmsConfig, ConfigModule, PaymentDetails } from '@spartacus/core';
import { CheckoutPaymentAdapter } from '@spartacus/checkout/core';
import { Observable } from 'rxjs';

@NgModule({
  declarations: [
    PaymentDetailsFormComponent
  ],
  imports: [
    CommonModule
  ]
})
export class NovalnetPaymentsModule implements CheckoutPaymentAdapter  {
  create(userId: string, cartId: string, paymentDetails: PaymentDetails): Observable<PaymentDetails> {
	  console.log("test18+++++++++++");    
    throw new Error('Method not implemented.');
  }
  set(userId: string, cartId: string, paymentDetailsId: string): Observable<any> {
	  console.log("test22+++++++++++");
    throw new Error('Method not implemented.');
  }
  loadCardTypes(): Observable<CardType[]> {
	  console.log("test26+++++++++++");
    throw new Error('Method not implemented.');
  }
}
